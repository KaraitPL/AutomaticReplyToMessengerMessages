package com.example.messengerreplyapp
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var messageTextView: TextView
    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val treeStructure = intent?.getStringExtra("tree_structure") ?: "No data received"
            val sender = intent?.getStringExtra("sender")
            val message = intent?.getStringExtra("message")
            messageTextView.text = treeStructure
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(intent)

        messageTextView = findViewById(R.id.messageTextView)

        if (!isAccessibilityServiceEnabled(this, MessengerReplyService::class.java)) {
            Toast.makeText(this, "Włącz usługę w ustawieniach dostępności", Toast.LENGTH_LONG).show()
            openAccessibilitySettings(this)
        }

        // Zarejestruj odbiornik
        val intentFilter = IntentFilter("com.example.messengerreplyapp.NEW_MESSAGE")
        registerReceiver(messageReceiver, intentFilter)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Wyrejestruj odbiornik
        unregisterReceiver(messageReceiver)
    }

    fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val serviceName = "${context.packageName}/${service.name}"
        return !TextUtils.isEmpty(enabledServices) && enabledServices.contains(serviceName)
    }
}