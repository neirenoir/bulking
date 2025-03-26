package dev.neire.mc.bulking.forge

import dev.architectury.platform.forge.EventBuses
import dev.neire.mc.bulking.Bulking
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext

@Mod(Bulking.MOD_ID)
class BulkingForge {
    init {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(
            Bulking.MOD_ID,
            FMLJavaModLoadingContext.get().modEventBus,
        )

        // Run our common setup.
        Bulking.init()
    }
}
