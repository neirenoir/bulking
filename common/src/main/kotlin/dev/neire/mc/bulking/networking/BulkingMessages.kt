package dev.neire.mc.bulking.networking

import com.illusivesoulworks.diet.platform.Services
import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.networking.NetworkChannel
import dev.architectury.networking.NetworkManager.PacketContext
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import dev.neire.mc.bulking.Bulking
import dev.neire.mc.bulking.client.gui.BulkingScreen
import dev.neire.mc.bulking.common.BulkingFoodData
import dev.neire.mc.bulking.common.ComputedEffects
import dev.neire.mc.bulking.common.NutritionData
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.ItemStack
import java.util.*
import java.util.function.Supplier

object BulkingMessages {
    class SyncPacket(
        val stomach: MutableList<Pair<ItemStack, NutritionData>>,
        val nutrition: MutableList<NutritionData>,
    ) {
        // Constructor to decode from network
        constructor(buf: FriendlyByteBuf) : this(
            stomach =
                mutableListOf<Pair<ItemStack, NutritionData>>().apply {
                    val stomachSize = buf.readInt()
                    for (i in 0 until stomachSize) {
                        val item = buf.readItem() // Read ItemStack
                        val nutritionSize = buf.readInt()
                        val nutritionData: NutritionData = mutableMapOf()

                        for (j in 0 until nutritionSize) {
                            val key = buf.readUtf()
                            val value = buf.readFloat()
                            nutritionData[key] = value
                        }

                        add(Pair(item, nutritionData))
                    }
                },
            nutrition =
                mutableListOf<NutritionData>().apply {
                    val historySize = buf.readInt()
                    for (i in 0 until historySize) {
                        val entrySize = buf.readInt()
                        val nutritionData: NutritionData = mutableMapOf()

                        for (j in 0 until entrySize) {
                            val key = buf.readUtf()
                            val value = buf.readFloat()
                            nutritionData[key] = value
                        }

                        add(nutritionData)
                    }
                },
        )

        // Serialize to network
        fun encode(buf: FriendlyByteBuf) {
            // Write stomach contents
            buf.writeInt(stomach.size)
            stomach.forEach { (item, nutritionData) ->
                buf.writeItem(item)
                buf.writeInt(nutritionData.size)
                nutritionData.forEach { (key, value) ->
                    buf.writeUtf(key)
                    buf.writeFloat(value)
                }
            }

            // Write nutrition history
            buf.writeInt(nutrition.size)
            nutrition.forEach { nutritionData ->
                buf.writeInt(nutritionData.size)
                nutritionData.forEach { (key, value) ->
                    buf.writeUtf(key)
                    buf.writeFloat(value)
                }
            }
        }

        fun apply(contextSupplier: Supplier<PacketContext>) {
            if (Platform.getEnvironment() == Env.SERVER) {
                // This packet is only meant to be processed on the client
                return
            }

            // Apply the synced data to the client-side tracker
            val player = contextSupplier.get().player
            if (player == null || player.foodData !is BulkingFoodData) {
                return
            }
            val tracker = (player.foodData as BulkingFoodData).dietTracker
            tracker.stomach.clear()
            tracker.stomach.addAll(this.stomach)
            tracker.nutrition.clear()
            tracker.nutrition.addAll(this.nutrition)
        }
    }

    class SyncReqPacket {
        constructor() {
            // No data needed for request
        }

        constructor(buf: FriendlyByteBuf) {
            // Nothing to read
        }

        fun encode(buf: FriendlyByteBuf) {
            // Nothing to encode
        }

        // Server-side handler
        fun apply(contextSupplier: Supplier<PacketContext>) {
            val context = contextSupplier.get()
            val serverPlayer = context.player

            if (serverPlayer !is ServerPlayer) {
                // This packet is only meant to be processed on the server
                return
            }

            val foodData = serverPlayer.foodData
            if (foodData is BulkingFoodData) {
                val tracker = foodData.dietTracker
                tracker.sync()
            }
        }
    }

    /**
     * Packet for syncing bulking effects between server and client
     */
    class SyncBulkingEffects {
        private val attributeData: List<AttributeData>
        private val effectData: List<EffectData>

        data class AttributeData(
            val attributeId: ResourceLocation,
            val modifiers: List<ModifierData>,
        )

        data class ModifierData(
            val uuid: UUID,
            val name: String,
            val amount: Double,
            val operation: Int,
        )

        data class EffectData(
            val effectId: ResourceLocation,
            val amplifier: Int,
            val duration: Int,
            val ambient: Boolean,
            val visible: Boolean,
        )

        constructor() {
            this.attributeData = emptyList()
            this.effectData = emptyList()
        }

        constructor(effects: ComputedEffects) {
            // Convert attribute modifiers to raw data
            this.attributeData =
                effects.first.map { (attribute, modifiers) ->
                    AttributeData(
                        attributeId =
                            Services.REGISTRY.getAttributeKey(attribute),
                        modifiers =
                            modifiers.map { modifier ->
                                ModifierData(
                                    uuid = modifier.id,
                                    name = modifier.name,
                                    amount = modifier.amount,
                                    operation = modifier.operation.toValue(),
                                )
                            },
                    )
                }

            // Convert status effects to raw data
            this.effectData =
                effects.second.map { effect ->
                    EffectData(
                        effectId =
                            Services.REGISTRY.getStatusEffectKey(effect.effect),
                        amplifier = effect.amplifier,
                        duration = effect.duration,
                        ambient = effect.isAmbient,
                        visible = effect.isVisible,
                    )
                }
        }

        // Deserialize from network buffer
        constructor(buf: FriendlyByteBuf) {
            // Read attribute data
            val attributeCount = buf.readVarInt()
            val attributeDataList = mutableListOf<AttributeData>()

            for (i in 0 until attributeCount) {
                // Read attribute ID
                val attributeId = buf.readResourceLocation()

                // Read modifiers for this attribute
                val modifierCount = buf.readVarInt()
                val modifierDataList = mutableListOf<ModifierData>()

                for (j in 0 until modifierCount) {
                    val uuid = buf.readUUID()
                    val name = buf.readUtf()
                    val amount = buf.readDouble()
                    val operation = buf.readVarInt()

                    modifierDataList.add(
                        ModifierData(uuid, name, amount, operation),
                    )
                }

                attributeDataList.add(
                    AttributeData(attributeId, modifierDataList),
                )
            }

            this.attributeData = attributeDataList

            // Read effect data
            val effectCount = buf.readVarInt()
            val effectDataList = mutableListOf<EffectData>()

            for (i in 0 until effectCount) {
                val effectId = buf.readResourceLocation()
                val amplifier = buf.readVarInt()
                val duration = buf.readVarInt()
                val ambient = buf.readBoolean()
                val visible = buf.readBoolean()

                effectDataList.add(
                    EffectData(effectId, amplifier, duration, ambient, visible),
                )
            }

            this.effectData = effectDataList
        }

        // Serialize to network buffer
        fun encode(buf: FriendlyByteBuf) {
            // Write attribute data
            buf.writeVarInt(attributeData.size)

            for (attrData in attributeData) {
                // Write attribute ID
                buf.writeResourceLocation(attrData.attributeId)

                // Write modifiers for this attribute
                buf.writeVarInt(attrData.modifiers.size)
                for (modifier in attrData.modifiers) {
                    buf.writeUUID(modifier.uuid)
                    buf.writeUtf(modifier.name)
                    buf.writeDouble(modifier.amount)
                    buf.writeVarInt(modifier.operation)
                }
            }

            // Write effect data
            buf.writeVarInt(effectData.size)

            for (effect in effectData) {
                buf.writeResourceLocation(effect.effectId)
                buf.writeVarInt(effect.amplifier)
                buf.writeVarInt(effect.duration)
                buf.writeBoolean(effect.ambient)
                buf.writeBoolean(effect.visible)
            }
        }

        // Rebuild the effects from raw data
        fun rebuildEffects(): ComputedEffects {
            // Rebuild attribute modifiers
            val attributeModifiers =
                mutableMapOf<Attribute, MutableSet<AttributeModifier>>()

            for (attrData in attributeData) {
                val attribute =
                    Services.REGISTRY
                        .getAttribute(attrData.attributeId)
                        .ifPresent { attr ->
                            val modifierSet = mutableSetOf<AttributeModifier>()

                            for (modData in attrData.modifiers) {
                                val operation =
                                    AttributeModifier.Operation.fromValue(
                                        modData.operation,
                                    )
                                modifierSet.add(
                                    AttributeModifier(
                                        modData.uuid,
                                        modData.name,
                                        modData.amount,
                                        operation,
                                    ),
                                )
                            }

                            attributeModifiers[attr] = modifierSet
                        }
            }

            // Rebuild status effects
            val statusEffects = mutableSetOf<MobEffectInstance>()

            for (effectData in this.effectData) {
                val effect =
                    Services.REGISTRY
                        .getStatusEffect(effectData.effectId)
                        .ifPresent { fx ->
                            statusEffects.add(
                                MobEffectInstance(
                                    fx,
                                    effectData.duration,
                                    effectData.amplifier,
                                    effectData.ambient,
                                    effectData.visible,
                                ),
                            )
                        }
            }

            return Pair(attributeModifiers, statusEffects)
        }

        fun apply(contextSupplier: Supplier<PacketContext>) {
            BulkingScreen.computedEffects = rebuildEffects()
        }
    }

    val CHANNEL =
        NetworkChannel.create(ResourceLocation(Bulking.MOD_ID, "network"))

    fun registerS2CPackets() {
        // SYNC PACKET
        CHANNEL.register(
            SyncPacket::class.java,
            SyncPacket::encode,
            ::SyncPacket,
            SyncPacket::apply,
        )

        // FX SYNC PACKET
        CHANNEL.register(
            SyncBulkingEffects::class.java,
            SyncBulkingEffects::encode,
            ::SyncBulkingEffects,
            SyncBulkingEffects::apply,
        )
    }

    fun registerC2SPackets() {
        // SYNC_REQ PACKET
        CHANNEL.register(
            SyncReqPacket::class.java,
            SyncReqPacket::encode,
            ::SyncReqPacket,
            SyncReqPacket::apply,
        )

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register { player ->
            if (player.foodData !is BulkingFoodData) {
                return@register
            }
            CHANNEL.sendToServer(SyncReqPacket())
        }
    }
}
