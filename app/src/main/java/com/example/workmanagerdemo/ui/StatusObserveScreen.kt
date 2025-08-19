package com.example.workmanagerdemo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*

@Composable
fun StatusObserveScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "任务状态监听演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<StatusWorker>().build()
            workManager.enqueue(request)
            workManager.getWorkInfoByIdLiveData(request.id).observeForever {
                status = "任务状态: ${it?.state}"
            }
        }) {
            Text("启动任务并监听状态")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
    }
}

class StatusWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("StatusWorker", "状态监听任务执行")
        return Result.success()
    }
}
