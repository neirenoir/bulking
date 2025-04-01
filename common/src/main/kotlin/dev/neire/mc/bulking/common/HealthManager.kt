package dev.neire.mc.bulking.common

import dev.architectury.event.events.common.PlayerEvent
import dev.neire.mc.bulking.config.BulkingConfig
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import java.util.UUID

object HealthManager {
    val HEALTH_MODIFIER_UUID =
        UUID.fromString("6c7f5d1e-17e2-4ec4-8586-42de711ba0f0")

    fun onSpawnPlayer(player: ServerPlayer) {
        val maxHealthModifier =
            BulkingConfig.BulkingCommonConfig.STARTING_HEALTH_MODIFIER.get()
        val maxHealthAttribute = player.getAttribute(Attributes.MAX_HEALTH)
        if (maxHealthAttribute != null) {
            maxHealthAttribute.removeModifier(HEALTH_MODIFIER_UUID)

            val modifier =
                AttributeModifier(
                    HEALTH_MODIFIER_UUID,
                    "Bulking base health reduction",
                    maxHealthModifier.toDouble(),
                    AttributeModifier.Operation.ADDITION,
                )

            maxHealthAttribute.addPermanentModifier(modifier)

            if (player.health > player.maxHealth) {
                player.health = player.maxHealth
            }
        }
    }

    fun register() {
        PlayerEvent.PLAYER_JOIN.register(this::onSpawnPlayer)
        PlayerEvent.PLAYER_RESPAWN.register { player, _ ->
            onSpawnPlayer(player)
        }
    }
}
