import java.nio.file.Paths
import kotlin.io.path.moveTo
import kotlin.io.path.ExperimentalPathApi
import java.nio.file.Files


plugins {
    java
    kotlin("jvm") version "1.7.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

subprojects {
    apply(plugin = "java")
}

var modVersion: String = project.property("global_version").toString()

//var modVersion: String = providers.gradleProperty("global_version")

var mcCore = project(":mccore")

tasks.register("buildCore") {
    dependsOn(mcCore.tasks.build)
    doLast {
        moveToOut(mcCore, "core")
    }
}

@OptIn(ExperimentalPathApi::class)
fun moveToOut(subProject: Project, versionStr: String) {
    val jarName = "Immersive Vehicles-${subProject.version}.jar"
    val source = Paths.get("${subProject.projectDir.canonicalPath}/build/libs/$jarName")
    val outDir = Paths.get("${project.projectDir.canonicalPath}/out")
    Files.createDirectories(outDir)
    source.moveTo(outDir.resolve(jarName), true)
}

@OptIn(ExperimentalPathApi::class)
fun moveToOut(moduleDirectory: String, artifactVersion: String) {
    val jarName = "Immersive Vehicles-$artifactVersion.jar"
    val source = Paths.get("${project.projectDir.canonicalPath}/$moduleDirectory/build/libs/$jarName")
    val outDir = Paths.get("${project.projectDir.canonicalPath}/out")
    Files.createDirectories(outDir)
    source.moveTo(outDir.resolve(jarName), true)
}

fun preBuild() {
    // Could probably be better somehow, but I'm not sure how
    project.projectDir.canonicalFile.walk()
        .filter { it.name == "gradle.properties" || it.name == "mcmod.info" || it.name == "InterfaceLoader.java" }
        .forEach { it.writeText(it.readText()
            .replace(Regex("mod_version=(.+)"), "mod_version=$modVersion")
            .replace(Regex("\"version\": \"[^\"]*\""), "\"version\": \"$modVersion\"")
            .replace(Regex("MODVER = \"[^\"]*\";"), "MODVER = \"$modVersion\";")) }
}
