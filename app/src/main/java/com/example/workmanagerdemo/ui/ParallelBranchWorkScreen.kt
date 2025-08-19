
/**
 * 任务链并行与分支演示页面
 *
 * 使用场景：
 * 1. 多个任务需要并行执行，且后续任务依赖于这些任务全部完成。
 *    例如：并行下载多个文件，全部下载完成后统一处理。
 * 2. 任务链可实现分支和合并，适合复杂流程控制。
 *
 * 方法说明：
 * - 串行任务链：beginWith(...).then(...).then(...)，任务按顺序依赖执行。
 * - 并行任务链：beginWith(listOf(...))，多个任务同时开始，全部完成后执行后续任务。
 * - 分支任务链：可通过多个 beginWith 创建分支，最后用 WorkContinuation.combine 合并。
 * - 条件分支：可根据前一个任务输出数据决定后续任务逻辑（需自定义 Worker）。
 * - 任务链支持唯一性和追加（APPEND），可构建复杂流程。
 *
 * 节点失败影响说明（图示）：
 *
 * 节点状态说明：
 * - 已执行节点：如果正常完成，监听到的状态为 SUCCEEDED；如果执行失败，监听到的状态为 FAILED。
 * - 执行失败节点：监听到的状态为 FAILED。
 * - 未执行节点（因依赖失败未被调度）：
 *   - 初始状态为 BLOCKED，表示等待依赖任务完成。
 *   - BLOCKED 状态出现时机（图示）：
 *
 *     依赖未完成：
 *     [A]   [B]
 *       \   /
 *        [C]
 *     若 A 或 B 还未完成，C 处于 BLOCKED（等待）。
 *
 *     依赖全部成功：
 *     [A] --success-->
 *     [B] --success-->
 *        [C] ENQUEUED → RUNNING → SUCCEEDED
 *
 *     依赖有失败/取消：
 *     [A] --failure-->
 *     [B] --success-->
 *        [C] BLOCKED → CANCELLED（不会执行）
 *
 * BLOCKED 状态监听示例：
 *
 * WorkManager 支持监听所有任务的所有状态（ENQUEUED、RUNNING、SUCCEEDED、FAILED、BLOCKED、CANCELLED），
 * 可通过 getWorkInfoByIdLiveData、getWorkInfosByTagLiveData、getWorkInfosForUniqueWorkLiveData 等方法实时获取。
 *
 * 任务状态典型顺序：
 * 1. BLOCKED（有依赖未完成时）
 * 2. ENQUEUED（依赖全部成功，准备执行）
 * 3. RUNNING（开始执行）
 * 4. SUCCEEDED（执行成功）
 * 5. FAILED（执行失败）
 * 6. CANCELLED（被取消或依赖失败未执行）
 *
 * val workC = OneTimeWorkRequestBuilder<WorkerC>().build()
 * workManager.getWorkInfoByIdLiveData(workC.id).observeForever {
 *     if (it?.state == WorkInfo.State.BLOCKED) {
 *         // 任务C正处于等待依赖完成的状态
 *     }
 *     if (it?.state == WorkInfo.State.CANCELLED) {
 *         // 依赖失败或取消，任务C不会执行
 *     }
 * }
 *   - 如果依赖任务失败或被取消，节点会被自动取消，状态变为 CANCELLED。
 *   - 整个生命周期不会进入 RUNNING 或 SUCCEEDED，只会经历 BLOCKED → CANCELLED。
 * - 整个任务链失败时，只有已执行的节点有 SUCCEEDED 或 FAILED，未执行的节点不会有 SUCCEEDED。
 *
 * 1. 串行任务链：
 *    A → B → C
 *    若 B 失败，则 C 不会执行。
 *    [A] --success--> [B] --failure--> (C 不执行)
 *
 * 2. 并行任务链：
 *    [A]   [B]
 *      \   /
 *       [C]
 *    若 A 或 B 任何一个失败，则 C 不会执行。
 *    [A] --failure-->
 *    [B] --success-->
 *    (C 不执行)
 *
 * 3. 分支合并任务链：
 *    [A]   [B]
 *      \   /
 *       [C]
 *    若 A 或 B 任何一个分支失败，则 C 不会执行。
 *    [A] --success-->
 *    [B] --failure-->
 *    (C 不执行)
 */
package com.example.workmanagerdemo.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*

private const val TAG = "ParallelBranchWorkScreen"


@Composable
fun ParallelBranchWorkScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }
    // 新增：分别保存 workA 和 workC 的状态
    var workAStatus by remember { mutableStateOf("未开始") }
    var workCStatus by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "任务链串行、并行、分支与合并演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // 串行任务链演示
        Button(onClick = {
            val workA = OneTimeWorkRequestBuilder<BranchWorker>().build()
            val workB = OneTimeWorkRequestBuilder<BranchWorker>().build()
            val workC = OneTimeWorkRequestBuilder<BranchWorker>().build()
            val continuation = workManager.beginWith(workA).then(workB).then(workC)
            continuation.enqueue()
            workManager.getWorkInfoByIdLiveData(workC.id).observeForever {
                status = "串行任务C状态: ${it?.state}"
            }
            // 清空分支状态
            workAStatus = "未开始"
            workCStatus = "未开始"
        }) {
            Text("启动串行任务链 (A→B→C)")
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 并行任务链演示
        Button(onClick = {
            val workA = OneTimeWorkRequestBuilder<BranchWorker>().build()
            val workB = OneTimeWorkRequestBuilder<BranchWorker>().build()
            val workC = OneTimeWorkRequestBuilder<BranchWorker>().build()
            // workA 和 workB 并行执行，全部完成后执行 workC
            // 执行顺序：workA + workB（并行） → workC（串行）
            val continuation = workManager.beginWith(listOf(workA, workB)).then(workC)
            continuation.enqueue()
            workManager.getWorkInfoByIdLiveData(workC.id).observeForever {
                status = "并行分支任务C状态: ${it?.state}"
            }
            // 清空分支状态
            workAStatus = "未开始"
            workCStatus = "未开始"
        }) {
            Text("启动并行分支任务链 ([A,B]→C)")
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 分支与合并任务链演示
        Button(onClick = {
            val workA = OneTimeWorkRequestBuilder<BranchWorkerA>().build()
            val workB = OneTimeWorkRequestBuilder<BranchWorker>().build()
            val workC = OneTimeWorkRequestBuilder<BranchWorker>().build()
            // branch1: workA 串行分支，branch2: workB 串行分支
            // WorkContinuation.combine 合并分支，全部完成后执行 workC
            // 执行顺序：workA（分支1）+ workB（分支2）（并行） → workC（合并后串行）
            val branch1 = workManager.beginWith(workA)
            val branch2 = workManager.beginWith(workB)
            val combined = WorkContinuation.combine(listOf(branch1, branch2)).then(workC)
            combined.enqueue()

            // 分别监听 workA 和 workC 的状态，并分别保存
            workManager.getWorkInfoByIdLiveData(workA.id).observeForever {
                Log.d(TAG, "workA state :" + it?.state)
                val data = it?.outputData?.getString("key_data")
                workAStatus = "分支合并任务A状态: ${it?.state}" + (if (data != null) " data: $data" else "")
            }

            workManager.getWorkInfoByIdLiveData(workC.id).observeForever {
                Log.d(TAG, "workC state :" + it?.state)
                workCStatus = "分支合并任务C状态: ${it?.state}"
            }
            // 清空主 status
            status = "未开始"
        }) {
            Text("启动分支合并任务链 ((A+B)→C)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
        // 新增：分别显示 workA 和 workC 的状态
        Text(text = workAStatus, style = MaterialTheme.typography.bodyLarge)
        Text(text = workCStatus, style = MaterialTheme.typography.bodyLarge)
    }
}

class BranchWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("BranchWorker", "分支任务执行")
        return Result.failure()
    }
}

class BranchWorkerA(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("BranchWorker", "分支任务执行")
        val output = androidx.work.Data.Builder().putString("key_data", "BranchWorkerA 执行成功").build()
        return Result.Success(output)
    }
}
