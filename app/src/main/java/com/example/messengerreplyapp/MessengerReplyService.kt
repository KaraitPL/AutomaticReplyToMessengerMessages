package com.example.messengerreplyapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Path
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MessengerReplyService : AccessibilityService() {

    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Obsługa powiadomień z Messengera
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val parcelableData = event.parcelableData
            if (parcelableData is android.app.Notification) {
                val extras = parcelableData.extras
                val packageName = event.packageName
                if (packageName == "com.facebook.orca") {
                    val contentIntent = parcelableData.contentIntent
                    try {
                        contentIntent.send() // Otwórz powiadomienie
                        Log.d("MessengerReplyService", "Opened Messenger notification")
                    } catch (e: PendingIntent.CanceledException) {
                        Log.e("MessengerReplyService", "Failed to open notification: ${e.message}")
                    }


                    // Dodaj opóźnienie, aby Messenger mógł w pełni załadować widok
                    Handler(Looper.getMainLooper()).postDelayed({
                        val rootNode = rootInActiveWindow ?: return@postDelayed

                        // Znajdź pole tekstowe i wprowadź odpowiedź
                        val inputField = findNodeByText(rootNode, "Write a message…")
                            ?: findNodeByClass(rootNode, "android.widget.EditText")

                        if (inputField != null) {
                            inputField.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                                putCharSequence(
                                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                    "Hello! This is an automatic reply."
                                )
                            })
                            Log.d("MessengerReplyService", "Message typed successfully")


                            Handler(Looper.getMainLooper()).postDelayed({
                                val targetNode = findNodeByContentDescriptionStartsWith(rootNode, "Wyślij")
                                if (targetNode != null) {
                                    if (targetNode.isClickable) {
                                        targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                        Log.d("MessengerReplyService", "Clicked on ViewGroup with contentDescription starting with 'Wyślij'")

                                        // Wracamy do poprzedniego ekranu
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            /*if (performGlobalAction(GLOBAL_ACTION_BACK)) {
                                                Log.d("MessengerReplyService", "Performed BACK action successfully")
                                            } else {
                                                Log.e("MessengerReplyService", "Failed to perform BACK action")
                                            }*/

                                            // Opcjonalnie: Przejdź na ekran główny
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                if (performGlobalAction(GLOBAL_ACTION_HOME)) {
                                                    Log.d("MessengerReplyService", "Returned to HOME screen successfully")
                                                } else {
                                                    Log.e("MessengerReplyService", "Failed to perform HOME action")
                                                }
                                            }, 500) // Opcjonalne opóźnienie
                                        }, 500) // Opcjonalne opóźnienie dla BACK
                                    } else {
                                        Log.e("MessengerReplyService", "Node with contentDescription starting with 'Wyślij' is not clickable")
                                    }
                                } else {
                                    Log.e("MessengerReplyService", "Node with contentDescription starting with 'Wyślij' not found")
                                }
                            }, 500) // 3000 ms = 3 sekundy
                        } else {
                            Log.e("MessengerReplyService", "Input field not found")
                            return@postDelayed
                        }
                    }, 1000) // Opóźnienie 1 sekundy
                }
            }
        }


    }

    private fun findNodeByContentDescriptionStartsWith(node: AccessibilityNodeInfo?, prefix: String): AccessibilityNodeInfo? {
        if (node == null) return null

        // Sprawdź, czy bieżący element ma odpowiedni contentDescription
        if (node.contentDescription != null && node.contentDescription.toString().startsWith(prefix)) {
            return node
        }

        // Rekurencyjnie przeszukaj dzieci
        for (i in 0 until node.childCount) {
            val child = findNodeByContentDescriptionStartsWith(node.getChild(i), prefix)
            if (child != null) {
                return child
            }
        }
        return null
    }


    override fun onInterrupt() {
        Log.e("MessengerReplyService", "Service interrupted")
    }

    private fun findNodeByText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text != null && node.text.toString().equals(text, ignoreCase = true)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = findNodeByText(node.getChild(i), text)
            if (child != null) return child
        }
        return null
    }

    private fun findNodeByClass(node: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.className != null && node.className.toString() == className) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = findNodeByClass(node.getChild(i), className)
            if (child != null) return child
        }
        return null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Tworzenie kanału powiadomień dla API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ForegroundServiceChannel",
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        startForegroundService()
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "ForegroundServiceChannel")
            .setContentTitle("Messenger Reply Service")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Zmień na ikonę w Twojej aplikacji
            .setPriority(NotificationCompat.PRIORITY_LOW) // Ustaw priorytet powiadomienia
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}