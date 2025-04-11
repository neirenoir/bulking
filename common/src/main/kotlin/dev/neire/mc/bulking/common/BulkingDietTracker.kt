package dev.neire.mc.bulking.common

import com.illusivesoulworks.diet.api.DietApi
import com.illusivesoulworks.diet.api.type.IDietResult
import com.illusivesoulworks.diet.api.type.IDietSuite
import com.illusivesoulworks.diet.api.type.IDietTracker
import com.illusivesoulworks.diet.common.config.DietConfig
import com.illusivesoulworks.diet.common.data.effect.DietEffect.MatchMethod
import com.illusivesoulworks.diet.common.data.suite.DietSuites
import com.illusivesoulworks.diet.common.util.DietResult
import com.illusivesoulworks.diet.platform.Services
import dev.neire.mc.bulking.common.Snacks.isSnack
import dev.neire.mc.bulking.common.effects.SumptuousFeastEffect
import dev.neire.mc.bulking.common.registries.BulkingAttributes
import dev.neire.mc.bulking.config.BulkingConfig
import dev.neire.mc.bulking.networking.BulkingMessages
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

typealias NutritionData = MutableMap<String, Float>
typealias ComputedEffects =
    Pair<Map<Attribute, MutableSet<AttributeModifier>>, Set<MobEffectInstance>>

class BulkingDietTracker(
    private val player: Player,
) : IDietTracker {
    val stomach: MutableList<Pair<ItemStack, NutritionData>> = mutableListOf()
    val nutrition: MutableList<NutritionData> = mutableListOf()
    val eatenFood: MutableSet<Item> = mutableSetOf()
    private var suite = BulkingConfig.BulkingCommonConfig.USE_SUITE.get()
    private var active = true
    private val activeModifiers: MutableMap<Attribute, MutableSet<UUID>> = mutableMapOf()

    companion object {
        val EFFECT_DURATION: MutableMap<MobEffect, Int> = mutableMapOf()
        const val DEFAULT_SUITE = "bulking_builtin"
        const val DEFAULT_STOMACH_SIZE = 3
        const val BALANCE_AROUND = 10

        const val STOMACH_TAG = "stomach"
        const val NUTRITION_TAG = "nutrition"
        const val EATEN_FOOD_TAG = "eaten_food"
        const val ACTIVE_TAG = "active"
        const val MODIFIERS_TAG = "modifiers"
        const val ATTRIBUTE_NAME_TAG = "attribute_name"
        const val ATTRIBUTE_UUID_TAG = "uuid"

        const val STOMACH_ITEM_TAG = "item"
        const val STOMACH_DIET_RESULT_TAG = "result"

        init {
            EFFECT_DURATION[MobEffects.CONFUSION] = 300
        }

        fun <K> applyStomachFormula(
            groups: Map<K, Float>,
            stomachWeight: Float,
        ): Map<K, Float> =
            groups
                .mapValues { (_, value) ->
                    val modifiedValue = value * BALANCE_AROUND / DEFAULT_STOMACH_SIZE
                    min(modifiedValue, 1f) * stomachWeight
                }.toMutableMap()
    }

    override fun consume(stack: ItemStack?) {
        if (stack == null || stack.isEmpty || !active) {
            return
        }

        if (Services.EVENT.fireConsumeStackEvent(stack, this.player)) {
            return
        }

        val dietResult = DietApi.getInstance().get(this.player, stack)
        if (dietResult != DietResult.EMPTY) {
            this.swallow(stack, dietResult)
        }
    }

    override fun consume(
        stack: ItemStack?,
        hunger: Int,
        saturationModifier: Float,
    ) {
        if (stack == null || stack.isEmpty || !active) {
            return
        }

        if (Services.EVENT.fireConsumeStackEvent(stack, this.player)) {
            return
        }

        val dietResult =
            DietApi.getInstance().get(
                this.player,
                stack,
                hunger,
                saturationModifier,
            )

        if (dietResult != DietResult.EMPTY) {
            this.swallow(stack, dietResult)
        }
    }

    override fun consume(
        stacks: MutableList<ItemStack>?,
        hunger: Int,
        saturationModifier: Float,
    ) {
        if (!active) {
            return
        }

        val dietResult =
            DietApi.getInstance().get(
                this.player,
                stacks,
                hunger,
                saturationModifier,
            )

        if (dietResult != DietResult.EMPTY) {
            this.swallow(ItemStack(Items.SUSPICIOUS_STEW), dietResult)
        }
    }

    fun swallow(
        stack: ItemStack,
        dietResult: IDietResult,
    ) {
        val stomachSizeAttr =
            player.getAttribute(BulkingAttributes.STOMACH_SIZE_ATTRIBUTE)
                ?: return

        val stomachSize = stomachSizeAttr.value.toInt()
        val foodProperties = stack.item.foodProperties
        val foodNutritionalData = dietResult.get()

        // If it's rotten flesh, empty the contents of the stomach
        if (stack.item == Items.ROTTEN_FLESH) {
            val vomitDamage = player.damageSources().starve()
            val vomitNausea =
                MobEffectInstance(
                    MobEffects.CONFUSION,
                    25 * 20,
                    2,
                    false,
                    true,
                    true,
                )

            this.stomach.clear()
            player.removeAllEffects()
            applyEffects()
            this.player.hurt(vomitDamage, 2f)
            this.player.addEffect(vomitNausea)
            this.addEaten(stack.item)

            return
        }

        if (foodNutritionalData.isEmpty()) {
            // This item has no nutritional value
            return
        }

        // If the stomach is full and this is not a snack, that means we
        // have eaten a canAlwaysEat() food. These food items should override
        // old stomach contents
        if (stomach.size >= stomachSize && !stack.isSnack()) {
            val overeaten = (stomach.size + 1) - stomachSize
            val newStomach =
                stomach.slice(
                    overeaten until stomach.size,
                )
            stomach.retainAll(newStomach)
        }

        if (foodProperties == null || !stack.isSnack()) {
            // We will make a concession to make all non-food ItemStacks
            // full-course meals
            val result = foodNutritionalData.mapKeys { (k, _) -> k.name }
            stomach.add(Pair(stack, result.toMutableMap()))
        }

        this.addEaten(stack.item)
        if (foodProperties == null) {
            // Player just ate a "non-edible" item. Syncing is necessary,
            // as it likely came from the capturedBlock serverside-only path
            this.syncStomachNutrition()
        }
        applyEffects()
    }

    fun digest() {
        val nutritionDays =
            BulkingConfig.BulkingCommonConfig.NUTRITION_DAYS.get()
        if (this.nutrition.size >= nutritionDays) {
            val overfedBy = (this.nutrition.size + 1) - nutritionDays
            val newNutrition =
                nutrition.slice(overfedBy until this.nutrition.size)
            this.nutrition.retainAll(newNutrition)
        }

        val stomachSizeAttr =
            player.getAttribute(BulkingAttributes.STOMACH_SIZE_ATTRIBUTE)
                ?: return
        val stomachValues = getStomachValues().toMutableMap()
        this.nutrition.add(stomachValues)
        val stomachContentsOnSleep = this.stomach.size
        this.stomach.clear()

        player.removeAllEffects()
        applyEffects()

        if (stomachContentsOnSleep >= stomachSizeAttr.value) {
            val sumptuousFeastEffect =
                MobEffectInstance(
                    SumptuousFeastEffect,
                    24000, // duration
                    0, // amplifier
                    false, // is ambient
                    true, // show in inventory
                    false, // show in corner
                )
            player.addEffect(sumptuousFeastEffect)
        }

        // Heal player
        if (player.health < player.maxHealth) {
            player.health = player.maxHealth
        }
    }

    fun computeEffects(): ComputedEffects {
        val attributeModifiers: MutableMap<Attribute, MutableSet<AttributeModifier>> =
            mutableMapOf()
        val mobEffects: MutableSet<MobEffectInstance> = mutableSetOf()

        DietSuites
            .getSuite(player.level(), this.suite)
            .ifPresent { currentSuite: IDietSuite ->
                for (effect in currentSuite.effects) {
                    var match = true
                    var multiplier = 0

                    for (condition in effect.conditions) {
                        val matches =
                            condition.getMatches(
                                this.player,
                                this.values,
                            )
                        if (matches == 0) {
                            match = false
                            break
                        }

                        if (condition.matchMethod === MatchMethod.EVERY) {
                            multiplier += matches
                        }
                    }

                    if (!match) {
                        continue
                    }

                    multiplier = max(1.0, multiplier.toDouble()).toInt()

                    for (attribute in effect.attributes) {
                        val att =
                            player.getAttribute(attribute.attribute)
                        val mod =
                            AttributeModifier(
                                effect.uuid,
                                "Diet group effect",
                                attribute.baseAmount + (multiplier - 1).toDouble() * attribute.increment,
                                attribute.operation,
                            )
                        if (att != null) {
                            attributeModifiers
                                .computeIfAbsent(
                                    attribute.attribute,
                                ) { mutableSetOf() }
                                .add(mod)

                            if (!att.hasModifier(mod)) {
                                activeModifiers
                                    .computeIfAbsent(
                                        attribute.attribute,
                                    ) { mutableSetOf() }
                                    .add(effect.uuid)
                            }
                        }
                    }

                    for (statusEffect in effect.statusEffects) {
                        val duration =
                            EFFECT_DURATION.getOrDefault(
                                statusEffect.effect,
                                24000,
                            )
                        val instance =
                            MobEffectInstance(
                                statusEffect.effect,
                                duration,
                                statusEffect.basePower + (multiplier - 1) * statusEffect.increment,
                                true,
                                false,
                            )
                        mobEffects.add(instance)
                    }
                }
            }
        return Pair(attributeModifiers.toMap(), mobEffects.toSet())
    }

    private fun applyEffects() {
        if (Services.EVENT.fireApplyEffectEvent(this.player)) {
            return
        }

        // Clear all modifiers
        activeModifiers.forEach { (attr, uuidCollection) ->
            val attribute = player.getAttribute(attr)
            attribute?.apply {
                uuidCollection.forEach { uuid ->
                    this.removeModifier(uuid)
                }
            }
        }

        val computed = computeEffects()

        computed.first.forEach { (att, mod) ->
            val attInstance = player.getAttribute(att) ?: return@forEach
            mod.forEach {
                attInstance.addPermanentModifier(it)
            }
        }
        computed.second.forEach {
            player.addEffect(it)
        }

        syncEffects(computed)

        // Clamp player health
        if (player.health > player.maxHealth) {
            player.health = player.maxHealth
        }
    }

    fun hasRoomForDessert(stack: ItemStack): Boolean {
        val stomachSizeAttr =
            player.getAttribute(BulkingAttributes.STOMACH_SIZE_ATTRIBUTE)
                ?: return true

        val stomachSize = stomachSizeAttr.value.toInt()

        if (stomach.size < stomachSize) {
            return true
        }

        val foodProperties = stack.item.foodProperties
        if (foodProperties?.canAlwaysEat() == true) {
            return true
        }

        val isFullHealth = this.player.health >= this.player.maxHealth
        val isGodMode = this.player.abilities.invulnerable
        val isSnack = stack.isSnack()
        val isVomitInducing = stack.item == Items.ROTTEN_FLESH

        return isGodMode || (isSnack && !isFullHealth) || isVomitInducing
    }

    private fun getStomachValues(): NutritionData {
        val nutritionWeight =
            BulkingConfig.BulkingCommonConfig.NUTRITION_WEIGHT
                .get()
                .toFloat()
        val stomachWeight = 1f - nutritionWeight

        // Aggregate values from stomach contents
        val aggregatedValues =
            stomach
                .flatMap { it.second.entries }
                .groupingBy { it.key }
                .fold(0f) { acc, entry -> acc + entry.value }
                .toMutableMap()

        // Apply formula to get weighted values
        return applyStomachFormula(
            aggregatedValues,
            stomachWeight,
        ).toMutableMap()
    }

    override fun getValues(): NutritionData {
        // The highest value modifier in the vanilla lists is 12% for
        // golden carrots. We're gonna assume 10% is a good number to
        // balance the mod around (and we can configure it later if
        // needed)
        val nutritionWeight =
            BulkingConfig.BulkingCommonConfig.NUTRITION_WEIGHT
                .get()
                .toFloat()
        val historyContents =
            nutrition
                .flatMap { contents -> contents.entries }
                .groupingBy { it.key }
                .fold(0f) { acc, element -> acc + element.value }
                .mapValues { (_, value) ->
                    value.coerceAtMost(1f) * nutritionWeight
                }

        val stomachContents = getStomachValues()
        return (stomachContents.asSequence() + historyContents.asSequence())
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> values.sum().coerceAtMost(1f) }
            .toMutableMap()
    }

    override fun getValue(group: String?): Float = values.getOrDefault(group, 0f)

    override fun getSuite(): String = this.suite

    override fun setSuite(suite: String?) {
        // FIXME: unfortunately, this method is only called by a "phantom copy"
        //        of the original Cardinal Components capability.
        //        This causes the phantom copy's default "builtin" suite to
        //        override our suite.
        //        I think it should be fixed upstream by relying on Services
        //        provider instead of accessing the DIET_TRACKER capability

        /*
        if (suite == null) {
            throw InvalidParameterException("Can not set suite to null")
        }
        DietSuites
            .getSuite(player.level(), suite)
            .ifPresent { _ ->
                this.suite = suite
            }
         */
    }

    override fun getModifiers(): MutableMap<Attribute, MutableSet<UUID>> = this.activeModifiers

    override fun setModifiers(modifiers: MutableMap<Attribute, MutableSet<UUID>>?) {
        if (modifiers == null) {
            return
        }

        this.activeModifiers.clear()
        this.activeModifiers.putAll(modifiers)
    }

    override fun isActive(): Boolean = this.active

    override fun setActive(active: Boolean) {
        this.active = active
    }

    override fun getPlayer(): Player = this.player

    fun syncStomachNutrition() {
        val p = this.player
        if (p !is ServerPlayer) {
            return
        }

        BulkingMessages.CHANNEL.sendToPlayer(
            p,
            BulkingMessages.SyncPacket(
                this.stomach,
                this.nutrition,
            ),
        )
    }

    fun syncEffects(computed: ComputedEffects) {
        val p = this.player
        if (p !is ServerPlayer) {
            return
        }

        BulkingMessages.CHANNEL.sendToPlayer(
            p,
            BulkingMessages.SyncBulkingEffects(computed),
        )
    }

    fun syncEaten() {
        val player = this.player
        if (player is ServerPlayer) {
            Services.NETWORK.sendEatenS2C(player, eatenFood)
        }
    }

    override fun sync() {
        syncStomachNutrition()
        syncEffects(computeEffects())
        syncEaten()
    }

    override fun getCapturedStack(): ItemStack = ItemStack.EMPTY

    override fun addEaten(item: Item?) {
        // This field seems to be for "unlocked" foods in case
        // the "hideTooltipsUntilEaten" config option is enabled
        if (item == null) {
            return
        }

        eatenFood.add(item)
    }

    override fun getEaten(): MutableSet<Item> = eatenFood

    override fun setEaten(newFoodEaten: MutableSet<Item>?) {
        if (newFoodEaten == null) {
            return
        }

        this.eatenFood.clear()
        this.eatenFood.addAll(newFoodEaten)
    }

    override fun save(tag: CompoundTag?) {
        if (tag == null) {
            return
        }

        val stomachTag = ListTag()
        this.stomach.forEach {
            val stomachPairTag = CompoundTag()
            val stomachResultTag = CompoundTag()
            val itemTag = CompoundTag()
            it.first.save(itemTag)
            stomachPairTag.put(STOMACH_ITEM_TAG, itemTag as Tag)
            it.second.forEach { kv ->
                stomachResultTag.putFloat(kv.key, kv.value)
            }
            stomachPairTag.put(STOMACH_DIET_RESULT_TAG, stomachResultTag)
            stomachTag.add(stomachPairTag)
        }

        val nutritionTag = ListTag()
        this.nutrition.forEach {
            val nutritionEntry = CompoundTag()
            it.forEach { kv ->
                nutritionEntry.putFloat(kv.key, kv.value)
            }
            nutritionTag.add(nutritionEntry)
        }

        val eatenFoodTag = ListTag()
        this.eatenFood.forEach {
            eatenFoodTag.add(
                StringTag.valueOf(Services.REGISTRY.getItemKey(it).toString()),
            )
        }

        val modifiersTag = ListTag()
        this.activeModifiers.forEach {
            val attributeTag = CompoundTag()
            attributeTag.put(
                ATTRIBUTE_NAME_TAG,
                StringTag.valueOf(
                    (
                        Services.REGISTRY.getAttributeKey(
                            it.key,
                        ) as ResourceLocation
                    ).toString(),
                ),
            )

            val uuids = ListTag()

            for (uuid in it.value) {
                uuids.add(StringTag.valueOf(uuid.toString()))
            }

            attributeTag.put(ATTRIBUTE_UUID_TAG, uuids)
            modifiersTag.add(attributeTag)
        }

        tag.put(STOMACH_TAG, stomachTag)
        tag.put(NUTRITION_TAG, nutritionTag)
        tag.put(EATEN_FOOD_TAG, eatenFoodTag)
        tag.putBoolean(ACTIVE_TAG, this.active)
        tag.put(MODIFIERS_TAG, modifiersTag)
    }

    override fun load(tag: CompoundTag?) {
        if (tag == null) {
            return
        }

        if (tag.contains(STOMACH_TAG)) {
            val newStomach = tag.getList(STOMACH_TAG, Tag.TAG_COMPOUND.toInt())
            this.stomach.clear()
            newStomach.forEach {
                it as CompoundTag

                if (it.contains(STOMACH_ITEM_TAG) &&
                    it.contains(STOMACH_DIET_RESULT_TAG)
                ) {
                    val newStomachItem =
                        ItemStack.of(it.getCompound(STOMACH_ITEM_TAG))

                    val newStomachNutritionData =
                        it.getCompound(STOMACH_DIET_RESULT_TAG)
                    val map: NutritionData = mutableMapOf()
                    newStomachNutritionData.allKeys.forEach { n ->
                        map[n] = newStomachNutritionData.getFloat(n)
                    }

                    stomach.add(Pair(newStomachItem, map))
                }
            }
        }

        if (tag.contains(NUTRITION_TAG)) {
            val newNutrition =
                tag.getList(NUTRITION_TAG, Tag.TAG_COMPOUND.toInt())
            this.nutrition.clear()
            newNutrition.forEach {
                it as CompoundTag

                val newNutritionData: NutritionData = mutableMapOf()
                it.allKeys.forEach { n -> newNutritionData[n] = it.getFloat(n) }
                this.nutrition.add(newNutritionData)
            }
        }

        if (tag.contains(EATEN_FOOD_TAG)) {
            val newEatenFood =
                tag.getList(EATEN_FOOD_TAG, Tag.TAG_STRING.toInt())
            this.eatenFood.clear()
            this.eatenFood.addAll(
                newEatenFood
                    .map {
                        ResourceLocation(it.asString)
                    }.filter {
                        Services.REGISTRY.getItem(it).isPresent
                    }.map {
                        Services.REGISTRY.getItem(it).get()
                    },
            )
        }

        if (tag.contains(ACTIVE_TAG)) {
            this.active = tag.getBoolean(ACTIVE_TAG)
        }

        if (tag.contains(MODIFIERS_TAG)) {
            val newModifiers =
                tag.getList(MODIFIERS_TAG, Tag.TAG_COMPOUND.toInt())
            this.activeModifiers.clear()
            newModifiers.forEach {
                it as CompoundTag
                val attrResLoc =
                    ResourceLocation(it.getString(ATTRIBUTE_NAME_TAG))
                val attribute =
                    Services.REGISTRY
                        .getAttribute(
                            attrResLoc,
                        ).orElse(
                            null,
                        ) ?: return@forEach

                val uuid: MutableSet<UUID> = mutableSetOf()
                val uuidTag =
                    it.getList(ATTRIBUTE_UUID_TAG, Tag.TAG_STRING.toInt())
                uuid.addAll(uuidTag.map { t -> UUID.fromString(t.asString) })

                this.activeModifiers[attribute] = uuid
            }
        }
    }

    override fun copy(
        oldPlayer: Player?,
        wasDeath: Boolean,
    ) {
        if (oldPlayer == null || oldPlayer.foodData !is BulkingFoodData) {
            return
        }

        val originalDietTracker =
            (oldPlayer.foodData as BulkingFoodData).dietTracker

        if ((wasDeath && BulkingConfig.BulkingCommonConfig.KEEP_STOMACH_ON_DEATH.get() == true) || !wasDeath) {
            this.stomach.addAll(
                (oldPlayer.foodData as BulkingFoodData).dietTracker.stomach,
            )
        }

        this.eatenFood.clear()
        this.eatenFood.addAll(originalDietTracker.eatenFood)

        if (wasDeath &&
            DietConfig.SERVER.deathPenaltyMethod.get() != DietConfig.DeathPenaltyMethod.RESET
        ) {
            val loss =
                (DietConfig.SERVER.deathPenaltyLoss.get() as Int).toFloat() / 100.0f
            val newNutrition =
                originalDietTracker.nutrition.map {
                    val newNutritionData: NutritionData = mutableMapOf()
                    if (DietConfig.SERVER.deathPenaltyMethod.get() == DietConfig.DeathPenaltyMethod.AMOUNT) {
                        it.entries.forEach { (k, v) ->
                            newNutritionData[k] = (v - loss).coerceAtLeast(0f)
                        }
                    } else {
                        it.entries.forEach { (k, v) ->
                            newNutritionData[k] = v * (1.0f - loss)
                        }
                    }

                    newNutritionData
                }

            this.nutrition.addAll(newNutrition)
        }

        this.active = originalDietTracker.active
        if (!wasDeath) {
            this.modifiers = originalDietTracker.modifiers
        }
    }

    override fun tick() {
        // NO-OP
    }

    override fun initSuite() {
        // NO-OP; we will calculate functionally
    }

    override fun setValue(
        group: String?,
        value: Float,
    ) {
        // NO-OP, you cannot really do that in functional style
    }

    override fun setValues(values: NutritionData?) {
        // NO-OP, you cannot really do that in functional style
    }

    override fun captureStack(stack: ItemStack?) {
        // NO-OP; we have other ways of capturing
    }
}
