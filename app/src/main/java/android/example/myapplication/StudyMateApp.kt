package android.example.myapplication

import android.app.Application
import android.example.myapplication.notification.NotificationHelper

class StudyMateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}