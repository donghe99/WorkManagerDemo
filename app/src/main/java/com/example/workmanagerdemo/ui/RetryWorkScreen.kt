
/**
 * 任务重试演示页面
 *
 * 使用场景：
 * 1. 后台任务可能因网络异常、临时资源不可用等原因失败时，需要自动重试。
 * 2. 适合如网络请求、文件上传、数据同步等对可靠性有要求的任务。
 *
 * 方法说明：
 * - 在 Worker 的 doWork() 方法中返回 Result.retry()，WorkManager 会自动调度重试。
 * - 重试次数和间隔由 WorkManager 自动管理，开发者可自定义重试逻辑。
 * - 如果最终仍失败，可返回 Result.failure()，任务终止。
 */
package com.example.workmanagerdemo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*

@Composable
fun RetryWorkScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "任务重试演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<RetryWorker>().build()
            workManager.enqueue(request)
            workManager.getWorkInfoByIdLiveData(request.id).observeForever {
                status = "重试任务状态: ${it?.state}"
            }
        }) {
            Text("启动重试任务")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
    }
}

class RetryWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("RetryWorker", "重试任务执行，模拟失败")
        return Result.retry()
    }
}
