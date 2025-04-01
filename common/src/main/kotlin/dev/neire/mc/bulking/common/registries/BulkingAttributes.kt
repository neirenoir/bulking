package dev.neire.mc.bulking.common.registries

import com.google.common.base.Suppliers
import dev.architectury.registry.registries.Registrar
import dev.architectury.registry.registries.RegistrarManager
import dev.neire.mc.bulking.Bulking
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.RangedAttribute

object BulkingAttributes {
    val MANAGER = Suppliers.memoize { RegistrarManager.get(Bulking.MOD_ID) }

    val STOMACH_SIZE_ATTRIBUTE =
        RangedAttribute(
            "attribute.bulking.stomach_size",
            3.0,
            0.0,
            100.0,
        ).setSyncable(true)

    fun register() {
        val attributes: Registrar<Attribute> =
            MANAGER.get().get(Registries.ATTRIBUTE)
        attributes.register(
            ResourceLocation(Bulking.MOD_ID, "stomach_size"),
        ) { STOMACH_SIZE_ATTRIBUTE }
    }
}
