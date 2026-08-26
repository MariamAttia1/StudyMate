package android.example.myapplication.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.example.myapplication.data.TaskEntity
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationScheduler(
    private val context: Context
) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNotification(task: TaskEntity) {

        try {

            // Convert:
            // date = 26/8/2026
            // time = 18:30
            //
            // into a real date/time.
            val dateTimeString =
                "${task.date} ${task.time}"

            val formatter =
                SimpleDateFormat(
                    "d/M/yyyy HH:mm",
                    Locale.getDefault()
                )

            formatter.isLenient = false

            val parsedDate =
                formatter.parse(dateTimeString)
                    ?: return

            val triggerTime =
                parsedDate.time

            // Don't schedule if the time has already passed.
            if (triggerTime <= System.currentTimeMillis()) {
                return
            }

            val intent =
                Intent(
                    context,
                    TaskAlarmReceiver::class.java
                ).apply {

                    putExtra(
                        EXTRA_TASK_ID,
                        task.id
                    )

                    putExtra(
                        EXTRA_TITLE,
                        task.title
                    )

                    putExtra(
                        EXTRA_SUBJECT,
                        task.subject
                    )
                }

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    task.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

            // Schedule exact alarm.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                if (alarmManager.canScheduleExactAlarms()) {

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )

                } else {

                    // Fallback if exact alarms are not allowed.
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }

            } else {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    fun cancelNotification(taskId: Int) {

        val intent =
            Intent(
                context,
                TaskAlarmReceiver::class.java
            )

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.cancel(
            pendingIntent
        )
    }

    companion object {

        const val EXTRA_TASK_ID =
            "task_id"

        const val EXTRA_TITLE =
            "title"

        const val EXTRA_SUBJECT =
            "subject"
    }
}