package dev.neire.mc.bulking.mixin;

import dev.neire.mc.bulking.common.BulkingDietTracker;
import dev.neire.mc.bulking.common.BulkingFoodData;
import dev.neire.mc.bulking.common.registries.BulkingAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class MixinPlayer
{
    @Shadow
    protected FoodData foodData;

    @Inject(at = @At("RETURN"), method = "<init>")
    private void bulking$onInit(CallbackInfo info)
    {
        Player player = (Player) (Object) this;
        foodData = new BulkingFoodData(player, new BulkingDietTracker(player));
    }

    @Inject(at = @At("RETURN"), method = "createAttributes()Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;")
    private static void bulking$createAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue().add(BulkingAttributes.INSTANCE.getSTOMACH_SIZE_ATTRIBUTE());
    }
}
