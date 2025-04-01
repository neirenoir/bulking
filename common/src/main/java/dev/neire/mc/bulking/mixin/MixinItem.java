package dev.neire.mc.bulking.mixin;

import dev.neire.mc.bulking.common.BulkingFoodData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(Item.class)
public class MixinItem {
    @Redirect(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;canEat(Z)Z"
            )
    )
    private boolean bulking$redirectCanEat(Player player, boolean ignoreHunger, Level level, Player playerAgain, InteractionHand hand) {
        // Get the item being used
        ItemStack itemStack = player.getItemInHand(hand);

        FoodData hunger = player.getFoodData();

        if (hunger instanceof BulkingFoodData) {
            return ((BulkingFoodData) hunger).canEat(itemStack);
        } else {
            // This will never return null because use() already checks if
            // it is edible by this point
            return player.canEat(
                    Objects.requireNonNull(
                            itemStack.getItem().getFoodProperties()
                    ).canAlwaysEat()
            );
        }
    }
}