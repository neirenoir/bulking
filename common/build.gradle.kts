architectury {
    common(rootProject.property("enabled_platforms").toString().split(","))
}

loom {
    accessWidenerPath.set(file("src/main/resources/bulking.accesswidener"))
}

dependencies {
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_api_version")}")
    // Remove the next line if you don't want to depend on the API
    modCompileOnlyApi("dev.architectury:architectury:${rootProject.property("architectury_api_version")}")

    // CC
    modCompileOnlyApi(
        "dev.onyxstudios.cardinal-components-api:cardinal-components-base:${rootProject.property("cardinal_components_version")}",
    )
    modCompileOnlyApi(
        "dev.onyxstudios.cardinal-components-api:cardinal-components-entity:${rootProject.property("cardinal_components_version")}",
    )

    // FCAP
    modCompileOnly("fuzs.forgeconfigapiport:forgeconfigapiport-common:${rootProject.property("fcap_version")}")

    // SpectreLib
    modImplementation("com.illusivesoulworks.spectrelib:spectrelib-common:${rootProject.property("spectrelib_range")}")

    // Diet
    modCompileOnlyApi("maven.modrinth:${rootProject.property("diet_slug")}:${rootProject.property("diet_fabric_version")}")
}
