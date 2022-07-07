package net.orito_itsuki.prototype_mc_wgpu.rust

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo

object FileExtractor {
    var isFinishFileExtract = false
    var count: Long = -1

    private fun deleteDirectory(directory: Path) {
        if (!Files.exists(directory)) return
        Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .map { it.toFile() }
            .forEach { it.delete() }
    }

    fun extract() {
        val src = javaClass.getResource("/wgpu-resources") ?: throw Exception("No resources")
        val uri = src.toURI()
        val path = if (uri.scheme == "jar") {
            val fileSystem = FileSystems.newFileSystem(uri, mutableMapOf<String, Any>())
            fileSystem.getPath("/wgpu-resources")
        } else {
            Paths.get(uri)
        }
        count = Files.walk(path).count()
        val currentDirectory = System.getProperty("user.dir")

        deleteDirectory(Path(currentDirectory, "wgpu-resources"))

        Files.walk(path)
            .filter { !it.isDirectory() }
            .forEach {
                val parent = path.parent
                val outPath = Path(currentDirectory, it.relativeTo(parent).toString())
                javaClass.classLoader.getResourceAsStream(it.relativeTo(parent).toString()).use {fis ->
                    if (fis != null) {
                        Files.createDirectories(outPath.parent)
                        Files.copy(fis, outPath)
                    }
                }
            }
        isFinishFileExtract = true
    }
}