package org.muilab.notigpt.reciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.muilab.notigpt.service.NotiListenerService

class BootUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        if (intent.action.equals("android.intent.action.BOOT_COMPLETED")) {
            val notiServiceIntent = Intent(context, NotiListenerService::class.java)
            context.startService(notiServiceIntent)
        }
    }
}