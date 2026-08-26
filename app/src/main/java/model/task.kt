package android.example.myapplication.model

data class Task(
    val id: Long = 0,
    val title: String,
    val subject: String,
    val dueDateTime: Long // epoch millis - the exact time the task is due
)