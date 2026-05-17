package org.jetbrains.plugins.template.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.plugins.template.services.PluginSettings
import org.jetbrains.plugins.template.utils.SoundPlayer
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent

class CrashSoundConfigurable : Configurable {
    private val enabledCheckBox = JCheckBox("Enable KrashMe")
    private val soundPathField = TextFieldWithBrowseButton()
    private val builtInSoundCombo = ComboBox<String>()
    private val testButton = JButton("Test Sound")
    private val defaultButton = JButton("Use Default Sound")
    private val randomButton = JButton("Random Sound")

    override fun getDisplayName(): String = "KrashMe"

    override fun createComponent(): JComponent {
        val names = mutableListOf("Random")
        names.addAll(SoundPlayer.BUILT_IN_SOUNDS.keys)
        builtInSoundCombo.model = DefaultComboBoxModel(names.toTypedArray())

        soundPathField.addBrowseFolderListener(
            "Select Sound File",
            "Choose a .wav or .mp3 file to play when a crash is detected",
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor().withFileFilter { 
                it.extension?.lowercase() in listOf("wav", "mp3")
            }
        )

        testButton.addActionListener {
            val customPath = soundPathField.text.takeIf { it.isNotEmpty() }
            val selectedName = builtInSoundCombo.selectedItem as? String
            val filename = if (selectedName == "Random") "RANDOM" else SoundPlayer.BUILT_IN_SOUNDS[selectedName]
            SoundPlayer.playCrashSound(customPath, filename)
        }

        defaultButton.addActionListener {
            soundPathField.text = ""
            val defaultEntry = SoundPlayer.BUILT_IN_SOUNDS.entries.find { it.value == "faaaa.mp3" }
            builtInSoundCombo.selectedItem = defaultEntry?.key ?: "Faaaa"
        }

        randomButton.addActionListener {
            builtInSoundCombo.selectedItem = "Random"
            soundPathField.text = ""
            SoundPlayer.playCrashSound(null, "RANDOM")
        }

        return panel {
            row {
                cell(enabledCheckBox)
            }
            row("Built-in sound:") {
                cell(builtInSoundCombo)
                comment("Played when 'Custom sound file' field below is empty.")
            }
            row("Custom sound file (.wav, .mp3):") {
                cell(soundPathField).align(AlignX.FILL)
                cell(testButton)
                cell(defaultButton)
                cell(randomButton)
            }
        }
    }

    override fun isModified(): Boolean {
        val settings = PluginSettings.instance.state
        val selectedName = builtInSoundCombo.selectedItem as? String
        val selectedFile = if (selectedName == "Random") "RANDOM" else SoundPlayer.BUILT_IN_SOUNDS[selectedName]
        
        return enabledCheckBox.isSelected != settings.isSoundEnabled ||
                soundPathField.text != settings.customSoundPath ||
                selectedFile != settings.selectedBuiltInSound
    }

    override fun apply() {
        val settings = PluginSettings.instance.state
        settings.isSoundEnabled = enabledCheckBox.isSelected
        settings.customSoundPath = soundPathField.text
        
        val selectedName = builtInSoundCombo.selectedItem as? String
        settings.selectedBuiltInSound = if (selectedName == "Random") "RANDOM" else (SoundPlayer.BUILT_IN_SOUNDS[selectedName] ?: "faaaa.mp3")
    }

    override fun reset() {
        val settings = PluginSettings.instance.state
        enabledCheckBox.isSelected = settings.isSoundEnabled
        soundPathField.text = settings.customSoundPath
        
        if (settings.selectedBuiltInSound == "RANDOM") {
            builtInSoundCombo.selectedItem = "Random"
        } else {
            val entry = SoundPlayer.BUILT_IN_SOUNDS.entries.find { it.value == settings.selectedBuiltInSound }
            builtInSoundCombo.selectedItem = entry?.key ?: SoundPlayer.BUILT_IN_SOUNDS.keys.firstOrNull()
        }
    }
}
