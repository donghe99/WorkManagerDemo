package com.example.workmanagerdemo.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

/**
 * 一次性任务 Worker 示例。
 * 用于演示 WorkManager 的一次性任务调度能力。
 * 只会执行一次，适合如上传日志、同步数据等场景。
 * 使用方法：
 * val request = OneTimeWorkRequestBuilder<SimpleWorker>().build()
 * WorkManager.getInstance(context).enqueue(request)
 */
class SimpleWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        // 这里可以执行一次性后台任务
        android.util.Log.d("SimpleWorker", "一次性任务执行")
        return Result.success()
    }
}
