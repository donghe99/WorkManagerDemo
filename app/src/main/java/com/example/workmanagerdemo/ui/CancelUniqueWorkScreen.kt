
/**
 * 任务取消与唯一任务演示页面
 *
 * 主要功能：
 * 1. 唯一任务（enqueueUniqueWork）：
 *    - 通过指定唯一名称（workName），确保同类型任务在队列中只保留一个，常用于日志上传、数据同步等场景。
 *    - 策略说明：
 *      - ExistingWorkPolicy.REPLACE：新任务替换旧任务。
 *      - ExistingWorkPolicy.KEEP：如果已有同名任务在队列中，则忽略新任务。
 *      - ExistingWorkPolicy.APPEND：新任务追加到已有任务链后（仅限任务链）。
 *      - ExistingWorkPolicy.APPEND_OR_REPLACE：如果已有任务是任务链则追加，否则替换（Android 12+）。
 *    - 同名唯一任务不会并存，WorkManager 会自动管理，避免并发冲突。
 *    - 可通过 WorkManager.cancelUniqueWork(workName) 一次性取消该名称下的所有任务（包括任务链）。
 *    - 取消任务链时，链中所有未完成任务都会被终止，监听 getWorkInfoByIdLiveData 可收到 CANCELLED 状态，但不会返回任何输出数据。
 *
 * 2. 任务取消（cancelAllWorkByTag）：
 *    - 可通过标签批量取消任务，也可结合唯一任务名称进行灵活管理。
 *
 * 用法说明：
 * - 启动唯一任务时，使用 enqueueUniqueWork 并指定名称和策略。
 * - 取消任务时，可按标签或唯一名称进行操作。
 * - 标签和唯一任务可结合使用，实现批量和精确管理。
 *   详细用法：
 *   - 为唯一任务设置标签（addTag），可通过标签批量查询或取消同类任务。
 *   - 例如：多个唯一任务（不同 workName）可使用同一个标签，便于统一取消或统计。
 *   - 既可通过 cancelUniqueWork(workName) 精确取消某个唯一任务，也可通过 cancelAllWorkByTag(tag) 批量取消所有带该标签的任务。
 *
 * ---------------------------------------------
 * 【WorkManager 取消任务时的详细执行流程说明】
 *
 * 1. 取消操作本质：
 *    - cancelUniqueWork/cancelAllWorkByTag/cancelWorkById 等方法会“立即请求”取消任务。
 *    - 取消是异步的，WorkManager 会尽快将目标任务的状态置为 CANCELLED。
 *
 * 2. 对不同状态任务的影响：
 *    - ENQUEUED/BLOCKED（未开始）：会很快变为 CANCELLED，任务不会被调度执行。
 *    - RUNNING（正在执行）：
 *        - WorkManager 会通知 Worker 停止。
 *        - Worker 需在 doWork() 中主动检查 isStopped 标志，及时 return，才能真正中断。
 *        - 如果 Worker 不响应 isStopped，任务会继续运行直到 doWork 返回。
 *    - SUCCEEDED/FAILED（已完成）：不会受影响，状态不会变为 CANCELLED。
 *
 * 3. 取消任务链时：
 *    - 链中所有未完成的节点（包括 BLOCKED、ENQUEUED、RUNNING）都会被置为 CANCELLED。
 *    - 已完成的节点（SUCCEEDED/FAILED）不会受影响。
 *    - 依赖于被取消节点的后续任务也会被自动取消（即使还未调度）。
 *    - 监听 getWorkInfoByIdLiveData 可收到 CANCELLED 状态，但不会有输出数据。
 *
 * 4. Worker 端响应：
 *    - 在 doWork() 中可通过 isStopped 判断是否被取消，建议及时 return Result.failure() 或直接 return。
 *    - 注意：isStopped 不能只在 doWork() 顶部判断一次，必须在耗时循环、分段处理、或关键步骤之间多次主动检查，才能及时响应取消。
 *      例如：
 *        for (i in 1..100) {
 *            if (isStopped) return Result.failure()
 *            // 执行一小段工作
 *        }
 *    - 取消时不会回调 onStopped（仅 ListenableWorker 有 onStopped，普通 Worker 需主动检查 isStopped）。
 *
 * 5. 取消不是“强制杀死线程”，而是“请求终止”，实际中断时间取决于 Worker 实现。
 *
 * 6. 典型现象：
 *    - 取消后，任务状态会很快变为 CANCELLED。
 *    - 如果任务正在运行，可能会有短暂延迟，直到 Worker 响应。
 *    - 取消的任务不会有输出数据。
 *
 * 7. 建议：
 *    - 长时间任务建议定期检查 isStopped，保证取消能及时生效。
 *    - 业务侧可监听状态变化，及时更新 UI。
 */
package com.example.workmanagerdemo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*


/**
 *
 *
 *tag / workname 的区别
 * workmanager 重新打开发射历史消息
 */

@Composable
fun CancelUniqueWorkScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "任务取消与唯一任务演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<CancelWorker>().addTag("uniqueTag").build()
            workManager.enqueueUniqueWork("uniqueWork", ExistingWorkPolicy.REPLACE, request)
            status = "唯一任务已入队"
        }) {
            Text("启动唯一任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            workManager.cancelAllWorkByTag("uniqueTag")
            status = "已取消所有 uniqueTag 任务"
        }) {
            Text("取消任务")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
    }
}

class CancelWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("CancelWorker", "唯一任务执行")
        return Result.success()
    }
}
