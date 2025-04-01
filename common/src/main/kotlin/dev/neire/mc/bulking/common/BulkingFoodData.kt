package dev.neire.mc.bulking.common

import dev.architectury.platform.Platform
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.food.FoodData
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class BulkingFoodData(
    val player: Player,
    val dietTracker: BulkingDietTracker,
) : FoodData() {
    var capturedBlock: ItemStack = ItemStack.EMPTY

    companion object {
        const val BULKING_TAG = "bulking"
    }

    init {
        dietTracker.initSuite()
    }

    override fun eat(
        hunger: Int,
        saturation: Float,
    ) {
        // Forge requires a different pathway
        if (!Platform.isForge()) {
            player.heal(hunger.toFloat())
        }

        if (capturedBlock != ItemStack.EMPTY) {
            dietTracker.consume(capturedBlock, hunger, saturation)
            capturedBlock = ItemStack.EMPTY
        }
    }

    override fun eat(
        item: Item,
        itemStack: ItemStack,
    ) {
        val foodProperties = item.foodProperties ?: return
        this.dietTracker.consume(itemStack)
        if (item != Items.ROTTEN_FLESH) {
            eat(foodProperties.nutrition, foodProperties.saturationModifier)
        }
    }

    override fun tick(player: Player) {
    }

    override fun getFoodLevel(): Int = 10

    override fun needsFood(): Boolean = this.dietTracker.hasRoomForDessert(ItemStack.EMPTY)

    fun canEat(stack: ItemStack): Boolean = this.dietTracker.hasRoomForDessert(stack)

    override fun addExhaustion(exhaustion: Float) { }

    override fun setFoodLevel(foodLevel: Int) { }

    override fun getSaturationLevel(): Float = 0f

    override fun setSaturation(saturationLevel: Float) { }

    override fun addAdditionalSaveData(compoundTag: CompoundTag) {
        super.addAdditionalSaveData(compoundTag)
        val bulkingTag = CompoundTag()
        this.dietTracker.save(bulkingTag)
        compoundTag.put(BULKING_TAG, bulkingTag)
    }

    override fun readAdditionalSaveData(compoundTag: CompoundTag) {
        super.readAdditionalSaveData(compoundTag)
        if (compoundTag.contains(BULKING_TAG)) {
            val bulkingTag = compoundTag.getCompound(BULKING_TAG)
            this.dietTracker.load(bulkingTag)
        }
    }
}
