package dev.neire.mc.bulking.forge.mixin;

import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.type.IDietResult;
import com.illusivesoulworks.diet.client.DietClientEvents;
import dev.neire.mc.bulking.common.BulkingDietTracker;
import dev.neire.mc.bulking.common.BulkingFoodData;
import dev.neire.mc.bulking.config.BulkingConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(DietClientEvents.class)
public class MixinDietClientEvents {
    @Shadow(remap = false)
    @Final
    private static TagKey<Item> SPECIAL_FOOD;

    @Inject(
            method = "renderItemTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;entrySet()Ljava/util/Set;",
                    shift = At.Shift.AFTER
            ),
            remap = false,
            cancellable = true
    )
    private static void modifyTooltipValues(
            Player player,
            ItemStack stack,
            List<Component> tooltips,
            CallbackInfo ci
    ) {
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

        IDietResult result = DietApi.getInstance().get(player, stack);
        Map<IDietGroup, Float> groups = result.get();
        boolean specialFood = stack.is(SPECIAL_FOOD);
        List<Component> groupsTooltips = new ArrayList<>();

        List<MutableComponent> beneficial = new ArrayList<>();
        List<MutableComponent> harmful = new ArrayList<>();

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