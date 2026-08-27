package android.example.myapplication.ui.dayagenda

import androidx.lifecycle.ViewModel
import android.example.myapplication.data.TaskRepository
import android.example.myapplication.data.TaskEntity
import kotlinx.coroutines.flow.Flow

class DayAgendaViewModel(private val repository: TaskRepository) : ViewModel() {
    fun getTasksByDate(date: String): Flow<List<TaskEntity>> = repository.getTasksByDate(date)
    
    suspend fun updateTask(task: TaskEntity) = repository.update(task)
    
    suspend fun deleteTask(task: TaskEntity) = repository.delete(task)
}