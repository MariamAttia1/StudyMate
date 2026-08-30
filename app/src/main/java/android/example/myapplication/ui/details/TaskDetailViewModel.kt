package android.example.myapplication.ui.details

import androidx.lifecycle.ViewModel
import data.TaskRepository
import data.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TaskDetailViewModel(private val repository: TaskRepository) : ViewModel() {
    
    fun getTask(id: Int): Flow<TaskEntity?> = flow {
        emit(repository.getTaskById(id))
    }

    fun markComplete(task: TaskEntity) {
        viewModelScope.launch {
            repository.update(task.copy(completed = true))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }
}
