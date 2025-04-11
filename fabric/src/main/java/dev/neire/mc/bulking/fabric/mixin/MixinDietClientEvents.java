package dev.neire.mc.bulking.fabric.mixin;

import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.client.DietClientEvents;
import com.llamalad7.mixinextras.sugar.Local;
import dev.neire.mc.bulking.common.BulkingDietTracker;
import dev.neire.mc.bulking.common.BulkingFoodData;
import dev.neire.mc.bulking.config.BulkingConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(DietClientEvents.class)
public class MixinDietClientEvents {
    @Inject(
            method = "renderItemTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;entrySet()Ljava/util/Set;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private static void modifyTooltipValues(
            Player player,
            ItemStack stack,
            List<Component> tooltips,
            CallbackInfo ci,
            @Local Map<IDietGroup, Float> groups,
            @Local boolean specialFood,
            @Local(ordinal = 1) List<MutableComponent> groupsTooltips,
            @Local(ordinal = 2) List<MutableComponent> beneficial,
            @Local(ordinal = 3) List<MutableComponent> harmful
    ) {
        // Clear the original tooltips that would be added
        beneficial.clear();
        harmful.clear();

        boolean shouldShow = true;
        FoodData foodData = player.getFoodData();
        if (foodData instanceof BulkingFoodData) {
            shouldShow =
                    ((BulkingFoodData) foodData)
                            .getDietTracker()
                            .getEaten()
                            .contains(stack.getItem())
                    || player.isCreative();
        } else {
            shouldShow = false;
        }

        if (!shouldShow) {
            ci.cancel();
            return;
        }

        float nutritionWeight = BulkingConfig.BulkingCommonConfig.INSTANCE.getNUTRITION_WEIGHT().get().floatValue();
        float stomachWeight = 1f - nutritionWeight;

        Map<IDietGroup, Float> modifiedGroups = BulkingDietTracker.Companion.applyStomachFormula(groups, stomachWeight);

        // Add our modified tooltips
        for (Map.Entry<IDietGroup, Float> entry : modifiedGroups.entrySet()) {
            float value = entry.getValue();
            Component groupName = Component.translatable(
                    "groups.diet." + entry.getKey().getName() + ".name");
            MutableComponent tooltip = null;

            if (specialFood) {
                tooltip = Component.translatable("tooltip.diet.group_", groupName);
            } else if (value > 0.0f) {
                // Format value according to your formula
                tooltip = Component.translatable("tooltip.diet.group",
                        String.format("%.1f", value * 100), groupName);
            }

            if (tooltip != null) {
                if (entry.getKey().isBeneficial()) {
                    tooltip.withStyle(ChatFormatting.GREEN);
                    beneficial.add(tooltip);
                } else {
                    tooltip.withStyle(ChatFormatting.RED);
                    harmful.add(tooltip);
                }
            }
        }

        groupsTooltips.addAll(beneficial);
        groupsTooltips.addAll(harmful);
        if (!groupsTooltips.isEmpty()) {
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("tooltip.diet.eaten").withStyle(ChatFormatting.GRAY));
            tooltips.addAll(groupsTooltips);
        }

        ci.cancel();
    }
}