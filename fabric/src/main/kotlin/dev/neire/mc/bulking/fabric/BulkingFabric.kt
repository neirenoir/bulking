package dev.neire.mc.bulking.fabric

import dev.neire.mc.bulking.Bulking
import dev.neire.mc.bulking.Bulking.init
import dev.neire.mc.bulking.common.BulkingFoodData
import dev.neire.mc.bulking.config.BulkingConfig
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
import net.minecraft.world.entity.player.Player
import net.minecraftforge.fml.config.ModConfig

class BulkingFabric : ModInitializer {
    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        init()

        EntitySleepEvents.STOP_SLEEPING.register(
            EntitySleepEvents.StopSleeping { player, _ ->
                if (player == null ||
                    player !is Player ||
                    player.foodData !is BulkingFoodData ||
                    !player.isSleepingLongEnough
                ) {
                    return@StopSleeping
                }

                (player.foodData as BulkingFoodData).dietTracker.digest()
            },
        )

        registerConfig()
    }

    private fun registerConfig() {
        ForgeConfigRegistry.INSTANCE.register(
            Bulking.MOD_ID,
            ModConfig.Type.SERVER,
            BulkingConfig.BulkingCommonConfig.SPEC,
        )

        ForgeConfigRegistry.INSTANCE.register(
            Bulking.MOD_ID,
            ModConfig.Type.CLIENT,
            BulkingConfig.BulkingClientConfig.SPEC,
        )
    }
}
