package dev.neire.mc.bulking.forge

import dev.architectury.platform.forge.EventBuses
import dev.neire.mc.bulking.Bulking
import dev.neire.mc.bulking.common.BulkingFoodData
import dev.neire.mc.bulking.config.BulkingConfig
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraftforge.client.event.RenderGuiOverlayEvent
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext

@Mod(Bulking.MOD_ID)
class BulkingForge {
    init {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(
            Bulking.MOD_ID,
            FMLJavaModLoadingContext.get().modEventBus,
        )

        MinecraftForge.EVENT_BUS.register(this)

        val modLoadingContext = ModLoadingContext.get()
        modLoadingContext.registerConfig(
            ModConfig.Type.SERVER,
            BulkingConfig.BulkingCommonConfig.SPEC,
        )

        modLoadingContext.registerConfig(
            ModConfig.Type.CLIENT,
            BulkingConfig.BulkingClientConfig.SPEC,
        )

        // Run our common setup.
        Bulking.init()
    }

    @SubscribeEvent
    fun onItemUseFinish(event: LivingEntityUseItemEvent.Finish) {
        // This method is required because Forge decided to be special
        // and not call the eat(Item, ItemStack) method
        val entity = event.entity
        val item = event.item

        // Check if the entity is a player and the item is food
        if (entity !is Player || !item.item.isEdible) {
            return
        }

        // Check if we have BulkingDietTracker
        if (entity.foodData is BulkingFoodData) {
            (entity.foodData as BulkingFoodData).eat(event.item.item, event.item)
            if (event.item.item != Items.ROTTEN_FLESH) {
                val heal =
                    event.item
                        .getFoodProperties(event.entity)
                        ?.nutrition
                        ?.toFloat()
                if (heal != null) {
                    event.entity.heal(heal)
                }
            }
        }
    }

    @SubscribeEvent
    fun onPlayerWakeUp(event: PlayerWakeUpEvent?) {
        if (event != null && event.wakeImmediately()) {
            val foodData = event.entity.foodData
            if (foodData !is BulkingFoodData) {
                return
            }

            foodData.dietTracker.digest()
        }
    }

    @SubscribeEvent
    fun onHudRender(event: RenderGuiOverlayEvent.Pre) {
        if (event.overlay == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.isCanceled = true
        }
    }
}
