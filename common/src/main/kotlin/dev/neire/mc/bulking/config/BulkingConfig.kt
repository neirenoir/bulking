package dev.neire.mc.bulking.config

import dev.neire.mc.bulking.config.BulkingConfig.BulkingCommonConfig.KEEP_STOMACH_ON_DEATH
import net.minecraftforge.common.ForgeConfigSpec

object BulkingConfig {
    object BulkingCommonConfig {
        private val BUILDER = ForgeConfigSpec.Builder()
        val KEEP_STOMACH_ON_DEATH: ForgeConfigSpec.ConfigValue<Boolean>
        val STARTING_HEALTH_MODIFIER: ForgeConfigSpec.ConfigValue<Int>
        val NUTRITION_DAYS: ForgeConfigSpec.ConfigValue<Int>
        val NUTRITION_WEIGHT: ForgeConfigSpec.ConfigValue<Double>
        val USE_SUITE: ForgeConfigSpec.ConfigValue<String>
        val SPEC: ForgeConfigSpec

        init {
            BUILDER.push("General")
            KEEP_STOMACH_ON_DEATH =
                BUILDER
                    .comment(
                        "Whether the contents of the stomach should be kept " +
                            "death or not.",
                    ).define("keep_stomach_on_death", false)
            STARTING_HEALTH_MODIFIER =
                BUILDER
                    .comment(
                        "How much to add to the default 20 maximum health. " +
                            "Negative values are allowed (and encouraged!).",
                    ).defineInRange(
                        "starting_health_modifier",
                        -14,
                        -19,
                        20,
                    )
            BUILDER.pop()

            BUILDER.push("Nutrition history")
            NUTRITION_DAYS =
                BUILDER
                    .comment(
                        "Number of days that will be kept track of to determine " +
                            "historical nutrition stats.",
                    ).defineInRange(
                        "nutrition_days",
                        1,
                        0,
                        50,
                    )
            NUTRITION_WEIGHT =
                BUILDER
                    .comment(
                        "How much will historical nutrition data factor in total " +
                            "nutrition, as opposed to current stomach contents.",
                    ).defineInRange(
                        "nutrition_weight",
                        0.33,
                        0f.toDouble(),
                        1f.toDouble(),
                    )
            BUILDER.pop()

            BUILDER.push("Suites")
            USE_SUITE =
                BUILDER
                    .comment("The default Diet suite to use.")
                    .define("use_suite", "bulking_builtin")
            BUILDER.pop()

            SPEC = BUILDER.build()
        }
    }

    object BulkingClientConfig {
        private val BUILDER = ForgeConfigSpec.Builder()
        val HIDE_VANILLA_HUNGER: ForgeConfigSpec.ConfigValue<Boolean>
        val SPEC: ForgeConfigSpec

        init {
            BUILDER.push("Interface")
            HIDE_VANILLA_HUNGER =
                BUILDER
                    .comment(
                        "Should the mod hide the vanilla hunger bar?",
                    ).define("hide_vanilla_hunger", true)
            BUILDER.pop()

            SPEC = BUILDER.build()
        }
    }
}
