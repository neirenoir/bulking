import net.fabricmc.loom.api.LoomGradleExtensionAPI

tasks.wrapper {
    gradleVersion = "8.11"
    // You can either download the binary-only version of Gradle (BIN) or
    // the full version (with sources and documentation) of Gradle (ALL)
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    java
    kotlin("jvm") version "1.8.22"
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.7-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

architectury {
    minecraft = rootProject.property("minecraft_version").toString()
}

subprojects {
    apply(plugin = "dev.architectury.loom")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")
    dependencies {
        "minecraft"("com.mojang:minecraft:${project.property("minecraft_version")}")
        // The following line declares the mojmap mappings, you may use other mappings as well
        "mappings"(
            loom.officialMojangMappings(),
        )
        // The following line declares the yarn mappings you may select this one as well.
        // "mappings"("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    base.archivesName.set(rootProject.property("archives_base_name").toString())
    // base.archivesBaseName = rootProject.property("archives_base_name").toString()
    version = rootProject.property("mod_version").toString()
    group = rootProject.property("maven_group").toString()

    repositories {
        maven {
            name = "Modrinth"
            url = uri("https://api.modrinth.com/maven")
            content {
                includeGroup("maven.modrinth")
            }
        }

        maven {
            name = "Illusive Soulworks"
            url = uri("https://maven.theillusivec4.top/")
        }

        maven {
            name = "Fuzs Mod Resources"
            url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
        }

        maven {
            name = "Ladysnake Mods"
            url = uri("https://maven.ladysnake.org/releases")
        }
        // other repositories
    }

    dependencies {
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    kotlin.target.compilations.all {
        kotlinOptions.jvmTarget = "17"
    }

    java {
        withSourcesJar()
    }
}
