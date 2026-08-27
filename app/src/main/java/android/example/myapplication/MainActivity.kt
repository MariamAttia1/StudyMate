package android.example.myapplication

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import data.TaskDatabase
import data.TaskEntity
import data.RepeatType
import notification.NotificationScheduler
import android.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


/*
 * ============================================================
 * STUDY TASK
 * ============================================================
 */

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


/*
 * ============================================================
 * COUNTDOWN FUNCTION
 * ============================================================
 */

fun calculateCountdown(
    date: String,
    time: String
): String {

    return try {

        val dateFormat =
            SimpleDateFormat(
                "d/M/yyyy HH:mm",
                Locale.getDefault()
            )

        val dueDate =
            dateFormat.parse(
                "$date $time"
            ) ?: return "Invalid date"

        val now =
            System.currentTimeMillis()

        val difference =
            dueDate.time - now

        /*
         * OVERDUE
         */

        if (difference <= 0) {

            val overdue =
                -difference

            val totalMinutes =
                overdue / (1000 * 60)

            val days =
                totalMinutes / (60 * 24)

            val hours =
                (totalMinutes % (60 * 24)) / 60

            val minutes =
                totalMinutes % 60

            when {

                days > 0 ->
                    "⚠️ Overdue by ${days}d ${hours}h"

                hours > 0 ->
                    "⚠️ Overdue by ${hours}h ${minutes}m"

                minutes > 0 ->
                    "⚠️ Overdue by ${minutes}m"

                else ->
                    "⚠️ Overdue"
            }

        } else {

            /*
             * UPCOMING
             */

            val totalMinutes =
                difference / (1000 * 60)

            val days =
                totalMinutes / (60 * 24)

            val hours =
                (totalMinutes % (60 * 24)) / 60

            val minutes =
                totalMinutes % 60

            when {

                days > 0 ->
                    "⏳ Due in ${days}d ${hours}h"

                hours > 0 ->
                    "⏳ Due in ${hours}h ${minutes}m"

                minutes > 0 ->
                    "⏳ Due in ${minutes}m"

                else ->
                    "⏳ Due very soon"
            }
        }

    } catch (e: Exception) {

        "Unable to calculate time"
    }
}


/*
 * ============================================================
 * CHECK IF TASK IS IN CURRENT WEEK
 * ============================================================
 */

fun isTaskInCurrentWeek(
    taskDate: String
): Boolean {

    return try {

        val format =
            SimpleDateFormat(
                "d/M/yyyy",
                Locale.getDefault()
            )

        val taskDateObject =
            format.parse(taskDate)
                ?: return false

        val taskCalendar =
            Calendar.getInstance()

        taskCalendar.time =
            taskDateObject

        val today =
            Calendar.getInstance()

        /*
         * Get first day of current week.
         * Calendar uses Sunday as first day by default,
         * so we calculate the week range manually.
         */

        val startOfWeek =
            Calendar.getInstance()

        startOfWeek.time =
            today.time

        startOfWeek.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        startOfWeek.set(
            Calendar.MINUTE,
            0
        )

        startOfWeek.set(
            Calendar.SECOND,
            0
        )

        startOfWeek.set(
            Calendar.MILLISECOND,
            0
        )

        val dayOfWeek =
            startOfWeek.get(
                Calendar.DAY_OF_WEEK
            )

        val daysFromMonday =
            when (dayOfWeek) {

                Calendar.SUNDAY -> 6

                Calendar.MONDAY -> 0

                Calendar.TUESDAY -> 1

                Calendar.WEDNESDAY -> 2

                Calendar.THURSDAY -> 3

                Calendar.FRIDAY -> 4

                Calendar.SATURDAY -> 5

                else -> 0
            }

        startOfWeek.add(
            Calendar.DAY_OF_MONTH,
            -daysFromMonday
        )

        val endOfWeek =
            Calendar.getInstance()

        endOfWeek.time =
            startOfWeek.time

        endOfWeek.add(
            Calendar.DAY_OF_MONTH,
            7
        )

        taskCalendar.before(endOfWeek) &&
                !taskCalendar.before(startOfWeek)

    } catch (e: Exception) {

        false
    }
}


/*
 * ============================================================
 * MAIN ACTIVITY
 * ============================================================
 */

class MainActivity : ComponentActivity() {

    private lateinit var database: TaskDatabase

    /*
     * NOTIFICATION PERMISSION
     */

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        /*
         * DATABASE
         */

        database =
            TaskDatabase.getDatabase(
                applicationContext
            )

        /*
         * REQUEST NOTIFICATION PERMISSION
         */

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }

        /*
         * COMPOSE
         */

        setContent {

            MyApplicationTheme {

                StudyMateUI(
                    database
                )
            }
        }
    }
}


/*
 * ============================================================
 * STUDYMATE UI
 * ============================================================
 */

@Composable
fun StudyMateUI(
    database: TaskDatabase
) {

    var showWelcomeScreen by remember {

        mutableStateOf(true)
    }

    if (showWelcomeScreen) {

        WelcomeScreen(

            onStart = {

                showWelcomeScreen =
                    false
            }
        )

    } else {

        StudyMateHome(
            database
        )
    }
}


/*
 * ============================================================
 * WELCOME SCREEN
 * ============================================================
 */

@Composable
fun WelcomeScreen(
    onStart: () -> Unit
) {

    Surface(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            /*
             * APP NAME
             */

            Text(

                text =
                    "StudyMate",

                fontSize =
                    38.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            /*
             * SUBTITLE
             */

            Text(

                text =
                    "Your study. Your goals. Your future.",

                fontSize =
                    16.sp
            )

            Spacer(
                modifier =
                    Modifier.height(60.dp)
            )

            /*
             * QUOTE
             */

            Text(

                text =
                    "“Small steps. Big results.”",

                fontSize =
                    24.sp,

                fontWeight =
                    FontWeight.Medium,

                lineHeight =
                    34.sp
            )

            Spacer(
                modifier =
                    Modifier.height(60.dp)
            )

            /*
             * START BUTTON
             */

            Button(

                onClick =
                    onStart,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {

                Text(

                    text =
                        "Start Studying",

                    fontSize =
                        17.sp
                )
            }
        }
    }
}


/*
 * ============================================================
 * STUDYMATE HOME
 * ============================================================
 */

@Composable
fun StudyMateHome(
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

    /*
     * FILTERS
     */

    var selectedSubject by remember {

        mutableStateOf(
            "All Subjects"
        )
    }

    var selectedStatus by remember {

        mutableStateOf(
            "All"
        )
    }

    var showSubjectMenu by remember {

        mutableStateOf(false)
    }

    var showStatusMenu by remember {

        mutableStateOf(false)
    }

    /*
     * LOAD TASKS
     */

    LaunchedEffect(Unit) {

        database.taskDao()
            .getAllTasks()
            .collect { entityList ->

                tasks =
                    entityList.map { entity ->

                        StudyTask(

                            id =
                                entity.id,

                            title =
                                entity.title,

                            subject =
                                entity.subject,

                            date =
                                entity.date,

                            time =
                                entity.time,

                            priority =
                                entity.priority,

                            repeat =
                                entity.repeat,

                            completed =
                                entity.completed
                        )
                    }
            }
    }

    /*
     * SUBJECTS
     */

    val subjects =
        listOf("All Subjects") +

                tasks
                    .map {
                        it.subject
                    }
                    .distinct()
                    .sorted()

    /*
     * FILTER TASKS
     */

    val filteredTasks =
        tasks.filter { task ->

            val subjectMatches =

                selectedSubject ==
                        "All Subjects" ||

                        task.subject ==
                        selectedSubject

            val statusMatches =

                when (
                    selectedStatus
                ) {

                    "Completed" ->
                        task.completed

                    "Pending" ->
                        !task.completed

                    else ->
                        true
                }

            subjectMatches &&
                    statusMatches
        }

    /*
     * ========================================================
     * WEEKLY PROGRESS
     * ========================================================
     */

    val weeklyTasks =
        tasks.filter { task ->

            isTaskInCurrentWeek(
                task.date
            )
        }

    val weeklyTotal =
        weeklyTasks.size

    val weeklyCompleted =
        weeklyTasks.count { task ->

            task.completed
        }

    val weeklyProgress =

        if (weeklyTotal > 0) {

            weeklyCompleted.toFloat() /
                    weeklyTotal.toFloat()

        } else {

            0f
        }

    /*
     * ADD TASK SCREEN
     */

    if (showAddTask) {

        AddTaskScreen(

            onBack = {

                showAddTask =
                    false
            },

            onSave = { task ->

                val entity =
                    TaskEntity(

                        title =
                            task.title,

                        subject =
                            task.subject,

                        date =
                            task.date,

                        time =
                            task.time,

                        priority =
                            task.priority,

                        repeat =
                            task.repeat,

                        repeatType = when(task.repeat) {
                            "Daily" -> RepeatType.DAILY
                            "Weekly" -> RepeatType.WEEKLY
                            else -> RepeatType.NONE
                        },

                        completed =
                            task.completed
                    )

                scope.launch {

                    /*
                     * SAVE TASK
                     */

                    val generatedId =
                        database.taskDao()
                            .insertTask(
                                entity
                            )

                    /*
                     * GET SAVED TASK
                     */

                    val savedTask =
                        database.taskDao()
                            .getTaskById(
                                generatedId.toInt()
                            )

                    /*
                     * SCHEDULE NOTIFICATION
                     */

                    if (
                        savedTask != null
                    ) {

                        NotificationScheduler(
                            context
                        ).scheduleNotification(
                            savedTask
                        )
                    }
                }

                showAddTask =
                    false
            }
        )

    } else {

        HomeScreen(

            tasks =
                filteredTasks,

            subjects =
                subjects,

            selectedSubject =
                selectedSubject,

            selectedStatus =
                selectedStatus,

            showSubjectMenu =
                showSubjectMenu,

            showStatusMenu =
                showStatusMenu,

            weeklyTotal =
                weeklyTotal,

            weeklyCompleted =
                weeklyCompleted,

            weeklyProgress =
                weeklyProgress,

            onSubjectMenuChange = {

                showSubjectMenu =
                    it
            },

            onStatusMenuChange = {

                showStatusMenu =
                    it
            },

            onSubjectSelected = {

                selectedSubject =
                    it

                showSubjectMenu =
                    false
            },

            onStatusSelected = {

                selectedStatus =
                    it

                showStatusMenu =
                    false
            },

            onAddTask = {

                showAddTask =
                    true
            },

            /*
             * DELETE TASK
             */

            onDeleteTask = { task ->

                scope.launch {

                    /*
                     * CANCEL NOTIFICATION
                     */

                    NotificationScheduler(
                        context
                    ).cancelNotification(
                        task.id
                    )

                    /*
                     * DELETE DATABASE TASK
                     */

                    val entity =
                        TaskEntity(

                            id =
                                task.id,

                            title =
                                task.title,

                            subject =
                                task.subject,

                            date =
                                task.date,

                            time =
                                task.time,

                            priority =
                                task.priority,

                            repeat =
                                task.repeat,

                            completed =
                                task.completed
                        )

                    database.taskDao()
                        .deleteTask(
                            entity
                        )
                }
            },

            /*
             * TOGGLE COMPLETED
             */

            onToggleCompleted = { task ->

                scope.launch {

                    val entity =
                        TaskEntity(

                            id =
                                task.id,

                            title =
                                task.title,

                            subject =
                                task.subject,

                            date =
                                task.date,

                            time =
                                task.time,

                            priority =
                                task.priority,

                            repeat =
                                task.repeat,

                            completed =
                                !task.completed
                        )

                    database.taskDao()
                        .updateTask(
                            entity
                        )
                }
            }
        )
    }
}


/*
 * ============================================================
 * HOME SCREEN
 * ============================================================
 */

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeScreen(

    tasks: List<StudyTask>,

    subjects: List<String>,

    selectedSubject: String,

    selectedStatus: String,

    showSubjectMenu: Boolean,

    showStatusMenu: Boolean,

    weeklyTotal: Int,

    weeklyCompleted: Int,

    weeklyProgress: Float,

    onSubjectMenuChange:
        (Boolean) -> Unit,

    onStatusMenuChange:
        (Boolean) -> Unit,

    onSubjectSelected:
        (String) -> Unit,

    onStatusSelected:
        (String) -> Unit,

    onAddTask:
        () -> Unit,

    onDeleteTask:
        (StudyTask) -> Unit,

    onToggleCompleted:
        (StudyTask) -> Unit
) {

    /*
     * ANIMATED PROGRESS
     */

    val animatedProgress by
    animateFloatAsState(
        targetValue =
            weeklyProgress,
        label =
            "Weekly Progress"
    )

    val context = LocalContext.current

    Scaffold(

        floatingActionButton = {

            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = {
                        val intent = android.content.Intent(context, android.example.myapplication.ui.calendar.CalendarActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("📅")
                }

                FloatingActionButton(
                    onClick =
                    onAddTask
                ) {

                    Text(

                        text =
                        "+",

                        fontSize =
                        24.sp
                    )
                }
            }
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {

            /*
             * HEADER
             */

            Text(

                text =
                    "StudyMate",

                fontSize =
                    30.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(

                text =
                    "Good Morning 👋",

                fontSize =
                    18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            /*
             * =================================================
             * WEEKLY PROGRESS CARD
             * =================================================
             */

            Card(

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(

                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Text(

                        text =
                            "Weekly Progress",

                        fontSize =
                            20.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(

                        text =
                            "$weeklyCompleted / $weeklyTotal tasks completed",

                        fontSize =
                            15.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    LinearProgressIndicator(

                        progress = {
                            animatedProgress
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(

                        text =
                            "${(weeklyProgress * 100).toInt()}%",

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            /*
             * TODAY'S TASKS
             */

            Text(

                text =
                    "Today's Tasks",

                fontSize =
                    22.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )

            /*
             * FILTER ROW
             */

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                /*
                 * SUBJECT FILTER
                 */

                Box(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    OutlinedButton(

                        onClick = {

                            onSubjectMenuChange(
                                true
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(

                            text =
                                selectedSubject,

                            maxLines =
                                1
                        )
                    }

                    DropdownMenu(

                        expanded =
                            showSubjectMenu,

                        onDismissRequest = {

                            onSubjectMenuChange(
                                false
                            )
                        }
                    ) {

                        subjects.forEach { subject ->

                            DropdownMenuItem(

                                text = {

                                    Text(
                                        subject
                                    )
                                },

                                onClick = {

                                    onSubjectSelected(
                                        subject
                                    )
                                }
                            )
                        }
                    }
                }

                /*
                 * STATUS FILTER
                 */

                Box(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    OutlinedButton(

                        onClick = {

                            onStatusMenuChange(
                                true
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            selectedStatus
                        )
                    }

                    DropdownMenu(

                        expanded =
                            showStatusMenu,

                        onDismissRequest = {

                            onStatusMenuChange(
                                false
                            )
                        }
                    ) {

                        DropdownMenuItem(

                            text = {

                                Text(
                                    "All"
                                )
                            },

                            onClick = {

                                onStatusSelected(
                                    "All"
                                )
                            }
                        )

                        DropdownMenuItem(

                            text = {

                                Text(
                                    "Pending"
                                )
                            },

                            onClick = {

                                onStatusSelected(
                                    "Pending"
                                )
                            }
                        )

                        DropdownMenuItem(

                            text = {

                                Text(
                                    "Completed"
                                )
                            },

                            onClick = {

                                onStatusSelected(
                                    "Completed"
                                )
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            /*
             * TASK LIST
             */

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

                            text =
                                "No tasks found",

                            fontSize =
                                20.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(

                            text =
                                "Try changing the filters or add a new task"
                        )
                    }
                }

            } else {

                LazyColumn(

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    items(

                        items =
                            tasks,

                        key = {
                            it.id
                        }

                    ) { task ->

                        TaskCard(

                            task =
                                task,

                            onDelete = {

                                onDeleteTask(
                                    task
                                )
                            },

                            onToggleCompleted = {

                                onToggleCompleted(
                                    task
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


/*
 * ============================================================
 * TASK CARD WITH COUNTDOWN
 * ============================================================
 */

@Composable
fun TaskCard(

    task: StudyTask,

    onDelete: () -> Unit,

    onToggleCompleted: () -> Unit
) {

    var countdown by remember {

        mutableStateOf(

            calculateCountdown(

                task.date,

                task.time
            )
        )
    }

    /*
     * UPDATE COUNTDOWN EVERY 30 SECONDS
     */

    LaunchedEffect(

        task.date,

        task.time,

        task.completed
    ) {

        while (true) {

            countdown =

                calculateCountdown(

                    task.date,

                    task.time
                )

            delay(30_000)
        }
    }

    /*
     * CARD
     */

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {

        Column(

            modifier =
                Modifier.padding(18.dp)
        ) {

            /*
             * TITLE
             */

            Text(

                text =
                    task.title,

                fontSize =
                    18.sp,

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

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            /*
             * COUNTDOWN
             */

            if (!task.completed) {

                Text(

                    text =
                        countdown,

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            /*
             * STATUS
             */

            Text(

                text =

                    if (task.completed) {

                        "Status: Completed ✅"

                    } else {

                        "Status: Pending ⏳"
                    },

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            /*
             * BUTTONS
             */

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Button(

                    onClick =
                        onToggleCompleted,

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        if (task.completed) {

                            "Mark Pending"

                        } else {

                            "Complete"
                        }
                    )
                }

                OutlinedButton(

                    onClick =
                        onDelete,

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        "Delete"
                    )
                }
            }
        }
    }
}


/*
 * ============================================================
 * ADD TASK SCREEN
 * ============================================================
 */

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun AddTaskScreen(

    onBack: () -> Unit,

    onSave:
        (StudyTask) -> Unit
) {

    var title by remember {

        mutableStateOf("")
    }

    var subject by remember {

        mutableStateOf("")
    }

    var selectedDate by remember {

        mutableStateOf(
            "Select date"
        )
    }

    var selectedTime by remember {

        mutableStateOf(
            "Select time"
        )
    }

    var priority by remember {

        mutableStateOf(
            "Medium"
        )
    }

    var repeat by remember {

        mutableStateOf(
            "None"
        )
    }

    var showRepeatMenu by remember {

        mutableStateOf(
            false
        )
    }

    val context =
        LocalContext.current

    val calendar =
        Calendar.getInstance()

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        "Add Task"
                    )
                },

                navigationIcon = {

                    TextButton(

                        onClick =
                            onBack
                    ) {

                        Text(
                            "Back"
                        )
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

            /*
             * TITLE
             */

            Text(

                text =
                    "Task Title",

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            OutlinedTextField(

                value =
                    title,

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

                singleLine =
                    true
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * SUBJECT
             */

            Text(

                text =
                    "Subject",

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            OutlinedTextField(

                value =
                    subject,

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

                singleLine =
                    true
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * DATE
             */

            Text(

                text =
                    "Due Date",

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

                Text(
                    selectedDate
                )
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * TIME
             */

            Text(

                text =
                    "Due Time",

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

                Text(
                    selectedTime
                )
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * PRIORITY
             */

            Text(

                text =
                    "Priority",

                fontWeight =
                    FontWeight.Bold
            )

            Row(

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                PriorityOption(

                    text =
                        "Low",

                    selected =
                        priority == "Low",

                    onClick = {

                        priority =
                            "Low"
                    }
                )

                PriorityOption(

                    text =
                        "Medium",

                    selected =
                        priority == "Medium",

                    onClick = {

                        priority =
                            "Medium"
                    }
                )

                PriorityOption(

                    text =
                        "High",

                    selected =
                        priority == "High",

                    onClick = {

                        priority =
                            "High"
                    }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * REPEAT
             */

            Text(

                text =
                    "Repeat",

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

                        showRepeatMenu =
                            true
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        repeat
                    )
                }

                DropdownMenu(

                    expanded =
                        showRepeatMenu,

                    onDismissRequest = {

                        showRepeatMenu =
                            false
                    }
                ) {

                    /*
                     * NONE
                     */

                    DropdownMenuItem(

                        text = {

                            Text(
                                "None"
                            )
                        },

                        onClick = {

                            repeat =
                                "None"

                            showRepeatMenu =
                                false
                        }
                    )

                    /*
                     * DAILY
                     */

                    DropdownMenuItem(

                        text = {

                            Text(
                                "Daily"
                            )
                        },

                        onClick = {

                            repeat =
                                "Daily"

                            showRepeatMenu =
                                false
                        }
                    )

                    /*
                     * WEEKLY
                     */

                    DropdownMenuItem(

                        text = {

                            Text(
                                "Weekly"
                            )
                        },

                        onClick = {

                            repeat =
                                "Weekly"

                            showRepeatMenu =
                                false
                        }
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            /*
             * SAVE
             */

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

                                title =
                                    title,

                                subject =
                                    subject,

                                date =
                                    selectedDate,

                                time =
                                    selectedTime,

                                priority =
                                    priority,

                                repeat =
                                    repeat
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

                Text(
                    "Save Task"
                )
            }
        }
    }
}


/*
 * ============================================================
 * PRIORITY OPTION
 * ============================================================
 */

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

            selected =
                selected,

            onClick =
                onClick
        )

        Text(
            text
        )

        Spacer(
            modifier =
                Modifier.width(5.dp)
        )
    }
}