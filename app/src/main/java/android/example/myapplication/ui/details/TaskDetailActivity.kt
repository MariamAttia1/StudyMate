package android.example.myapplication.ui.details

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.example.myapplication.ui.theme.*
import data.TaskDatabase
import data.TaskRepository
import data.TaskEntity
import android.example.myapplication.ui.calendar.Day

class TaskDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getIntExtra("task_id", -1)
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val viewModel = TaskDetailViewModel(repository)

        setContent {
            MyApplicationTheme {
                TaskDetailScreen(taskId, viewModel, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(taskId: Int, viewModel: TaskDetailViewModel, onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
    val task by viewModel.getTask(taskId).collectAsState(initial = null)
    
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
                title = { Text("Task Details", style = Typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Edit") // Placeholder
                    }
                }
            )
        }
    ) { padding ->
        task?.let { t ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.background(BrandGradient).padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📚", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(t.title, color = Color.White, style = Typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(t.subject, color = Color.White.copy(alpha = 0.8f), style = Typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                PriorityBadge(t.priority)
                            }
                        }
                    }
                }

                // Detail Rows
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        DetailRow(Icons.Default.DateRange, "Date", t.date)
                        DetailRow(Icons.Default.Notifications, "Time", t.time)
                        DetailRow(Icons.Default.MoreVert, "Repeat", t.repeat)
                        DetailRow(Icons.Default.MoreVert, "Status", if (t.completed) "Completed" else "Pending")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.markComplete(t); onBack() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandGradientEnd)
                    ) {
                        Text("Mark Complete", color = BrandGradientEnd, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.deleteTask(t); onBack() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PriorityHighText)
                    ) {
                        Text("Delete Task", color = PriorityHighText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(36.dp).background(AppBackground, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandGradientStart, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), style = Typography.bodyMedium, color = TextSecondary)
        Text(value, style = Typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (bg, text) = when (priority) {
        "High" -> PriorityHighBg to PriorityHighText
        "Medium" -> PriorityMediumBg to PriorityMediumText
        else -> PriorityLowBg to PriorityLowText
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = priority,
            color = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
