package com.github.khanco.krashme.services

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(name = "CrashSoundSettings", storages = [Storage("crash_sound_settings.xml")])
class PluginSettings : PersistentStateComponent<PluginSettings.State> {
    class State {
        var isSoundEnabled: Boolean = true
        var customSoundPath: String = ""
        var selectedBuiltInSound: String = "faaaa.mp3"
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        val instance: PluginSettings
            get() = service()
    }
}
