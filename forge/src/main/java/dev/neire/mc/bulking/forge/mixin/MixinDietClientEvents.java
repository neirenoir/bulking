package dev.neire.mc.bulking.forge.mixin;

import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.client.DietClientEvents;
import dev.neire.mc.bulking.common.BulkingDietTracker;
import dev.neire.mc.bulking.config.BulkingConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(DietClientEvents.class)
public class MixinDietClientEvents {
    @Redirect(
            method = "renderItemTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;entrySet()Ljava/util/Set;"
            ),
            remap = false
    )
    private static Set<Map.Entry<IDietGroup, Float>> redirectEntrySet(Map<IDietGroup, Float> groups, Player player, ItemStack stack, List<Component> tooltips) {
        float nutritionWeight = BulkingConfig.BulkingCommonConfig.INSTANCE.getNUTRITION_WEIGHT().get().floatValue();
        float stomachWeight = 1f - nutritionWeight;

        Map<IDietGroup, Float> modifiedGroups = BulkingDietTracker.Companion.applyStomachFormula(groups, stomachWeight);

        return modifiedGroups.entrySet();
    }
}