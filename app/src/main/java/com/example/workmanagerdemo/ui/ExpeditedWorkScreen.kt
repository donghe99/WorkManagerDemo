
/**
 * 加急任务调度演示页面
 *
 * 使用场景：
 * 1. 需要尽快执行的高优先级任务，如即时消息同步、紧急数据处理等。
 * 2. 适合对时效性要求高的后台任务。
 *
 * 方法说明：
 * - 使用 OneTimeWorkRequestBuilder.setExpedited() 设置任务为加急。
 * - 加急任务会优先调度，但受系统资源和配额限制，超出配额时可指定降级策略：
 *   - RUN_AS_NON_EXPEDITED_WORK_REQUEST：超出配额时降级为普通任务继续执行。
 *   - DROP_WORK_REQUEST：超出配额时直接丢弃该任务，不会执行（适合非关键任务）。
 * - 仅支持 Android 12 及以上系统。
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
fun ExpeditedWorkScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "加急任务调度演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<ExpeditedWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            workManager.enqueue(request)
            workManager.getWorkInfoByIdLiveData(request.id).observeForever {
                status = "加急任务状态: ${it?.state}"
            }
        }) {
            Text("启动加急任务")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
    }
}

class ExpeditedWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("ExpeditedWorker", "加急任务执行")
        return Result.success()
    }
}
