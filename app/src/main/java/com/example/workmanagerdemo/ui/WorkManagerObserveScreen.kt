package com.example.workmanagerdemo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*

/**
 * WorkManager 监听演示页面
 *
 * 主要功能与使用说明：
 * 1. 按任务 ID 监听：
 *    - getWorkInfoByIdLiveData(workId)
 *    - 用于监听单个任务的状态变化（如 ENQUEUED、RUNNING、SUCCEEDED、FAILED、CANCELLED）。
 *
 * 2. 按标签监听：
 *    - getWorkInfosByTagLiveData(tag)
 *    - 用于监听所有带指定标签的任务状态变化，适合批量任务管理。
 *    - 注意：如果标签关联任务较多，监听结果会包含所有相关任务的状态。
 *
 * 3. 按唯一任务名称监听：
 *    - getWorkInfosForUniqueWorkLiveData(workName)
 *    - 用于监听同一 workName 下所有任务（或任务链）的状态变化。
 *    - 注意：同名 workName 的任务链会返回链中所有节点的状态，任务多时监听结果也会较多。
 *
 * 4. 通过 WorkInfo 获取任务详细状态和输出数据：
 *    - 监听结果中可获取任务当前状态、输出数据、失败原因等。
 *
 * 5. 监听方式均为 LiveData，适合与 UI 结合实时展示任务进度和结果。
 *
 * 示例代码见下方按钮演示。
 */

@Composable
fun WorkManagerObserveScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var statusById by remember { mutableStateOf("未开始") }
    var statusByTag by remember { mutableStateOf("未开始") }
    var statusByName by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "WorkManager 监听演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // 按任务ID监听
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<ObserveWorker>().build()
            workManager.enqueue(request)
            workManager.getWorkInfoByIdLiveData(request.id).observeForever {
                statusById = "按ID监听状态: ${it?.state}"
            }
        }) {
            Text("按任务ID监听")
        }
        Text(text = statusById, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // 按标签监听
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<ObserveWorker>().addTag("observeTag").build()
            workManager.enqueue(request)
            workManager.getWorkInfosByTagLiveData("observeTag").observeForever {
                statusByTag = "按标签监听状态: " + it.joinToString { info -> info.state.name }
            }
        }) {
            Text("按标签监听")
        }
        Text(text = statusByTag, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // 按唯一任务名称监听
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<ObserveWorker>().build()
            workManager.enqueueUniqueWork("observeUnique", ExistingWorkPolicy.REPLACE, request)
            workManager.getWorkInfosForUniqueWorkLiveData("observeUnique").observeForever {
                statusByName = "按名称监听状态: " + it.joinToString { info -> info.state.name }
            }
        }) {
            Text("按唯一任务名称监听")
        }
        Text(text = statusByName, style = MaterialTheme.typography.bodyLarge)
    }
}

class ObserveWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("ObserveWorker", "监听演示任务执行")
        return Result.success()
    }
}
