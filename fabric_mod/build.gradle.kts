import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val archivesName = "prototype-mc-wgpu"
group = "net.orito-itsuki"
version = "0.1.0"

plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("fabric-loom") version libs.versions.fabric.loom
    id("com.github.johnrengelman.shadow") version libs.versions.shadow
    id("fr.stardustenterprises.rust.importer") version libs.versions.rust.importer
}

loom {
    accessWidenerPath.set(file("src/main/resources/prototype_mc_wgpu.accesswidener"))
}

dependencies {
    versionCatalogs {
        minecraft(libs.minecraft)
        mappings(libs.yarn)
        modImplementation(libs.fabric)
        modImplementation(libs.fabric.loader)
        modImplementation(libs.fabric.kotlin)
        modImplementation(libs.yanl)
        shadow(libs.yanl)
        rust(project(":wgpu_renderer"))
    }
}

rustImport {
    baseDir.set("/META-INF/natives")
    layout.set("hierarchical")
}

tasks {
    val javaVersion = JavaVersion.VERSION_17
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }
    withType<KotlinCompile> {
        kotlinOptions { jvmTarget = javaVersion.toString() }
    }
    jar { from("LICENSE") { rename { "${it}_${archivesName}" } } }
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
    shadowJar {
        archiveClassifier.set("dev")
        configurations = listOf(project.configurations.shadow.get())
    }
    remapJar {
        dependsOn(shadowJar)
        mustRunAfter(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
    }
}
