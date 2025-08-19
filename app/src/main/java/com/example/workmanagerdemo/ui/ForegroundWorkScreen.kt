
/**
 * 前台服务任务演示页面
 *
 * 使用场景：
 * 1. 需要长时间运行且不能被系统杀死的任务，如大文件上传/下载、音视频处理、数据同步等。
 * 2. 任务执行期间需要向用户展示通知，保证任务的可见性和重要性。
 * 3. 满足 Android 系统要求：后台任务如果时间较长，必须提升为前台服务，否则可能被系统中断。
 *
 * 方法说明：
 * - 在 Worker 中调用 setForegroundAsync(ForegroundInfo)，即可将任务提升为前台服务。
 * - ForegroundInfo 需包含通知，任务执行期间会在通知栏显示进度或状态。
 * - 适合对可靠性和用户体验要求较高的场景。
 */
package com.example.workmanagerdemo.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*

@Composable
fun ForegroundWorkScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "前台服务任务演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<ForegroundWorker>().build()
            workManager.enqueue(request)
            workManager.getWorkInfoByIdLiveData(request.id).observeForever {
                status = "前台任务状态: ${it?.state}"
            }
        }) {
            Text("启动前台服务任务")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
    }
}

class ForegroundWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        setForegroundAsync(createForegroundInfo())
        android.util.Log.d("ForegroundWorker", "前台服务任务执行")
        Thread.sleep(2000)
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = "workmanager_demo_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "WorkManager Demo", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(applicationContext, channelId)
            .setContentTitle("WorkManager Demo")
            .setContentText("前台服务任务正在执行...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        return ForegroundInfo(1, notification)
    }
}
