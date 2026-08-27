package android.example.myapplication.ui.dayagenda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.TaskDatabase
import data.TaskRepository
import android.example.myapplication.ui.theme.MyApplicationTheme
import android.example.myapplication.StudyTask
import android.example.myapplication.TaskCard
import kotlinx.coroutines.launch

class DayAgendaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selectedDate = intent.getStringExtra("selected_date") ?: ""
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val viewModel = DayAgendaViewModel(repository)

        setContent {
            MyApplicationTheme {
                DayAgendaScreen(selectedDate, viewModel) {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAgendaScreen(date: String, viewModel: DayAgendaViewModel, onBack: () -> Unit) {
    val tasks by viewModel.getTasksByDate(date).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks for $date") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(tasks) { entity ->
                val studyTask = StudyTask(
                    id = entity.id,
                    title = entity.title,
                    subject = entity.subject,
                    date = entity.date,
                    time = entity.time,
                    priority = entity.priority,
                    repeat = entity.repeat,
                    completed = entity.completed
                )
                TaskCard(
                    task = studyTask,
                    onDelete = {
                        scope.launch { viewModel.deleteTask(entity) }
                    },
                    onToggleCompleted = {
                        scope.launch { viewModel.updateTask(entity.copy(completed = !entity.completed)) }
                    }
                )
            }
        }
    }
}