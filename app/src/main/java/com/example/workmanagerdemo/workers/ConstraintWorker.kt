package com.example.workmanagerdemo.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

/**
 * 约束任务 Worker 示例。
 * 用于演示 WorkManager 的任务约束能力。
 * 可设置如网络、充电等条件，满足时才执行。
 *
 * 可用约束条件（Constraints.Builder）：
 * 1. setRequiredNetworkType(NetworkType) // 网络类型（NONE、CONNECTED、UNMETERED、NOT_ROAMING、METERED）
 * 2. setRequiresCharging(boolean)        // 设备是否充电
 * 3. setRequiresDeviceIdle(boolean)      // 设备是否空闲（Android 6.0+）。
 *    设为 true 时，只有设备进入空闲（如长时间未操作、Doze模式）才会执行任务，适合低优先级、可延迟的任务。
 * 4. setRequiresBatteryNotLow(boolean)   // 电量是否不低。
 *    设为 true 时，只有设备电量充足（未进入低电量模式）才会执行任务，避免影响用户体验。
 * 5. setRequiresStorageNotLow(boolean)   // 存储空间是否不低。
 *    设为 true 时，只有设备存储空间充足（未进入低存储模式）才会执行任务，适合需要写入大量数据的任务。
 * 6. addContentUriTrigger(Uri, boolean)  // 内容 URI 变化触发（Android 11+）。
 *    监听指定内容 URI（如媒体库、数据库等），当内容发生变化时自动触发任务。第二个参数为是否触发时需要设备处于空闲。
 *
 * 示例：
 * val constraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.CONNECTED)
 *     .setRequiresCharging(true)
 *     .setRequiresDeviceIdle(false)
 *     .setRequiresBatteryNotLow(true)
 *     .setRequiresStorageNotLow(true)
 *     .build()
 * val request = OneTimeWorkRequestBuilder<ConstraintWorker>()
 *     .setConstraints(constraints)
 *     .build()
 * WorkManager.getInstance(context).enqueue(request)
 */
class ConstraintWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        // 这里可以执行带约束的后台任务
        android.util.Log.d("ConstraintWorker", "约束任务执行")
        return Result.success()
    }
}
