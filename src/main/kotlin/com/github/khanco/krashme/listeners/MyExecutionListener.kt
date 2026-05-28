package com.github.khanco.krashme.listeners

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.github.khanco.krashme.services.PluginSettings
import com.github.khanco.krashme.utils.SoundPlayer
import java.util.concurrent.atomic.AtomicLong

class MyExecutionListener : ExecutionListener {
    private var lastSoundTime = AtomicLong(0)
    private val debounceMs = 5000L

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        val project = env.project
        handler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (!PluginSettings.instance.state.isSoundEnabled) return
                
                val text = event.text
                if (isCrashPattern(text)) {
                    notifyCrash(project, "Crash detected in output: ${text.take(100)}...")
                }
            }

            override fun processTerminated(event: ProcessEvent) {
                if (!PluginSettings.instance.state.isSoundEnabled) return

                if (event.exitCode != 0 && event.exitCode != 130) {
                    notifyCrash(project, "Process terminated with exit code ${event.exitCode}")
                }
            }

            override fun startNotified(event: ProcessEvent) {}
        })
    }

    private fun isCrashPattern(text: String): Boolean {
        val upperText = text.uppercase()
        return upperText.contains("FATAL EXCEPTION") ||
               upperText.contains("EXCEPTION IN THREAD") ||
               (upperText.contains("ERROR") && upperText.contains("EXCEPTION")) ||
               upperText.contains("FORCE CLOSE")
    }

    private fun notifyCrash(project: Project, message: String) {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastSoundTime.get()
        if (currentTime - lastTime > debounceMs) {
            if (lastSoundTime.compareAndSet(lastTime, currentTime)) {
                // Play crash sound
                SoundPlayer.playCrashSound()

                // Show notification
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("KrashMe")
                    .createNotification("App Crash Detected", message, NotificationType.ERROR)
                    .notify(project)
            }
        }
    }
}
