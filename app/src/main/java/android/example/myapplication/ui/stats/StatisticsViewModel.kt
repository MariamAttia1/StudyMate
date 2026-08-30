package android.example.myapplication.ui.stats

import androidx.lifecycle.ViewModel
import data.TaskRepository
import data.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StatisticsViewModel(private val repository: TaskRepository) : ViewModel() {
    val allTasks: Flow<List<TaskEntity>> = repository.allTasks

    val stats = allTasks.map { tasks ->
        val total = tasks.size
        val completed = tasks.count { it.completed }
        val pending = total - completed
        val rate = if (total > 0) (completed.toFloat() / total.toFloat()) else 0f
        
        TaskStats(
            totalTasks = total,
            completedTasks = completed,
            pendingTasks = pending,
            completionRate = rate
        )
    }
}

data class TaskStats(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val completionRate: Float
)
