package android.example.myapplication.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.example.myapplication.R

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val taskId = intent.getIntExtra(
            NotificationScheduler.EXTRA_TASK_ID,
            -1
        )

        val title = intent.getStringExtra(
            NotificationScheduler.EXTRA_TITLE
        ) ?: "Study Task"

        val subject = intent.getStringExtra(
            NotificationScheduler.EXTRA_SUBJECT
        ) ?: ""

        createNotificationChannel(context)

        val notificationText =
            if (subject.isNotEmpty()) {
                "$title • $subject"
            } else {
                title
            }

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("StudyMate Reminder")
                .setContentText(notificationText)
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                    NotificationCompat.CATEGORY_REMINDER
                )
                .setAutoCancel(true)
                .setDefaults(
                    NotificationCompat.DEFAULT_ALL
                )
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                taskId,
                notification
            )
    }

    private fun createNotificationChannel(
        context: Context
    ) {

        val channel = NotificationChannel(
            CHANNEL_ID,
            "StudyMate Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {

            description =
                "Reminders for StudyMate tasks"
        }

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        notificationManager.createNotificationChannel(
            channel
        )
    }

    companion object {

        const val CHANNEL_ID =
            "studymate_reminders"
    }
}