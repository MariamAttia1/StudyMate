package android.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class RepeatLogicTest {

    @Test
    fun testNextOccurrenceDaily() {
        val dateFormat = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        val initialDateString = "1/1/2026"
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(initialDateString)!!
        
        // Add 1 day for DAILY
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        
        val nextDate = dateFormat.format(calendar.time)
        assertEquals("2/1/2026", nextDate)
    }

    @Test
    fun testNextOccurrenceWeekly() {
        val dateFormat = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        val initialDateString = "1/1/2026" // A Thursday
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(initialDateString)!!
        
        // Add 7 days for WEEKLY
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        
        val nextDate = dateFormat.format(calendar.time)
        assertEquals("8/1/2026", nextDate)
    }
}