package data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTasksByDate(date: String): Flow<List<TaskEntity>> = taskDao.getTasksByDate(date)

    suspend fun insert(task: TaskEntity) = taskDao.insertTask(task)
    
    suspend fun update(task: TaskEntity) = taskDao.updateTask(task)
    
    suspend fun delete(task: TaskEntity) = taskDao.deleteTask(task)
    
    suspend fun getTaskById(id: Int) = taskDao.getTaskById(id)
}