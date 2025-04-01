package dev.neire.mc.bulking.common

import dev.neire.mc.bulking.Bulking
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

object Snacks {
    val SNACK_TAG: TagKey<Item> =
        TagKey.create(
            Registries.ITEM,
            ResourceLocation(Bulking.MOD_ID, "snack"),
        )

    fun ItemStack.isSnack(): Boolean = this.`is`(SNACK_TAG)
}
