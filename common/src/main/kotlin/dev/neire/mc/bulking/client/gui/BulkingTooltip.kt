package dev.neire.mc.bulking.client.gui

import dev.neire.mc.bulking.common.BulkingFoodData
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import java.util.*

object BulkingTooltip {
    fun getEffects(): List<Component> {
        val foodData =
            (Minecraft.getInstance().player ?: return emptyList()).foodData
        if (foodData !is BulkingFoodData) {
            return emptyList()
        }
        val computed = BulkingScreen.computedEffects ?: return emptyList()

        val modifiers = computed.first
        val effects = computed.second

        if (modifiers.isEmpty() && effects.isEmpty()) {
            return mutableListOf()
        }

        val tooltips = mutableListOf<Component>()
        tooltips.add(Component.translatable("tooltip.diet.effects"))
        tooltips.add(Component.empty())

        val mergedAttributes = HashMap<Attribute, AttributeTooltip>()

        for (modifier in modifiers) {
            val att = modifier.key
            val mods = modifier.value
            mods.forEach { mod ->
                mergedAttributes
                    .computeIfAbsent(att) { AttributeTooltip() }
                    .merge(mod)
            }
        }

        for ((key, info) in mergedAttributes) {
            addAttributeTooltip(
                tooltips,
                info.added,
                AttributeModifier.Operation.ADDITION,
                key,
            )
            addAttributeTooltip(
                tooltips,
                info.baseMultiplier,
                AttributeModifier.Operation.MULTIPLY_BASE,
                key,
            )
            addAttributeTooltip(
                tooltips,
                info.totalMultiplier - 1.0f,
                AttributeModifier.Operation.MULTIPLY_TOTAL,
                key,
            )
        }

        val mergedEffects = HashMap<MobEffect, Int>()

        for (effect in effects) {
            mergedEffects.compute(effect.effect) { _, v ->
                v?.let { maxOf(it, effect.amplifier) } ?: effect.amplifier
            }
        }

        for ((effect1, value) in mergedEffects) {
            var iformattabletextcomponent: MutableComponent =
                Component.translatable(effect1.descriptionId)

            if (value > 0) {
                iformattabletextcomponent =
                    Component.translatable(
                        "potion.withAmplifier",
                        iformattabletextcomponent,
                        Component.translatable("potion.potency.$value"),
                    )
            }
            tooltips.add(
                iformattabletextcomponent.withStyle(effect1.category.tooltipFormatting),
            )
        }

        return tooltips
    }

    private fun addAttributeTooltip(
        tooltips: MutableList<Component>,
        amount: Float,
        operation: AttributeModifier.Operation,
        attribute: Attribute,
    ) {
        val formattedAmount: Double =
            when {
                operation != AttributeModifier.Operation.MULTIPLY_BASE &&
                    operation != AttributeModifier.Operation.MULTIPLY_TOTAL -> {
                    if (attribute == Attributes.KNOCKBACK_RESISTANCE) {
                        amount * 10.0
                    } else {
                        amount.toDouble()
                    }
                }
                else -> amount * 100.0
            }

        when {
            amount > 0.0f -> {
                tooltips.add(
                    Component
                        .translatable(
                            "attribute.modifier.plus.${operation.toValue()}",
                            ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(formattedAmount),
                            Component.translatable(attribute.descriptionId),
                        ).withStyle(ChatFormatting.BLUE),
                )
            }
            amount < 0.0f -> {
                val absAmount = -formattedAmount
                tooltips.add(
                    Component
                        .translatable(
                            "attribute.modifier.take.${operation.toValue()}",
                            ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(absAmount),
                            Component.translatable(attribute.descriptionId),
                        ).withStyle(ChatFormatting.RED),
                )
            }
        }
    }

    private class AttributeTooltip {
        var added = 0f
        var baseMultiplier = 0.0f
        var totalMultiplier = 1.0f

        fun merge(modifier: AttributeModifier) {
            val amount = modifier.amount.toFloat()

            when (modifier.operation) {
                AttributeModifier.Operation.MULTIPLY_BASE -> baseMultiplier += amount
                AttributeModifier.Operation.MULTIPLY_TOTAL -> totalMultiplier *= 1.0f + amount
                else -> added += amount
            }
        }
    }
}
