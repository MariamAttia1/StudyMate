package android.example.myapplication.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import android.example.myapplication.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val taskId =
            intent.getIntExtra(
                EXTRA_TASK_ID,
                -1
            )

        if (taskId == -1) {
            return
        }

        /*
         * REMOVE CURRENT NOTIFICATION
         */

        NotificationManagerCompat
            .from(context)
            .cancel(taskId)

        /*
         * DONE
         */

        if (
            intent.action ==
            ACTION_DONE
        ) {

            val database =
                TaskDatabase.getDatabase(
                    context.applicationContext
                )

            CoroutineScope(
                Dispatchers.IO
            ).launch {

                val task =
                    database.taskDao()
                        .getTaskById(taskId)

                if (task != null) {

                    val completedTask =
                        task.copy(
                            completed = true
                        )

                    database.taskDao()
                        .updateTask(
                            completedTask
                        )
                }
            }
        }

        /*
         * SNOOZE
         */

        else if (
            intent.action ==
            ACTION_SNOOZE
        ) {

            val title =
                intent.getStringExtra(
                    EXTRA_TITLE
                ) ?: "Study Task"

            val subject =
                intent.getStringExtra(
                    EXTRA_SUBJECT
                ) ?: ""

            NotificationScheduler(
                context.applicationContext
            ).scheduleSnooze(
                taskId = taskId,
                title = title,
                subject = subject
            )
        }
    }

    companion object {

        const val ACTION_DONE =
            "android.example.myapplication.ACTION_DONE"

        const val ACTION_SNOOZE =
            "android.example.myapplication.ACTION_SNOOZE"

        const val EXTRA_TASK_ID =
            "extra_task_id"

        const val EXTRA_TITLE =
            "extra_title"

        const val EXTRA_SUBJECT =
            "extra_subject"
    }
}