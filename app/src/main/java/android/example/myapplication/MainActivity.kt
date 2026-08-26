package android.example.myapplication

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.example.myapplication.data.TaskDatabase
import android.example.myapplication.data.TaskEntity
import android.example.myapplication.notification.NotificationScheduler
import android.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.util.Calendar


// ============================================================
// STUDY TASK MODEL
// ============================================================

data class StudyTask(
    val id: Int = 0,
    val title: String,
    val subject: String,
    val date: String,
    val time: String,
    val priority: String,
    val repeat: String,
    val completed: Boolean = false
)


// ============================================================
// MAIN ACTIVITY
// ============================================================

class MainActivity : ComponentActivity() {

    private lateinit var database: TaskDatabase


    // --------------------------------------------------------
    // Notification permission launcher
    // --------------------------------------------------------

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            if (isGranted) {
                // Notification permission granted.
            }
        }


    // --------------------------------------------------------
    // ON CREATE
    // --------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        // Connect to the real Room database
        database =
            TaskDatabase.getDatabase(
                applicationContext
            )


        // Request exact alarm permission
        checkExactAlarmPermission()


        // Request notification permission
        requestNotificationPermission()


        // Start Compose UI
        setContent {

            MyApplicationTheme {

                StudyMateUI(database)

            }
        }
    }


    // --------------------------------------------------------
    // EXACT ALARM PERMISSION
    // --------------------------------------------------------

    private fun checkExactAlarmPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val alarmManager =
                getSystemService(
                    ALARM_SERVICE
                ) as AlarmManager


            if (!alarmManager.canScheduleExactAlarms()) {

                try {

                    val intent = Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse(
                            "package:$packageName"
                        )
                    )

                    startActivity(intent)

                } catch (e: Exception) {

                    e.printStackTrace()

                }
            }
        }
    }


    // --------------------------------------------------------
    // NOTIFICATION PERMISSION
    // --------------------------------------------------------

    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED


            if (!permissionGranted) {

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }
}


// ============================================================
// STUDYMATE UI
// ============================================================

@Composable
fun StudyMateUI(
    database: TaskDatabase
) {

    var showAddTask by remember {

        mutableStateOf(false)

    }


    var tasks by remember {

        mutableStateOf<List<StudyTask>>(
            emptyList()
        )

    }


    val scope =
        rememberCoroutineScope()


    val context =
        LocalContext.current


    // --------------------------------------------------------
    // READ TASKS FROM ROOM DATABASE
    // --------------------------------------------------------

    LaunchedEffect(Unit) {

        database
            .taskDao()
            .getAllTasks()
            .collect { entityList ->


                tasks =
                    entityList.map { entity ->

                        StudyTask(

                            id = entity.id,

                            title = entity.title,

                            subject = entity.subject,

                            date = entity.date,

                            time = entity.time,

                            priority = entity.priority,

                            repeat = entity.repeat,

                            completed = entity.completed
                        )
                    }
            }
    }


    // --------------------------------------------------------
    // ADD TASK SCREEN
    // --------------------------------------------------------

    if (showAddTask) {

        AddTaskScreen(

            onBack = {

                showAddTask = false

            },


            onSave = { task ->


                // Create Room entity
                val entity = TaskEntity(

                    title = task.title,

                    subject = task.subject,

                    date = task.date,

                    time = task.time,

                    priority = task.priority,

                    repeat = task.repeat,

                    completed = task.completed
                )


                scope.launch {


                    // ------------------------------------------------
                    // 1. SAVE TASK TO REAL DATABASE
                    // ------------------------------------------------

                    val generatedId =
                        database
                            .taskDao()
                            .insertTask(entity)


                    // ------------------------------------------------
                    // 2. GET TASK WITH REAL DATABASE ID
                    // ------------------------------------------------

                    val savedTask =
                        database
                            .taskDao()
                            .getTaskById(
                                generatedId.toInt()
                            )


                    // ------------------------------------------------
                    // 3. SCHEDULE NOTIFICATION
                    // ------------------------------------------------

                    if (savedTask != null) {

                        NotificationScheduler(
                            context
                        ).scheduleNotification(
                            savedTask
                        )
                    }
                }


                // Return to home screen
                showAddTask = false
            }
        )


    } else {


        // ----------------------------------------------------
        // HOME SCREEN
        // ----------------------------------------------------

        HomeScreen(

            tasks = tasks,

            onAddTask = {

                showAddTask = true

            }
        )
    }
}


// ============================================================
// HOME SCREEN
// ============================================================

@Composable
fun HomeScreen(
    tasks: List<StudyTask>,
    onAddTask: () -> Unit
) {

    Scaffold(

        floatingActionButton = {

            FloatingActionButton(

                onClick = onAddTask

            ) {

                Text(

                    text = "+",

                    fontSize = 24.sp

                )
            }
        }

    ) { padding ->


        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)

        ) {


            Text(

                text = "StudyMate",

                fontSize = 30.sp,

                fontWeight = FontWeight.Bold

            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(

                text = "Good Morning 👋",

                fontSize = 18.sp

            )


            Spacer(
                modifier = Modifier.height(30.dp)
            )


            Text(

                text = "Today's Tasks",

                fontSize = 22.sp,

                fontWeight = FontWeight.Bold

            )


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            if (tasks.isEmpty()) {


                Box(

                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    contentAlignment =
                        Alignment.Center

                ) {


                    Column(

                        horizontalAlignment =
                            Alignment.CenterHorizontally

                    ) {


                        Text(

                            text = "No tasks yet",

                            fontSize = 20.sp,

                            fontWeight =
                                FontWeight.Bold

                        )


                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )


                        Text(

                            text =
                                "Press + to add your first task"

                        )
                    }
                }


            } else {


                LazyColumn(

                    modifier =
                        Modifier.fillMaxWidth()

                ) {


                    items(tasks) { task ->

                        TaskCard(task)

                    }
                }
            }
        }
    }
}


// ============================================================
// TASK CARD
// ============================================================

@Composable
fun TaskCard(
    task: StudyTask
) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)

    ) {


        Column(

            modifier =
                Modifier.padding(18.dp)

        ) {


            Text(

                text = task.title,

                fontSize = 18.sp,

                fontWeight =
                    FontWeight.Bold

            )


            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )


            Text(
                text =
                    "Subject: ${task.subject}"
            )


            Text(
                text =
                    "Date: ${task.date}"
            )


            Text(
                text =
                    "Time: ${task.time}"
            )


            Text(
                text =
                    "Priority: ${task.priority}"
            )


            Text(
                text =
                    "Repeat: ${task.repeat}"
            )
        }
    }
}


// ============================================================
// ADD TASK SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(

    onBack: () -> Unit,

    onSave: (StudyTask) -> Unit

) {


    var title by remember {

        mutableStateOf("")

    }


    var subject by remember {

        mutableStateOf("")

    }


    var selectedDate by remember {

        mutableStateOf("Select date")

    }


    var selectedTime by remember {

        mutableStateOf("Select time")

    }


    var priority by remember {

        mutableStateOf("Medium")

    }


    var repeat by remember {

        mutableStateOf("None")

    }


    var showRepeatMenu by remember {

        mutableStateOf(false)

    }


    val context =
        LocalContext.current


    val calendar =
        Calendar.getInstance()


    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text("Add Task")

                },


                navigationIcon = {

                    TextButton(

                        onClick = onBack

                    ) {

                        Text("Back")

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

        ) {


            // ------------------------------------------------
            // TITLE
            // ------------------------------------------------

            Text(

                text = "Task Title",

                fontWeight =
                    FontWeight.Bold

            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            OutlinedTextField(

                value = title,

                onValueChange = {

                    title = it

                },

                modifier =
                    Modifier.fillMaxWidth(),

                placeholder = {

                    Text(
                        "Example: Study CCNA"
                    )

                },

                singleLine = true

            )


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            // ------------------------------------------------
            // SUBJECT
            // ------------------------------------------------

            Text(

                text = "Subject",

                fontWeight =
                    FontWeight.Bold

            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            OutlinedTextField(

                value = subject,

                onValueChange = {

                    subject = it

                },

                modifier =
                    Modifier.fillMaxWidth(),

                placeholder = {

                    Text(
                        "Example: Networking"
                    )

                },

                singleLine = true

            )


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            // ------------------------------------------------
            // DATE
            // ------------------------------------------------

            Text(

                text = "Due Date",

                fontWeight =
                    FontWeight.Bold

            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            OutlinedButton(

                onClick = {

                    DatePickerDialog(

                        context,

                        { _, year, month, day ->

                            selectedDate =
                                "$day/${month + 1}/$year"

                        },

                        calendar.get(
                            Calendar.YEAR
                        ),

                        calendar.get(
                            Calendar.MONTH
                        ),

                        calendar.get(
                            Calendar.DAY_OF_MONTH
                        )

                    ).show()

                },

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(selectedDate)

            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            // ------------------------------------------------
            // TIME
            // ------------------------------------------------

            Text(

                text = "Due Time",

                fontWeight =
                    FontWeight.Bold

            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            OutlinedButton(

                onClick = {

                    TimePickerDialog(

                        context,

                        { _, hour, minute ->

                            selectedTime =
                                String.format(
                                    "%02d:%02d",
                                    hour,
                                    minute
                                )

                        },

                        calendar.get(
                            Calendar.HOUR_OF_DAY
                        ),

                        calendar.get(
                            Calendar.MINUTE
                        ),

                        true

                    ).show()

                },

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(selectedTime)

            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            // ------------------------------------------------
            // PRIORITY
            // ------------------------------------------------

            Text(

                text = "Priority",

                fontWeight =
                    FontWeight.Bold

            )


            Row(

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                PriorityOption(

                    text = "Low",

                    selected =
                        priority == "Low",

                    onClick = {

                        priority = "Low"

                    }
                )


                PriorityOption(

                    text = "Medium",

                    selected =
                        priority == "Medium",

                    onClick = {

                        priority = "Medium"

                    }
                )


                PriorityOption(

                    text = "High",

                    selected =
                        priority == "High",

                    onClick = {

                        priority = "High"

                    }
                )
            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            // ------------------------------------------------
            // REPEAT
            // ------------------------------------------------

            Text(

                text = "Repeat",

                fontWeight =
                    FontWeight.Bold

            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Box {


                OutlinedButton(

                    onClick = {

                        showRepeatMenu = true

                    },

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    Text(repeat)

                }


                DropdownMenu(

                    expanded =
                        showRepeatMenu,

                    onDismissRequest = {

                        showRepeatMenu = false

                    }

                ) {


                    DropdownMenuItem(

                        text = {

                            Text("None")

                        },

                        onClick = {

                            repeat = "None"

                            showRepeatMenu = false

                        }
                    )


                    DropdownMenuItem(

                        text = {

                            Text("Daily")

                        },

                        onClick = {

                            repeat = "Daily"

                            showRepeatMenu = false

                        }
                    )


                    DropdownMenuItem(

                        text = {

                            Text("Weekly")

                        },

                        onClick = {

                            repeat = "Weekly"

                            showRepeatMenu = false

                        }
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )


            // ------------------------------------------------
            // SAVE BUTTON
            // ------------------------------------------------

            Button(

                onClick = {

                    if (

                        title.isNotBlank() &&

                        subject.isNotBlank() &&

                        selectedDate !=
                        "Select date" &&

                        selectedTime !=
                        "Select time"

                    ) {


                        onSave(

                            StudyTask(

                                title = title,

                                subject = subject,

                                date = selectedDate,

                                time = selectedTime,

                                priority = priority,

                                repeat = repeat

                            )
                        )
                    }
                },


                modifier =
                    Modifier.fillMaxWidth(),


                enabled =

                    title.isNotBlank() &&

                            subject.isNotBlank() &&

                            selectedDate !=
                            "Select date" &&

                            selectedTime !=
                            "Select time"

            ) {

                Text("Save Task")

            }
        }
    }
}


// ============================================================
// PRIORITY OPTION
// ============================================================

@Composable
fun PriorityOption(

    text: String,

    selected: Boolean,

    onClick: () -> Unit

) {


    Row(

        verticalAlignment =
            Alignment.CenterVertically

    ) {


        RadioButton(

            selected = selected,

            onClick = onClick

        )


        Text(text)


        Spacer(

            modifier =
                Modifier.width(5.dp)

        )
    }
}