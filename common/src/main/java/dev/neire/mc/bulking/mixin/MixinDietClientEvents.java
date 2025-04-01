package dev.neire.mc.bulking.mixin;

import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.client.DietClientEvents;
import com.illusivesoulworks.diet.client.screen.DietScreen;
import com.illusivesoulworks.diet.client.screen.DynamicButton;
import com.llamalad7.mixinextras.sugar.Local;
import dev.neire.mc.bulking.client.gui.BulkingScreen;
import dev.neire.mc.bulking.common.BulkingDietTracker;
import dev.neire.mc.bulking.config.BulkingConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(DietClientEvents.class)
public class MixinDietClientEvents {

    /**
     * Redirects the tick method to use our CustomDietScreen instead of the original DietScreen
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
            ),
            remap = true
    )
    private static void redirectSetScreen(Minecraft minecraft, Screen screen) {
        if (screen instanceof DietScreen) {
            boolean fromInventory = minecraft.screen instanceof InventoryScreen;
            minecraft.setScreen(new BulkingScreen(fromInventory));
        } else {
            minecraft.setScreen(screen);
        }
    }

    /**
     * Redirects the getButton method to return a button that opens our CustomDietScreen
     */
    @Redirect(
            method = "getButton",
            at = @At(
                    value = "NEW",
                    target = "com/illusivesoulworks/diet/client/screen/DynamicButton"
            ),
            remap = false
    )
    private static DynamicButton redirectButtonCreation(
            AbstractContainerScreen<?> containerScreen,
            int x, int y, int width, int height,
            int xTexStart, int yTexStart, int yDiffText,
            net.minecraft.resources.ResourceLocation resourceLocation,
            net.minecraft.client.gui.components.Button.OnPress onPress) {

        return new DynamicButton(
                containerScreen,
                x, y, width, height,
                xTexStart, yTexStart, yDiffText,
                resourceLocation,
                button -> Minecraft.getInstance().setScreen(new BulkingScreen(true))
        );
    }

    @Inject(
            method = "renderItemTooltip",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private static void onRenderItemTooltip(Player player, ItemStack stack, List<Component> tooltips, CallbackInfo ci) {
        if (stack.getItem() == Items.ROTTEN_FLESH) {
            // Add special tooltip for Rotten Flesh
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("tooltip.bulking.rotten_flesh")
                    .withStyle(ChatFormatting.RED));
            ci.cancel();
        }
    }

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

        // Calculate modified values using your formula
        float nutritionWeight = BulkingConfig.BulkingCommonConfig.INSTANCE.getNUTRITION_WEIGHT().get().floatValue();
        float stomachWeight = 1f - nutritionWeight;

        // Convert the diet groups to use your formula
        Map<IDietGroup, Float> modifiedGroups = applyStomachFormula(groups, stomachWeight);

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

    /**
     * Apply the stomach formula to the diet groups
     */
    @Unique
    private static Map<IDietGroup, Float> applyStomachFormula(Map<IDietGroup, Float> groups, float stomachWeight) {
        // This would be your actual implementation of the formula
        // For now, I'll just create a simple version based on what you provided

        Map<IDietGroup, Float> result = new java.util.HashMap<>();

        for (Map.Entry<IDietGroup, Float> entry : groups.entrySet()) {
            float value = entry.getValue();
            // Simulate your formula: value * BALANCE_AROUND / DEFAULT_STOMACH_SIZE
            float modifiedValue = value * BulkingDietTracker.BALANCE_AROUND / BulkingDietTracker.DEFAULT_STOMACH_SIZE;
            // Coerce and apply stomach weight
            modifiedValue = Math.min(modifiedValue, 1f) * stomachWeight;
            result.put(entry.getKey(), modifiedValue);
        }

        return result;
    }
}