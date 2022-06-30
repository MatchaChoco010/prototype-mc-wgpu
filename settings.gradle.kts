rootProject.name = "Prototype MC wgpu"
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        mavenLocal()
        gradlePluginPortal()
    }
}
include("wgpu_renderer")
include("fabric_mod")