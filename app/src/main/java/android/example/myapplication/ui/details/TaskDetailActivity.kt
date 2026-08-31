package android.example.myapplication.ui.details

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.example.myapplication.ui.theme.*
import android.example.myapplication.StudyTask
import android.example.myapplication.PriorityPillSelector
import data.TaskDatabase
import data.TaskRepository
import data.TaskEntity
import java.util.Calendar
import java.util.Locale

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
    var isEditing by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (isEditing && task != null) {
        EditTaskScreen(
            task = task!!,
            onBack = { isEditing = false },
            onSave = { updatedTask ->
                viewModel.updateTask(updatedTask)
                isEditing = false
            }
        )
    } else {
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
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        showMenu = false
                                        isEditing = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        task?.let { viewModel.deleteTask(it) }
                                        onBack()
                                    },
                                    leadingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Red) }
                                )
                            }
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
                            onClick = { viewModel.toggleComplete(t); onBack() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandGradientEnd)
                        ) {
                            Text(
                                if (t.completed) "Mark Pending" else "Mark Complete",
                                color = BrandGradientEnd,
                                fontWeight = FontWeight.Bold
                            )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    task: TaskEntity,
    onBack: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    BackHandler {
        onBack()
    }

    var title by remember { mutableStateOf(task.title) }
    var subject by remember { mutableStateOf(task.subject) }
    var selectedDate by remember { mutableStateOf(task.date) }
    var selectedTime by remember { mutableStateOf(task.time) }
    var priority by remember { mutableStateOf(task.priority) }
    var repeat by remember { mutableStateOf(task.repeat) }
    var showRepeatMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val canSave = title.isNotBlank() && subject.isNotBlank() && 
                 selectedDate != "Select date" && selectedTime != "Select time"

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
                title = { 
                    Text("Edit Task", style = Typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            if (canSave) {
                                onSave(task.copy(
                                    title = title,
                                    subject = subject,
                                    date = selectedDate,
                                    time = selectedTime,
                                    priority = priority,
                                    repeat = repeat
                                ))
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .background(if (canSave) BrandGradient else Brush.linearGradient(listOf(Color.LightGray, Color.LightGray)), CircleShape)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Save", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column {
                Text("Task Title", style = Typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandGradientStart,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }

            Column {
                Text("Subject", style = Typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandGradientStart,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Date", style = Typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d -> selectedDate = "$d/${m + 1}/$y" },
                                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Text(selectedDate, color = TextPrimary)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Time", style = Typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(context, { _, h, m -> selectedTime = String.format("%02d:%02d", h, m) },
                                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Text(selectedTime, color = TextPrimary)
                    }
                }
            }

            Column {
                Text("Priority", style = Typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PriorityPillSelector("Low", priority == "Low") { priority = "Low" }
                    PriorityPillSelector("Medium", priority == "Medium") { priority = "Medium" }
                    PriorityPillSelector("High", priority == "High") { priority = "High" }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    if (canSave) {
                        onSave(task.copy(
                            title = title,
                            subject = subject,
                            date = selectedDate,
                            time = selectedTime,
                            priority = priority,
                            repeat = repeat
                        ))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(if (canSave) BrandGradient else Brush.linearGradient(listOf(Color.LightGray, Color.LightGray)), RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = canSave
            ) {
                Text("Update Task", style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
