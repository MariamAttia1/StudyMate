package android.example.myapplication.ui.calendar

import androidx.lifecycle.ViewModel
import android.example.myapplication.data.TaskRepository
import android.example.myapplication.data.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class CalendarViewModel(private val repository: TaskRepository) : ViewModel() {
    val allTasks: Flow<List<TaskEntity>> = repository.allTasks
}