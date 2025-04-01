package dev.neire.mc.bulking

import dev.architectury.event.events.client.ClientPlayerEvent
import dev.architectury.event.events.common.PlayerEvent
import dev.neire.mc.bulking.common.BulkingFoodData
import dev.neire.mc.bulking.common.HealthManager
import dev.neire.mc.bulking.common.registries.BulkingAttributes
import dev.neire.mc.bulking.networking.BulkingMessages
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object Bulking {
    const val MOD_ID = "bulking"
    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    fun init() {
        BulkingMessages.registerS2CPackets()
        BulkingMessages.registerC2SPackets()
        HealthManager.register()
        BulkingAttributes.register()
        registerSpawnHandlers()
    }

    private fun registerSpawnHandlers() {
        PlayerEvent.PLAYER_CLONE.register { old, new, conqueredEnd ->
            if (new.foodData !is BulkingFoodData) {
                return@register
            }

            (new.foodData as BulkingFoodData).dietTracker.copy(old, !conqueredEnd)
        }

        PlayerEvent.PLAYER_RESPAWN.register { new, _ ->
            if (new.foodData !is BulkingFoodData) {
                return@register
            }

            val bulkingFoodData = new.foodData as BulkingFoodData
            bulkingFoodData.dietTracker.syncStomachNutrition()
            bulkingFoodData.dietTracker.syncEffects(
                bulkingFoodData.dietTracker.computeEffects(),
            )
        }

        ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register { oldPlayer, newPlayer ->
            if (oldPlayer.foodData !is BulkingFoodData ||
                newPlayer.foodData !is BulkingFoodData
            ) {
                return@register
            }
            (newPlayer.foodData as BulkingFoodData).dietTracker.eaten =
                (oldPlayer.foodData as BulkingFoodData).dietTracker.eaten
        }
    }
}
