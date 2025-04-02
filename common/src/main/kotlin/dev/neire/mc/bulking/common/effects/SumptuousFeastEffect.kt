package dev.neire.mc.bulking.common.effects

import com.google.common.base.Suppliers
import dev.architectury.registry.registries.Registrar
import dev.architectury.registry.registries.RegistrarManager
import dev.neire.mc.bulking.Bulking
import dev.neire.mc.bulking.data.SumptuousFeastAttributesManager
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeMap
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import java.util.UUID

/**
 * A mob effect that boosts all attributes by a percentage.
 * Uses data-driven configuration to determine which attributes to boost
 */
object SumptuousFeastEffect :
    MobEffect(
        MobEffectCategory.BENEFICIAL,
        0x7FB8FF,
    ) {
    const val BOOST_AMOUNT = 0.1
    const val UUID_SEED_PREFIX = "sumptuous_feast_boost_"
    val MANAGER = Suppliers.memoize { RegistrarManager.get(Bulking.MOD_ID) }

    fun register() {
        val attributes: Registrar<MobEffect> =
            MANAGER.get().get(Registries.MOB_EFFECT)
        attributes.register(
            ResourceLocation(Bulking.MOD_ID, "sumptuous_feast"),
        ) { this }
    }

    override fun addAttributeModifiers(
        entity: LivingEntity,
        attributeMap: AttributeMap,
        amplifier: Int,
    ) {
        attributeMap.attributes.keys.forEach { attribute ->
            // Check if this attribute should be boosted according to config
            if (
                !SumptuousFeastAttributesManager.shouldBoostAttribute(attribute)
            ) {
                return@forEach
            }

            // Generate a deterministic UUID based on the attribute ID
            val uuid =
                UUID.nameUUIDFromBytes(
                    (UUID_SEED_PREFIX + attribute.descriptionId)
                        .toByteArray(),
                )
            val boost =
                (
                    if (!SumptuousFeastAttributesManager
                            .shouldInvertBoost(attribute)
                    ) {
                        BOOST_AMOUNT
                    } else {
                        -BOOST_AMOUNT
                    }
                ) * (amplifier + 1)
            val modifier =
                AttributeModifier(
                    uuid,
                    "Sumptuous Feast effect boost",
                    boost,
                    AttributeModifier.Operation.MULTIPLY_TOTAL,
                )

            attributeMap.getInstance(attribute)!!.addPermanentModifier(modifier)
        }
    }

    override fun removeAttributeModifiers(
        entity: LivingEntity,
        attributeMap: AttributeMap,
        amplifier: Int,
    ) {
        // Remove all attribute modifiers when the effect expires
        attributeMap.attributes.keys.forEach { attribute ->
            val uuid =
                UUID.nameUUIDFromBytes(
                    (UUID_SEED_PREFIX + attribute.descriptionId).toByteArray(),
                )
            attributeMap.getInstance(attribute)!!.removeModifier(uuid)
        }
    }

    override fun isDurationEffectTick(
        duration: Int,
        amplifier: Int,
    ): Boolean {
        // The effect only needs to be applied once when added and removed when
        // it ends
        return false
    }
}
