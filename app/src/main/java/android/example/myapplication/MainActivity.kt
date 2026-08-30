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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import data.TaskDatabase
import data.TaskEntity
import data.RepeatType
import notification.NotificationScheduler
import android.example.myapplication.ui.theme.*
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
 * CHECK IF PREVIOUS OCCURRENCE WAS IN CURRENT WEEK
 * ============================================================
 */

fun wasTaskInCurrentWeek(
    taskDate: String,
    repeat: String
): Boolean {

    if (repeat == "None") return false

    return try {

        val format =
            SimpleDateFormat(
                "d/M/yyyy",
                Locale.getDefault()
            )

        val taskDateObject =
            format.parse(taskDate)
                ?: return false

        val calendar =
            Calendar.getInstance()

        calendar.time =
            taskDateObject

        /*
         * Subtract period to get previous date
         */

        if (repeat == "Daily") {

            calendar.add(
                Calendar.DAY_OF_YEAR,
                -1
            )

        } else if (repeat == "Weekly") {

            calendar.add(
                Calendar.WEEK_OF_YEAR,
                -1
            )
        }

        val previousDate =
            format.format(
                calendar.time
            )

        isTaskInCurrentWeek(
            previousDate
        )

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
            ) ||
                    wasTaskInCurrentWeek(
                        task.date,
                        task.repeat
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
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandGradientEnd,
                        selectedTextColor = BrandGradientEnd,
                        indicatorColor = BrandGradientStart.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        val intent = android.content.Intent(context, android.example.myapplication.ui.calendar.CalendarActivity::class.java)
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                    label = { Text("Calendar") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { 
                        selectedTab = 2
                        val intent = android.content.Intent(context, android.example.myapplication.ui.stats.StatisticsActivity::class.java)
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = "Stats") }, // Using MoreVert as Stats placeholder
                    label = { Text("Stats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = "More") },
                    label = { Text("More") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                modifier = Modifier
                    .size(56.dp)
                    .background(BrandGradient, CircleShape)
                    .shadow(4.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {

            /*
             * HEADER
             */
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good morning 👋",
                        style = Typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Let's make this day productive!",
                        style = Typography.bodyMedium
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            /*
             * =================================================
             * WEEKLY PROGRESS CARD
             * =================================================
             */

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(BrandGradient)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Weekly Progress",
                            color = Color.White,
                            style = Typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(80.dp),
                                    color = Color.White,
                                    strokeWidth = 8.dp,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                                Text(
                                    text = "${(weeklyProgress * 100).toInt()}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column {
                                Text(
                                    text = "$weeklyCompleted of $weeklyTotal",
                                    color = Color.White,
                                    style = Typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "tasks completed",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = Typography.bodyMedium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Keep going! You're doing great 🔥",
                            color = Color.White,
                            style = Typography.labelMedium
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )

            /*
             * TODAY'S TASKS
             */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Tasks",
                    style = Typography.titleLarge
                )
            }

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
                            Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {

                        Text(

                            text =
                                selectedSubject,

                            maxLines =
                                1,
                            color = TextPrimary
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
                            Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {

                        Text(
                            selectedStatus,
                            color = TextPrimary
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
                        Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clickable {
                val intent = android.content.Intent(context, android.example.myapplication.ui.details.TaskDetailActivity::class.java).apply {
                    putExtra("task_id", task.id)
                }
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {

        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = getSubjectColor(task.subject).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = task.time.split(":")[0], // Placeholder: hour as icon
                    color = getSubjectColor(task.subject),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = task.subject,
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange, 
                        contentDescription = null, 
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (task.completed) "Completed" else countdown,
                        style = Typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                PriorityPill(task.priority)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onToggleCompleted,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (task.completed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Toggle Complete",
                            tint = if (task.completed) BrandGradientEnd else Color.LightGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityPill(priority: String) {
    val (bg, text) = when (priority) {
        "High" -> PriorityHighBg to PriorityHighText
        "Medium" -> PriorityMediumBg to PriorityMediumText
        else -> PriorityLowBg to PriorityLowText
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = priority,
            color = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun getSubjectColor(subject: String): Color {
    return when (subject.lowercase()) {
        "networking" -> Color(0xFF7B61FF)
        "operating systems" -> Color(0xFF4F63E0)
        "programming" -> Color(0xFFE0455A)
        else -> Color(0xFFE0932C)
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

    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("Select date") }
    var selectedTime by remember { mutableStateOf("Select time") }
    var priority by remember { mutableStateOf("Medium") }
    var repeat by remember { mutableStateOf("None") }
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
                    Text("Add New Task", style = Typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Back", modifier = Modifier.size(24.dp)) // Using MoreVert as a placeholder for back arrow if needed, but standard Icons.Default.ArrowBack is better
                    }
                },
                actions = {
                    IconButton(
                        onClick = { if (canSave) onSave(StudyTask(title = title, subject = subject, date = selectedDate, time = selectedTime, priority = priority, repeat = repeat)) },
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

            // TASK TITLE
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
                    placeholder = { Text("Study Kotlin Basics", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true
                )
            }

            // SUBJECT
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
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = BrandGradientStart) },
                    trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    placeholder = { Text("Programming", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true
                )
            }

            // DATE & TIME
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = BrandGradientStart, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(selectedDate, color = if (selectedDate == "Select date") TextSecondary else TextPrimary)
                        }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = BrandGradientStart, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(selectedTime, color = if (selectedTime == "Select time") TextSecondary else TextPrimary)
                        }
                    }
                }
            }

            // PRIORITY
            Column {
                Text("Priority", style = Typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PriorityPillSelector("Low", priority == "Low") { priority = "Low" }
                    PriorityPillSelector("Medium", priority == "Medium") { priority = "Medium" }
                    PriorityPillSelector("High", priority == "High") { priority = "High" }
                }
            }

            // REPEAT
            Column {
                Text("Repeat", style = Typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Box {
                    OutlinedButton(
                        onClick = { showRepeatMenu = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = BrandGradientStart, modifier = Modifier.size(20.dp)) // Using MoreVert as repeat icon placeholder
                                Spacer(Modifier.width(8.dp))
                                Text(repeat, color = TextPrimary)
                            }
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextSecondary)
                        }
                    }
                    DropdownMenu(expanded = showRepeatMenu, onDismissRequest = { showRepeatMenu = false }) {
                        listOf("None", "Daily", "Weekly").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { repeat = it; showRepeatMenu = false })
                        }
                    }
                }
            }

            // NOTES (Optional)
            Column {
                Text("Notes (Optional)", style = Typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = "", // Placeholder since not in DB
                    onValueChange = { },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandGradientStart,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    placeholder = { Text("Focus on variables, data types and control flow.", color = TextSecondary.copy(alpha = 0.5f)) },
                    minLines = 3
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // SAVE BUTTON
            Button(
                onClick = { if (canSave) onSave(StudyTask(title = title, subject = subject, date = selectedDate, time = selectedTime, priority = priority, repeat = repeat)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(if (canSave) BrandGradient else Brush.linearGradient(listOf(Color.LightGray, Color.LightGray)), RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = canSave
            ) {
                Text("Save Task", style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RowScope.PriorityPillSelector(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val (bg, text) = when (label) {
        "High" -> PriorityHighBg to PriorityHighText
        "Medium" -> PriorityMediumBg to PriorityMediumText
        else -> PriorityLowBg to PriorityLowText
    }
    
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clickable(onClick = onClick)
            .then(if (isSelected) Modifier.shadow(4.dp, RoundedCornerShape(12.dp)) else Modifier),
        color = if (isSelected) bg else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, color = if (isSelected) text else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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