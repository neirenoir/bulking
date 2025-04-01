package dev.neire.mc.bulking.mixin;

import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.platform.Services;
import dev.neire.mc.bulking.common.BulkingFoodData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unused")
@Mixin(ServerPlayerGameMode.class)
public class MixinServerPlayerGameMode {

    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/level/block/state/BlockState.use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"),
            method = "useItemOn")
    public void bulking$preBlockActivated(
            ServerPlayer player, Level world, ItemStack stack,
            InteractionHand hand, BlockHitResult result,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (player == null || !(player.getFoodData() instanceof BulkingFoodData)) {
            return;
        }

        BlockPos pos = result.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        ItemStack blockStack;
        try {
            blockStack = Services.REGISTRY.getPickStack(state, result, world, pos, player);
        } catch (Exception e) {
            DietConstants.LOG.debug("Error getting stack from {}", state);
            return;
        }
        ((BulkingFoodData) player.getFoodData()).setCapturedBlock(blockStack);
    }

    @Inject(
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "net/minecraft/world/level/block/state/BlockState.use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"),
            method = "useItemOn")
    public void bulking$postBlockActivated(ServerPlayer player, Level world, ItemStack stack,
                                        InteractionHand hand, BlockHitResult result,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null || !(player.getFoodData() instanceof BulkingFoodData)) {
            return;
        }
        ((BulkingFoodData) player.getFoodData()).setCapturedBlock(ItemStack.EMPTY);
    }
}