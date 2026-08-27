package notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.example.myapplication.R
import data.TaskDatabase
import data.TaskRepository
import data.RepeatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.annotation.SuppressLint

class TaskAlarmReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val taskId =
            intent.getIntExtra(
                NotificationScheduler.EXTRA_TASK_ID,
                -1
            )

        val title =
            intent.getStringExtra(
                NotificationScheduler.EXTRA_TITLE
            ) ?: "Study Task"

        val subject =
            intent.getStringExtra(
                NotificationScheduler.EXTRA_SUBJECT
            ) ?: ""

        if (taskId == -1) {
            return
        }

        createNotificationChannel(
            context
        )

        val notificationText =
            if (subject.isNotEmpty()) {
                "$title • $subject"
            } else {
                title
            }

        /*
         * DONE ACTION
         */

        val doneIntent =
            Intent(
                context,
                NotificationActionReceiver::class.java
            ).apply {

                action =
                    NotificationActionReceiver.ACTION_DONE

                putExtra(
                    NotificationActionReceiver.EXTRA_TASK_ID,
                    taskId
                )
            }

        val donePendingIntent =
            PendingIntent.getBroadcast(
                context,
                taskId + 10000,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        /*
         * SNOOZE ACTION
         */

        val snoozeIntent =
            Intent(
                context,
                NotificationActionReceiver::class.java
            ).apply {

                action =
                    NotificationActionReceiver.ACTION_SNOOZE

                putExtra(
                    NotificationActionReceiver.EXTRA_TASK_ID,
                    taskId
                )

                putExtra(
                    NotificationActionReceiver.EXTRA_TITLE,
                    title
                )

                putExtra(
                    NotificationActionReceiver.EXTRA_SUBJECT,
                    subject
                )
            }

        val snoozePendingIntent =
            PendingIntent.getBroadcast(
                context,
                taskId + 20000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        /*
         * CREATE NOTIFICATION
         */

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    R.mipmap.ic_launcher
                )
                .setContentTitle(
                    "StudyMate Reminder"
                )
                .setContentText(
                    notificationText
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                    NotificationCompat.CATEGORY_REMINDER
                )
                .setAutoCancel(false)
                .setDefaults(
                    NotificationCompat.DEFAULT_ALL
                )

                /*
                 * DONE
                 */

                .addAction(
                    android.R.drawable.ic_menu_save,
                    "Done",
                    donePendingIntent
                )

                /*
                 * SNOOZE 10 MIN
                 */

                .addAction(
                    android.R.drawable.ic_popup_sync,
                    "Snooze 10 min",
                    snoozePendingIntent
                )

                .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                taskId,
                notification
            )

        /*
         * RESCHEDULE IF REPEATING
         */

        val database =
            TaskDatabase.getDatabase(context)

        val repository =
            TaskRepository(database.taskDao())

        val scheduler =
            NotificationScheduler(context)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {

            try {
                val task =
                    repository.getTaskById(taskId)

                if (
                    task != null &&
                    (task.repeat != "None" || task.repeatType != RepeatType.NONE)
                ) {

                    scheduler.rescheduleRepeatingTask(
                        task,
                        repository
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun createNotificationChannel(
        context: Context
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
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

            notificationManager
                .createNotificationChannel(
                    channel
                )
        }
    }

    companion object {

        const val CHANNEL_ID =
            "studymate_reminders"
    }
}