plugins {
    id("com.github.johnrengelman.shadow")
}

repositories {
    maven {
        url = uri("https://maven.quiltmc.org/repository/release/")
    }

    maven { url = uri("https://maven.terraformersmc.com/releases/") }
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentFabric: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentFabric.extendsFrom(common)
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_api_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-fabric:${rootProject.property("architectury_api_version")}")

    common(project(":common", "namedElements")) {
        isTransitive = false
    }
    shadowCommon(project(":common", "transformProductionFabric")) {
        isTransitive = false
    }

    // Fabric Kotlin
    modImplementation("net.fabricmc:fabric-language-kotlin:${rootProject.property("fabric_kotlin_version")}")

    // Cardinal Components
    modApi("dev.onyxstudios.cardinal-components-api:cardinal-components-base:${rootProject.property("cardinal_components_version")}")
    modApi("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:${rootProject.property("cardinal_components_version")}")

    // Diet
    modImplementation("com.illusivesoulworks.spectrelib:spectrelib-fabric:${rootProject.property("spectrelib_range")}")
    modImplementation("maven.modrinth:${rootProject.property("diet_slug")}:${rootProject.property("diet_fabric_version")}")

    // FCAP
    modImplementation("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${rootProject.property("fcap_version")}")
    modLocalRuntime("fuzs.forgeconfigscreens:forgeconfigscreens-fabric:${rootProject.property("fcs_version")}")

    // Mod Menu - for config testing purposes
    modRuntimeOnly("com.terraformersmc:modmenu:7.2.2")
}

tasks.processResources {
    inputs.property("group", rootProject.property("maven_group"))
    inputs.property("version", project.version)

    // Copy icon to resources directory
    inputs.file(rootProject.file("common/src/main/resources/icon.png"))
    from(rootProject.file("common/src/main/resources/icon.png"))

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "group" to rootProject.property("maven_group"),
                "version" to project.version,
                "mod_id" to rootProject.property("mod_id"),
                "minecraft_version" to rootProject.property("minecraft_version"),
                "architectury_version" to rootProject.property("architectury_api_version"),
                "fabric_kotlin_version" to rootProject.property("fabric_kotlin_version"),
            ),
        )
    }
}

tasks.shadowJar {
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
