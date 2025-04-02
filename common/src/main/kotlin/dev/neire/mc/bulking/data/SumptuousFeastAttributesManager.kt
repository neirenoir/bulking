package dev.neire.mc.bulking.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.illusivesoulworks.diet.platform.Services
import dev.architectury.registry.ReloadListenerRegistry
import dev.neire.mc.bulking.Bulking
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.entity.ai.attributes.Attribute
import java.util.function.Consumer

/**
 * Config manager for attribute boosting effects for the Sumptuous Feast effect.
 */
object SumptuousFeastAttributesManager :
    SimpleJsonResourceReloadListener(
        GsonBuilder().setPrettyPrinting().create(),
        "sumptuous_feast",
    ) {
    private val CONFIG_ID =
        ResourceLocation(Bulking.MOD_ID, "sumptuous_feast_attributes")

    // These sets store the attribute registry names
    private val POSITIVE_ATTRIBUTES: MutableSet<String> = mutableSetOf()
    private val NEGATIVE_ATTRIBUTES: MutableSet<String> = mutableSetOf()
    const val POSITIVE_ATTRIBUTES_TAG = "positive_attributes"
    const val NEGATIVE_ATTRIBUTES_TAG = "negative_attributes"

    fun register() {
        ReloadListenerRegistry.register(
            PackType.SERVER_DATA,
            this,
            CONFIG_ID,
        )
        Bulking.LOGGER.info("Registered attribute config manager")
    }

    fun shouldBoostAttribute(attribute: Attribute?): Boolean {
        if (attribute == null) return false

        val registryName: ResourceLocation =
            Services.REGISTRY.getAttributeKey(attribute)
        return POSITIVE_ATTRIBUTES.contains(registryName.toString()) ||
            NEGATIVE_ATTRIBUTES.contains(registryName.toString())
    }

    fun shouldInvertBoost(attribute: Attribute?): Boolean {
        if (attribute == null) return false

        val registryName: ResourceLocation =
            Services.REGISTRY.getAttributeKey(attribute)
        return NEGATIVE_ATTRIBUTES.contains(registryName.toString())
    }

    override fun apply(
        resourceMap: Map<ResourceLocation?, JsonElement?>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller,
    ) {
        Bulking.LOGGER.info("Loading attribute boost configuration...")

        // Clear previous configurations
        POSITIVE_ATTRIBUTES.clear()
        NEGATIVE_ATTRIBUTES.clear()

        resourceMap.forEach {
            (
                location: ResourceLocation?, jsonElement: JsonElement?,
            ),
            ->
            try {
                val config = jsonElement!!.asJsonObject

                // Load attributes to boost
                if (config.has(POSITIVE_ATTRIBUTES_TAG)) {
                    config
                        .getAsJsonArray(POSITIVE_ATTRIBUTES_TAG)
                        .forEach(
                            Consumer { element: JsonElement ->
                                val attributeName = element.asString
                                POSITIVE_ATTRIBUTES.add(attributeName)
                                Bulking.LOGGER.debug(
                                    "Added attribute to boost: {}",
                                    attributeName,
                                )
                            },
                        )
                }

                // Load attributes to invert
                if (config.has(NEGATIVE_ATTRIBUTES_TAG)) {
                    config
                        .getAsJsonArray(NEGATIVE_ATTRIBUTES_TAG)
                        .forEach(
                            Consumer { element: JsonElement ->
                                val attributeName = element.asString
                                NEGATIVE_ATTRIBUTES.add(attributeName)
                                Bulking.LOGGER.debug(
                                    "Added attribute to invert: {}",
                                    attributeName,
                                )
                            },
                        )
                }

                Bulking.LOGGER.info(
                    "Loaded attribute configuration from {}",
                    location,
                )
            } catch (e: Exception) {
                Bulking.LOGGER.error(
                    "Error loading attribute configuration from {}: {}",
                    location,
                    e.message,
                )
            }
        }

        Bulking.LOGGER.info(
            "Attribute boost configuration loaded. " +
                "{} attributes to boost, " +
                "{} attributes to invert.",
            POSITIVE_ATTRIBUTES.size,
            NEGATIVE_ATTRIBUTES.size,
        )
    }
}
