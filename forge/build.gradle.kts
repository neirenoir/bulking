plugins {
    id("com.github.johnrengelman.shadow")
}

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    forge.apply {
        convertAccessWideners.set(true)
        extraAccessWideners.add(
            loom.accessWidenerPath
                .get()
                .asFile.name,
        )

        mixinConfig("bulking.mixins.json")
        mixinConfig("bulking-forge.mixins.json")
    }
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentForge: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentForge.extendsFrom(common)
}

repositories {
    // KFF
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

dependencies {
    forge("net.minecraftforge:forge:${rootProject.property("forge_version")}")
    // Remove the next line if you don't want to depend on the API
    modImplementation("dev.architectury:architectury-forge:${rootProject.property("architectury_api_version")}")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionForge")) { isTransitive = false }

    // Kotlin For Forge
    implementation("thedarkcolour:kotlinforforge:${rootProject.property("kotlin_for_forge_version")}")

    // Diet
    modImplementation("com.illusivesoulworks.spectrelib:spectrelib-forge:${rootProject.property("spectrelib_range")}")
    modImplementation("maven.modrinth:${rootProject.property("diet_slug")}:${rootProject.property("diet_forge_version")}")

    // YACL
    modRuntimeOnly("fuzs.forgeconfigscreens:forgeconfigscreens-forge:${rootProject.property("fcs_version")}")
}

tasks.processResources {
    inputs.property("group", rootProject.property("maven_group"))
    inputs.property("version", project.version)

    // Copy icon to resources directory
    inputs.file(rootProject.file("common/src/main/resources/icon.png"))
    from(rootProject.file("common/src/main/resources/icon.png"))

    filesMatching("META-INF/mods.toml") {
        expand(
            mapOf(
                "group" to rootProject.property("maven_group"),
                "version" to project.version,
                "mod_id" to rootProject.property("mod_id"),
                "minecraft_version" to rootProject.property("minecraft_version"),
                "architectury_version" to rootProject.property("architectury_api_version"),
                "kotlin_for_forge_version" to rootProject.property("kotlin_for_forge_version"),
                "logoFile" to "icon.png",
            ),
        )
    }
}

tasks.shadowJar {
    exclude("fabric.mod.json")
    exclude("architectury.common.json")
    configurations = listOf(shadowCommon)
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    injectAccessWidener.set(true)
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set(null as String?)
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.sourcesJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val commonSources = project(":common").tasks.getByName<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}

components.getByName("java") {
    this as AdhocComponentWithVariants
    this.withVariantsFromConfiguration(project.configurations["shadowRuntimeElements"]) {
        skip()
    }
}
