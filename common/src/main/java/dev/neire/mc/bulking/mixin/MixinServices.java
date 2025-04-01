package dev.neire.mc.bulking.mixin;

import com.illusivesoulworks.diet.platform.Services;
import com.illusivesoulworks.diet.platform.services.ICapabilityService;
import dev.neire.mc.bulking.common.capabilities.BulkingCapabilityService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Services.class, remap = false)
public class MixinServices {
    @Redirect(
            method = "<clinit>", // Static initializer
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/illusivesoulworks/diet/platform/Services;load(Ljava/lang/Class;)Ljava/lang/Object;"
            )
    )
    @SuppressWarnings("unchecked")
    private static <T> T bulking$redirectLoad(Class<T> clazz) {
        if (clazz == ICapabilityService.class) {
            return (T) BulkingCapabilityService.INSTANCE;
        }

        return Services.load(clazz);
    }
}