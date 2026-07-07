package com.gerar.controlelectrico

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val openIntent = PendingIntent.getActivity(
            context,
            802,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.medidor_original)
            .setContentTitle("Control Eléctrico")
            .setContentText("Recuerda registrar la lectura, importar el recibo y guardar un respaldo.")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(802, notification)
    }

    private companion object {
        const val CHANNEL_ID = "control_electrico_reminders"
    }
}
