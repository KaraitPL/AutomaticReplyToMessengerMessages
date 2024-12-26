package com.example.messengerreplyapp

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MessengerNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            val packageName = it.packageName
            if (packageName == "com.facebook.orca") { // Messenger package name
                val extras = it.notification.extras
                val message = extras.getCharSequence("android.text").toString()
                val sender = extras.getCharSequence("android.title").toString()

                // Wyślij broadcast do MainActivity
                val intent = Intent("com.example.messengerreplyapp.NEW_MESSAGE")
                intent.putExtra("sender", sender)
                intent.putExtra("message", message)
                sendBroadcast(intent)
            }
        }
    }
}
