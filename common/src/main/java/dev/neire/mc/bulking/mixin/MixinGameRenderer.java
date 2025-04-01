package dev.neire.mc.bulking.mixin;

import dev.neire.mc.bulking.config.BulkingConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.throwables.MixinException;

@Mixin(Gui.class)
public class MixinGameRenderer {
    @Shadow
    private int getVehicleMaxHearts(LivingEntity livingEntity) {
        throw new MixinException("Could not apply Bulking HUD mixin?");
    }

    @Redirect(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;getVehicleMaxHearts(Lnet/minecraft/world/entity/LivingEntity;)I"), require = 0)
    private int onGetMountHealth(Gui hud, LivingEntity entity)
    {
        // This tricks the code into thinking that there will be a mount
        // health bar to be rendered instead of the hunger bar.
        // Huge thanks to @matthewperiut for finding this out
        if (BulkingConfig.BulkingClientConfig.INSTANCE.getHIDE_VANILLA_HUNGER().get()) {
            return -1;
        } else {
            return this.getVehicleMaxHearts(entity);
        }
    }
}
