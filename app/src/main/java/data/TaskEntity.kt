package android.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val subject: String,

    val date: String,

    val time: String,

    val priority: String,

    val repeat: String,

    val completed: Boolean = false
)