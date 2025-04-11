package dev.neire.mc.bulking.mixin;

import com.illusivesoulworks.diet.client.DietClientEvents;
import com.illusivesoulworks.diet.client.screen.DietScreen;
import com.illusivesoulworks.diet.client.screen.DynamicButton;
import dev.neire.mc.bulking.client.gui.BulkingScreen;
import dev.neire.mc.bulking.common.BulkingFoodData;
import dev.neire.mc.bulking.common.Snacks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Mixin(DietClientEvents.class)
public class MixinDietClientEvents {
    @Inject(
            method = "renderItemTooltip",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private static void onRenderItemTooltip(Player player, ItemStack stack, List<Component> tooltips, CallbackInfo ci) {
        if (player == null) {
            ci.cancel();
            return;
        }

        if (stack.getItem() == Items.ROTTEN_FLESH) {
            // Add special tooltip for Rotten Flesh
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("tooltip.bulking.rotten_flesh")
                    .withStyle(ChatFormatting.RED));
            ci.cancel();
            return;
        }

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

        if (Snacks.INSTANCE.isSnack(stack) && shouldShow) {
            // Add special tooltip for snacks
            tooltips.add(Component.empty());
            if (stack.getItem().isEdible()) {
                int nutrition =
                        Objects.requireNonNull(
                            stack.getItem().getFoodProperties()
                        ).getNutrition();
                String hearts =
                        String.join("", Collections.nCopies(
                                 nutrition / 2, "❤"
                        ));
                MutableComponent heartComponent =
                        Component
                                .literal(hearts)
                                .withStyle(ChatFormatting.DARK_RED);
                if (nutrition % 2 == 1) {
                    // Add a "half heart" as a lighter heart icon
                    heartComponent =
                            heartComponent
                                .append("❤")
                                .withStyle(ChatFormatting.RED);
                }

                tooltips.add(
                    heartComponent
                );
                ci.cancel();
            }

        }
    }

    /**
     * Redirects the tick method to use our CustomDietScreen instead of the original DietScreen
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
            )
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


}
