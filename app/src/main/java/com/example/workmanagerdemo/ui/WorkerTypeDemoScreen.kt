package com.example.workmanagerdemo.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import androidx.work.ListenableWorker
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import androidx.work.CoroutineWorker
import androidx.work.rxjava3.RxWorker
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.util.concurrent.SettableFuture
import io.reactivex.rxjava3.core.Single

/**
 * Worker 类型对比与用法演示页面
 *
 * 1. Worker（同步任务）
 *    - 适合短时、阻塞式任务，doWork() 直接返回 Result。
 *    - 需主动检查 isStopped 响应取消。
 *
 * 2. CoroutineWorker（协程异步任务）
 *    - 适合 Kotlin 协程场景，doWork() 为 suspend 函数。
 *    - 支持挂起、异步、自动感知取消。
 *    - 最常用原因：
 *      * 与 Kotlin 协程生态完美集成，代码更简洁易读
 *      * 支持结构化并发，自动处理生命周期和取消
 *      * 可以轻松使用 async/await、flow 等协程特性
 *      * 性能更好，避免线程阻塞，资源利用更高效
 *      * 错误处理更优雅，支持 try-catch 和协程异常处理器
 *      * 与 Jetpack Compose 等现代 Android 组件配合更好
 *
 * 3. RxWorker（RxJava 异步任务）
 *    - 适合 RxJava 场景，doWork() 返回 Single<Result>。
 *    - 支持响应式流、自动感知取消。
 *
 * 用法建议：
 * - 普通同步任务优先用 Worker。
 * - 协程异步任务优先用 CoroutineWorker（最常用）。
 *   * 现代 Android 开发的标准选择
 *   * 支持所有异步场景：网络请求、数据库操作、文件 I/O 等
 *   * 代码维护性更好，团队协作更容易
 * - RxJava 场景用 RxWorker。
 */
@Composable
fun WorkerTypeDemoScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Worker 类型对比与用法演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<DemoWorker>().build()
            workManager.enqueue(request)
            status = "普通 Worker 已入队"
        }) { Text("启动普通 Worker") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<DemoCoroutineWorker>().build()
            workManager.enqueue(request)
            status = "CoroutineWorker 已入队"
        }) { Text("启动 CoroutineWorker") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<DemoRxWorker>().build()
            workManager.enqueue(request)
            status = "RxWorker 已入队"
        }) { Text("启动 RxWorker") }

    }
}

/**
 * 普通同步 Worker 示例
 */
class DemoWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Log.d("DemoWorker", "同步任务执行")
        for (i in 1..5) {
            if (isStopped) return Result.failure() // 响应取消
            Thread.sleep(500)
        }
        return Result.success()
    }
}

/**
 * 协程异步 Worker 示例
 */
class DemoCoroutineWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Log.d("DemoCoroutineWorker", "协程任务执行")
        for (i in 1..5) {
            if (isStopped) return Result.failure() // 协程也可主动检查
            delay(500)
        }
        return Result.success()
    }
}

/**
 * RxJava 异步 Worker 示例
 */
class DemoRxWorker(ctx: Context, params: WorkerParameters) : RxWorker(ctx, params) {
    override fun createWork(): Single<Result> {
        Log.d("DemoRxWorker", "RxJava任务执行")
        return Single.create { emitter ->
            for (i in 1..5) {
                if (isStopped) {
                    emitter.onSuccess(Result.failure())
                    return@create
                }
                Thread.sleep(500)
            }
            emitter.onSuccess(Result.success())
        }
    }
}

