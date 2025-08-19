package com.example.workmanagerdemo.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

/**
 * 周期性任务 Worker 示例。
 * 用于演示 WorkManager 的周期性任务调度能力。
 * 可定期执行任务，如定时同步、定时清理等。
 *
 * 使用方法：
 * // 创建一个周期性任务请求
 * val request = PeriodicWorkRequestBuilder<PeriodicWorker>(15, TimeUnit.MINUTES).build()
 * WorkManager.getInstance(context).enqueue(request)
 *
 * PeriodicWorkRequestBuilder 参数说明：
 * @param repeatInterval 任务的重复间隔时间，单位由第二个参数指定。必须大于等于 15 分钟（WorkManager 平台限制，低于会抛异常）。
 * @param repeatIntervalTimeUnit 间隔时间的单位，如 TimeUnit.MINUTES、TimeUnit.HOURS 等。
 *
 * 最小间隔：15 分钟。
 * 最大间隔：没有硬性限制，可设置为任意大的时间间隔。
 *
 * 例如：
 * PeriodicWorkRequestBuilder<PeriodicWorker>(15, TimeUnit.MINUTES)
 * 表示每 15 分钟执行一次该 Worker。
 *
 * 你还可以设置约束、输入数据等：
 * val constraints = Constraints.Builder().setRequiresCharging(true).build()
 * val request = PeriodicWorkRequestBuilder<PeriodicWorker>(1, TimeUnit.HOURS)
 *     .setConstraints(constraints)
 *     .build()
 */
class PeriodicWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        // 这里可以执行周期性后台任务
        android.util.Log.d("PeriodicWorker", "周期性任务执行")
        return Result.success()
    }
}
