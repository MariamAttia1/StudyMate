package android.example.myapplication.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationScheduler.EXTRA_TASK_ID, -1)
        val title = intent.getStringExtra(NotificationScheduler.EXTRA_TITLE) ?: "Task"
        val subject = intent.getStringExtra(NotificationScheduler.EXTRA_SUBJECT) ?: ""

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // temporary icon, we'll replace this later
            .setContentTitle(title)
            .setContentText(subject)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(taskId.toInt(), notification)
    }
}