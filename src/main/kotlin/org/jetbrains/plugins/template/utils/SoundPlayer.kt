package org.jetbrains.plugins.template.utils

import com.intellij.util.concurrency.AppExecutorUtil
import javazoom.jl.player.Player
import org.jetbrains.plugins.template.services.PluginSettings
import java.awt.Toolkit
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.*
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineEvent

object SoundPlayer {
    /**
     * Dynamically discover built-in sounds from the resources/sounds folder.
     * Returns a map of "Display Name" to "filename.mp3"
     */
    val BUILT_IN_SOUNDS: Map<String, String> by lazy {
        val sounds = mutableMapOf<String, String>()
        try {
            val resourceUri = javaClass.getResource("/sounds")?.toURI()
            if (resourceUri != null) {
                if (resourceUri.scheme == "jar") {
                    // Running from JAR (Production)
                    FileSystems.newFileSystem(resourceUri, emptyMap<String, Any>()).use { fs ->
                        val path = fs.getPath("/sounds")
                        Files.list(path).forEach { p ->
                            val filename = p.fileName.toString()
                            if (filename.endsWith(".mp3")) {
                                sounds[formatName(filename)] = filename
                            }
                        }
                    }
                } else {
                    // Running from File System (Development)
                    val folder = File(resourceUri)
                    folder.listFiles()?.forEach { file ->
                        if (file.name.endsWith(".mp3")) {
                            sounds[formatName(file.name)] = file.name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Ensure there's at least a fallback if discovery fails
        if (sounds.isEmpty()) {
            sounds["Faaaa"] = "faaaa.mp3"
        }
        sounds.toSortedMap()
    }

    private fun formatName(filename: String): String {
        return filename.removeSuffix(".mp3")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    fun playCrashSound(overridePath: String? = null, overrideBuiltInFile: String? = null) {
        AppExecutorUtil.getAppExecutorService().execute {
            try {
                val settings = PluginSettings.instance.state
                val customPath = overridePath ?: settings.customSoundPath
                
                if (customPath.isNotEmpty()) {
                    val file = File(customPath)
                    if (file.exists()) {
                        if (customPath.endsWith(".wav", ignoreCase = true)) {
                            playWav(file)
                            return@execute
                        } else if (customPath.endsWith(".mp3", ignoreCase = true)) {
                            playMp3(FileInputStream(file))
                            return@execute
                        }
                    }
                }
                
                // Fallback to selected built-in sound
                var filename = overrideBuiltInFile ?: settings.selectedBuiltInSound
                
                if (filename == "RANDOM") {
                    filename = BUILT_IN_SOUNDS.values.randomOrNull() ?: "faaaa.mp3"
                }

                val resourcePath = "/sounds/$filename"
                val stream = javaClass.getResourceAsStream(resourcePath) 
                    ?: javaClass.getResourceAsStream("/sounds/faaaa.mp3")
                
                if (stream != null) {
                    playMp3(stream)
                } else {
                    Toolkit.getDefaultToolkit().beep()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toolkit.getDefaultToolkit().beep()
            }
        }
    }

    private fun playMp3(inputStream: InputStream) {
        val bufferedIn = BufferedInputStream(inputStream)
        val player = Player(bufferedIn)
        player.play()
    }

    private fun playWav(file: File) {
        try {
            val audioStream = AudioSystem.getAudioInputStream(file)
            val format = audioStream.format
            val info = DataLine.Info(Clip::class.java, format)
            val clip = AudioSystem.getLine(info) as Clip
            clip.open(audioStream)
            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    clip.close()
                }
            }
            clip.start()
        } catch (e: Exception) {
            e.printStackTrace()
            Toolkit.getDefaultToolkit().beep()
        }
    }
}
