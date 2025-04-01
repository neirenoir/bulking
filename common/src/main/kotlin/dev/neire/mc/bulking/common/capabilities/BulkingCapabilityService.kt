package dev.neire.mc.bulking.common.capabilities

import com.illusivesoulworks.diet.api.type.IDietTracker
import com.illusivesoulworks.diet.common.data.DietFabricGroups
import com.illusivesoulworks.diet.common.data.DietFabricSuites
import com.illusivesoulworks.diet.common.data.group.DietGroups
import com.illusivesoulworks.diet.common.data.suite.DietSuites
import com.illusivesoulworks.diet.platform.services.ICapabilityService
import com.mojang.authlib.minecraft.InsecurePublicKeyException.InvalidException
import dev.architectury.platform.Platform
import dev.neire.mc.bulking.common.BulkingFoodData
import net.minecraft.world.entity.player.Player
import java.util.Optional

object BulkingCapabilityService : ICapabilityService {
    override fun get(player: Player?): Optional<out IDietTracker?> {
        val foodData = player?.foodData ?: return Optional.empty()

        if (foodData !is BulkingFoodData) {
            return Optional.empty()
        }

        return Optional.of(foodData.dietTracker)
    }

    override fun getGroupsListener(): DietGroups =
        if (Platform.isForge()) {
            DietGroups()
        } else if (Platform.isFabric()) {
            DietFabricGroups()
        } else {
            throw InvalidException("Only Forge and Fabric are supported")
        }

    override fun getSuitesListener(): DietSuites =
        if (Platform.isForge()) {
            DietSuites()
        } else if (Platform.isFabric()) {
            DietFabricSuites()
        } else {
            throw InvalidException("Only Forge and Fabric are supported")
        }
}
