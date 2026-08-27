package notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import data.TaskEntity
import data.TaskRepository
import data.RepeatType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationScheduler(
    private val context: Context
) {

    private val alarmManager =
        context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

    /*
     * NORMAL TASK NOTIFICATION
     */

    fun scheduleNotification(task: TaskEntity) {

        try {

            val dateTimeString =
                "${task.date} ${task.time}"

            val formatter =
                SimpleDateFormat(
                    "d/M/yyyy HH:mm",
                    Locale.getDefault()
                )

            formatter.isLenient = false

            val parsedDate =
                formatter.parse(
                    dateTimeString
                ) ?: return

            val triggerTime =
                parsedDate.time

            // Don't schedule past notifications.
            if (
                triggerTime <=
                System.currentTimeMillis()
            ) {
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

            scheduleAlarm(
                triggerTime,
                pendingIntent
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    /*
     * SNOOZE NOTIFICATION
     *
     * Schedules the notification
     * 10 minutes from now.
     */

    fun scheduleSnooze(
        taskId: Int,
        title: String,
        subject: String
    ) {

        try {

            val snoozeTime =
                System.currentTimeMillis() +
                        (10 * 60 * 1000)

            val intent =
                Intent(
                    context,
                    TaskAlarmReceiver::class.java
                ).apply {

                    putExtra(
                        EXTRA_TASK_ID,
                        taskId
                    )

                    putExtra(
                        EXTRA_TITLE,
                        title
                    )

                    putExtra(
                        EXTRA_SUBJECT,
                        subject
                    )
                }

            /*
             * Use a different request code
             * so the snooze alarm doesn't
             * accidentally conflict with
             * the original alarm.
             */

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    taskId + SNOOZE_REQUEST_OFFSET,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

            scheduleAlarm(
                snoozeTime,
                pendingIntent
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    /*
     * COMMON ALARM FUNCTION
     */

    private fun scheduleAlarm(
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            if (
                alarmManager.canScheduleExactAlarms()
            ) {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )

            } else {

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
    }

    /*
     * CANCEL NORMAL NOTIFICATION
     */

    fun cancelNotification(
        taskId: Int
    ) {

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

        /*
         * Also cancel a possible
         * snoozed alarm.
         */

        val snoozePendingIntent =
            PendingIntent.getBroadcast(
                context,
                taskId + SNOOZE_REQUEST_OFFSET,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.cancel(
            snoozePendingIntent
        )
    }

    /*
     * RESCHEDULE REPEATING TASK
     */

    suspend fun rescheduleRepeatingTask(
        task: TaskEntity,
        repository: TaskRepository
    ) {

        if (task.repeat == "None" && task.repeatType == RepeatType.NONE) return

        try {
            
            val dateFormat =
                SimpleDateFormat(
                    "d/M/yyyy",
                    Locale.getDefault()
                )

            val calendar =
                Calendar.getInstance()

            calendar.time =
                dateFormat.parse(task.date) ?: return

            /*
             * ADD TIME BASED ON REPEAT
             */

            val currentRepeat = if (task.repeatType != RepeatType.NONE) {
                task.repeatType.name.lowercase().replaceFirstChar { it.uppercase() }
            } else {
                task.repeat
            }

            if (currentRepeat == "Daily") {

                calendar.add(
                    Calendar.DAY_OF_YEAR,
                    1
                )

            } else if (currentRepeat == "Weekly") {

                calendar.add(
                    Calendar.WEEK_OF_YEAR,
                    1
                )
            }

            val nextDate =
                dateFormat.format(
                    calendar.time
                )

            /*
             * UPDATE TASK IN DATABASE
             * AND SCHEDULE NEXT
             */

            val nextTask =
                task.copy(
                    date = nextDate,
                    completed = false
                )

            repository.update(nextTask)

            scheduleNotification(nextTask)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {

        const val EXTRA_TASK_ID =
            "task_id"

        const val EXTRA_TITLE =
            "title"

        const val EXTRA_SUBJECT =
            "subject"

        const val SNOOZE_REQUEST_OFFSET =
            50000
    }
}