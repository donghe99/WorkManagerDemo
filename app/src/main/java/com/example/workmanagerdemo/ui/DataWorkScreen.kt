
/**
 * 数据任务演示页面
 *
 * 主要场景：
 * 1. 任务链：多个任务串联执行，前一个任务的输出作为下一个任务的输入。
 *    例如：下载文件 -> 解压文件 -> 处理数据 -> 上传结果。
 *    WorkManager 支持 beginWith/then 组合任务，并自动传递数据。
 *
 * 2. 参数传递：为 Worker 传递参数，如文件路径、账号、配置信息等。
 *    通过 setInputData 传递，Worker 内通过 inputData 获取。
 *
 * 3. 结果返回：Worker 执行完毕后返回结果，主线程或其它任务可获取。
 *    通过 Result.success(Data) 返回，WorkInfo.outputData 获取。
 *
 * 4. 任务分支：可根据前一个任务结果决定后续任务执行逻辑。
 *
 * 5. 轻量级数据存储：适合传递少量、结构化的基本数据（如状态、标识、简单配置等）。
 *
 * 注意：Data 只适合轻量级数据，不建议用于大文件或复杂对象传递。
 */
package com.example.workmanagerdemo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.example.workmanagerdemo.workers.DataWorker

private const val TAG = "DataWorkScreen"

// 示例 Worker：下载、解压、处理、上传
class DownloadWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val url = inputData.getString("url") ?: ""
        android.util.Log.d(TAG, "下载任务执行，url: $url")
        val filePath = "/sdcard/downloaded_file.txt" // 假设下载后路径
        val output = androidx.work.Data.Builder().putString("filePath", filePath).build()
        return Result.success(output)
    }
}

class UnzipWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val filePath = inputData.getString("filePath") ?: ""
        android.util.Log.d(TAG, "解压任务执行，filePath: $filePath")
        val unzipPath = "/sdcard/unzipped/" // 假设解压后路径
        val output = androidx.work.Data.Builder().putString("unzipPath", unzipPath).build()
        return Result.success(output)
    }
}

class ProcessWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val unzipPath = inputData.getString("unzipPath") ?: ""
        android.util.Log.d(TAG, "处理任务执行，unzipPath: $unzipPath")
        val result = "处理完成" // 假设处理结果
        val output = androidx.work.Data.Builder().putString("result", result).build()
        return Result.success(output)
    }
}

class UploadWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val result = inputData.getString("result") ?: ""
        android.util.Log.d(TAG, "上传任务执行，result: $result")
        val output = androidx.work.Data.Builder().putString("uploadStatus", "上传成功").build()
        return Result.success(output)
    }
}

@Composable
fun DataWorkScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "任务链与数据传递演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // DataWorker 单任务演示
        Text(text = "DataWorker 单任务演示：用于参数传递和结果返回", style = MaterialTheme.typography.titleMedium)
        Button(onClick = {
            val inputData = Data.Builder().putString("input_key", "测试数据").build()
            val request = OneTimeWorkRequestBuilder<DataWorker>()
                .setInputData(inputData)
                .build()
            workManager.enqueue(request)
            workManager.getWorkInfoByIdLiveData(request.id).observeForever {
                val output = it?.outputData?.getString("output_key") ?: "无输出"
                status = "DataWorker状态: ${it?.state}, 输出: $output"
            }
        }) {
            Text("启动 DataWorker 单任务")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 任务链演示
        Text(text = "任务链场景：下载→解压→处理→上传，数据自动传递", style = MaterialTheme.typography.titleMedium)
        Button(onClick = {
            // 1. 下载任务，传递 url
            val downloadData = Data.Builder().putString("url", "https://example.com/file.zip").build()
            val download = OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(downloadData).build()

            // 2. 解压任务，依赖下载
            val unzip = OneTimeWorkRequestBuilder<UnzipWorker>().build()

            // 3. 处理任务，依赖解压
            val process = OneTimeWorkRequestBuilder<ProcessWorker>().build()

            // 4. 上传任务，依赖处理
            val upload = OneTimeWorkRequestBuilder<UploadWorker>().build()

            // 任务链
            workManager
                .beginWith(download)
                .then(unzip)
                .then(process)
                .then(upload)
                .enqueue()

            // 监听最终任务结果
            workManager.getWorkInfoByIdLiveData(upload.id).observeForever {
                val uploadStatus = it?.outputData?.getString("uploadStatus") ?: "无输出"
                status = "上传任务状态: ${it?.state}, 结果: $uploadStatus"
            }
        }) {
            Text("启动任务链（下载→解压→处理→上传）")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * ========================================
 * 【数据任务详细使用场景与代码示例】
 * ========================================
 *
 * 数据任务是 WorkManager 的核心特性，支持任务间的数据传递、结果返回和任务链构建。
 * 适用于需要多步骤协作、数据流转的复杂业务场景。
 *
 * ========================================
 * 1. 文件处理流水线
 * ========================================
 *
 * 【场景：批量文件处理系统】
 * 应用场景：图片处理应用、文档转换工具、媒体文件处理
 * 
 * 数据流：原始文件 → 格式转换 → 压缩优化 → 质量检测 → 存储上传
 * 
 * 代码示例：
 * ```kotlin
 * // 图片处理流水线
 * val imageProcessingChain = workManager
 *     .beginWith(OneTimeWorkRequestBuilder<ImageDownloadWorker>()
 *         .setInputData(Data.Builder()
 *             .putString("imageUrl", "https://example.com/image.jpg")
 *             .putString("targetFormat", "webp")
 *             .build())
 *         .build())
 *     .then(OneTimeWorkRequestBuilder<ImageResizeWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<ImageCompressWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<ImageUploadWorker>().build())
 *     .enqueue()
 * ```
 *
 * ========================================
 * 2. 数据同步与备份
 * ========================================
 *
 * 【场景：多平台数据同步】
 * 应用场景：云盘应用、笔记应用、通讯录同步
 * 
 * 数据流：本地数据 → 冲突检测 → 数据合并 → 上传同步 → 状态确认
 * 
 * 代码示例：
 * ```kotlin
 * // 数据同步链
 * val syncChain = workManager
 *     .beginWith(OneTimeWorkRequestBuilder<LocalDataCollectorWorker>()
 *         .setInputData(Data.Builder()
 *             .putString("syncType", "contacts")
 *             .putLong("lastSyncTime", System.currentTimeMillis())
 *             .build())
 *         .build())
 *     .then(OneTimeWorkRequestBuilder<ConflictResolverWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<DataMergerWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<CloudUploadWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<SyncStatusUpdaterWorker>().build())
 *     .enqueue()
 * ```
 *
 * ========================================
 * 3. 机器学习与数据分析
 * ========================================
 *
 * 【场景：离线AI模型训练】
 * 应用场景：智能相机、语音识别、推荐系统
 * 
 * 数据流：数据收集 → 预处理 → 模型训练 → 模型评估 → 模型部署
 * 
 * 代码示例：
 * ```kotlin
 * // AI模型训练链
 * val aiTrainingChain = workManager
 *     .beginWith(OneTimeWorkRequestBuilder<DataCollectorWorker>()
 *         .setInputData(Data.Builder()
 *             .putString("modelType", "image_classification")
 *             .putInt("dataSize", 10000)
 *             .build())
 *         .build())
 *     .then(OneTimeWorkRequestBuilder<DataPreprocessorWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<ModelTrainerWorker>()
 *         .setInputData(Data.Builder()
 *             .putString("algorithm", "cnn")
 *             .putInt("epochs", 100)
 *             .build())
 *         .build())
 *     .then(OneTimeWorkRequestBuilder<ModelEvaluatorWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<ModelDeployerWorker>().build())
 *     .enqueue()
 * ```
 *
 * ========================================
 * 4. 电商订单处理
 * ========================================
 *
 * 【场景：订单生命周期管理】
 * 应用场景：电商平台、外卖应用、服务预约
 * 
 * 数据流：订单创建 → 库存检查 → 支付处理 → 物流安排 → 配送跟踪
 * 
 * 代码示例：
 * ```kotlin
 * // 订单处理链
 * val orderProcessingChain = workManager
 *     .beginWith(OneTimeWorkRequestBuilder<OrderValidatorWorker>()
 *         .setInputData(Data.Builder()
 *             .putString("orderId", "ORD123456")
 *             .putString("userId", "user789")
 *             .putDouble("totalAmount", 299.99)
 *             .build())
 *         .build())
 *     .then(OneTimeWorkRequestBuilder<InventoryCheckerWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<PaymentProcessorWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<LogisticsArrangerWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<OrderStatusUpdaterWorker>().build())
 *     .enqueue()
 * ```
 *
 * ========================================
 * 5. 内容创作与发布
 * ========================================
 *
 * 【场景：多媒体内容制作】
 * 应用场景：视频编辑、播客制作、文章发布
 * 
 * 数据流：原始素材 → 内容编辑 → 质量检查 → 格式转换 → 发布上线
 * 
 * 代码示例：
 * ```kotlin
 * // 视频制作链
 * val videoProductionChain = workManager
 *     .beginWith(OneTimeWorkRequestBuilder<VideoDownloadWorker>()
 *         .setInputData(Data.Builder()
 *             .putString("videoUrl", "https://example.com/raw_video.mp4")
 *             .putString("projectId", "proj_001")
 *             .build())
 *         .build())
 *     .then(OneTimeWorkRequestBuilder<VideoEditorWorker>()
 *         .setInputData(Data.Builder()
 *             .putString("editConfig", "trim:0-60,add_watermark:true")
 *             .build())
 *         .build())
 *     .then(OneTimeWorkRequestBuilder<VideoCompressorWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<QualityCheckerWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<VideoPublisherWorker>().build())
 *     .enqueue()
 * ```
 *
 * ========================================
 * 6. 数据迁移与升级
 * ========================================
 *
 * 【场景：数据库架构升级】
 * 应用场景：应用版本升级、数据迁移、系统重构
 * 
 * 数据流：数据备份 → 结构转换 → 数据迁移 → 完整性验证 → 新系统激活
 * 
 * 代码示例：
 * ```kotlin
 * // 数据迁移链
 * val dataMigrationChain = workManager
 *     .beginWith(OneTimeWorkRequestBuilder<DatabaseBackupWorker>()
 *         .setInputData(Data.Builder()
 *             .putString("dbName", "user_database")
 *             .putString("backupPath", "/backup/v2.0/")
 *             .build())
 *         .build())
 *     .then(OneTimeWorkRequestBuilder<SchemaUpdaterWorker>()
 *         .setInputData(Data.Builder()
 *             .putString("targetVersion", "2.0")
 *             .putString("migrationScript", "v1_to_v2.sql")
 *             .build())
 *         .build())
 *     .then(OneTimeWorkRequestBuilder<DataMigratorWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<DataValidatorWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<SystemActivatorWorker>().build())
 *     .enqueue()
 * ```
 *
 * ========================================
 * 7. 高级数据流模式
 * ========================================
 *
 * 【场景1：条件分支执行】
 * ```kotlin
 * // 根据前一个任务结果决定后续执行路径
 * val conditionalChain = workManager
 *     .beginWith(OneTimeWorkRequestBuilder<DataAnalyzerWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<DecisionMakerWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<PathAWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<PathBWorker>().build())
 *     .enqueue()
 * 
 * // DecisionMakerWorker 根据分析结果选择执行 PathA 或 PathB
 * class DecisionMakerWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
 *     override fun doWork(): Result {
 *         val analysisResult = inputData.getString("analysis_result") ?: ""
 *         val decision = if (analysisResult.contains("success")) "path_a" else "path_b"
 *         
 *         val output = Data.Builder()
 *             .putString("decision", decision)
 *             .putString("next_path", decision)
 *             .build()
 *         
 *         return Result.success(output)
 *     }
 * }
 * ```
 *
 * 【场景2：并行任务聚合】
 * ```kotlin
 * // 多个并行任务完成后聚合结果
 * val parallelWork1 = OneTimeWorkRequestBuilder<WorkerA>().build()
 * val parallelWork2 = OneTimeWorkRequestBuilder<WorkerB>().build()
 * val parallelWork3 = OneTimeWorkRequestBuilder<WorkerC>().build()
 * 
 * val aggregationWork = OneTimeWorkRequestBuilder<ResultAggregatorWorker>().build()
 * 
 * workManager
 *     .beginWith(listOf(parallelWork1, parallelWork2, parallelWork3))
 *     .then(aggregationWork)
 *     .enqueue()
 * 
 * // ResultAggregatorWorker 收集所有并行任务的结果
 * class ResultAggregatorWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
 *     override fun doWork(): Result {
 *         // 获取所有前置任务的结果
 *         val workInfos = workManager.getWorkInfosByIds(
 *             listOf(parallelWork1.id, parallelWork2.id, parallelWork3.id)
 *         )
 *         
 *         val aggregatedResult = workInfos.joinToString { workInfo ->
 *             "${workInfo.tags.first()}: ${workInfo.outputData.getString("result")}"
 *         }
 *         
 *         val output = Data.Builder()
 *             .putString("aggregated_result", aggregatedResult)
 *             .build()
 *         
 *         return Result.success(output)
 *     }
 * }
 * ```
 *
 * 【场景3：循环任务链】
 * ```kotlin
 * // 支持循环执行的任务链（如重试机制）
 * val retryChain = workManager
 *     .beginWith(OneTimeWorkRequestBuilder<MainTaskWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<RetryDecisionWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<MainTaskWorker>().build()) // 可以重复
 *     .enqueue()
 * 
 * // RetryDecisionWorker 决定是否需要重试
 * class RetryDecisionWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
 *     override fun doWork(): Result {
 *         val attemptCount = inputData.getInt("attempt_count", 0)
 *         val maxRetries = inputData.getInt("max_retries", 3)
 *         
 *         val shouldRetry = attemptCount < maxRetries
 *         
 *         val output = Data.Builder()
 *             .putBoolean("should_retry", shouldRetry)
 *             .putInt("next_attempt", attemptCount + 1)
 *             .build()
 *         
 *         return if (shouldRetry) Result.retry() else Result.success(output)
 *     }
 * }
 * ```
 *
 * ========================================
 * 8. 数据传递最佳实践
 * ========================================
 *
 * 【原则1：数据大小控制】
 * - Data 对象适合传递轻量级数据（< 1MB）
 * - 大文件使用文件路径传递，Worker 内部读取
 * - 复杂对象序列化为 JSON 字符串
 *
 * 【原则2：数据类型选择】
 * ```kotlin
 * // 推荐的数据类型
 * val data = Data.Builder()
 *     .putString("text", "简单文本")           // ✅ 推荐
 *     .putInt("number", 42)                   // ✅ 推荐
 *     .putLong("timestamp", System.currentTimeMillis()) // ✅ 推荐
 *     .putBoolean("flag", true)               // ✅ 推荐
 *     .putFloat("price", 19.99f)              // ✅ 推荐
 *     .putStringArray("tags", arrayOf("tag1", "tag2")) // ✅ 推荐
 *     .putString("jsonData", gson.toJson(complexObject)) // ✅ 复杂对象
 *     .build()
 *
 * // 避免的数据类型
 * val badData = Data.Builder()
 *     .putByteArray("largeFile", largeByteArray) // ❌ 大文件
 *     .putString("hugeText", veryLongString)     // ❌ 超长文本
 *     .build()
 * ```
 *
 * 【原则3：错误处理与回滚】
 * ```kotlin
 * // 任务链中的错误处理
 * class SafeWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
 *     override fun doWork(): Result {
 *         try {
 *             // 执行主要逻辑
 *             val result = performMainTask()
 *             
 *             // 返回成功结果
 *             val output = Data.Builder()
 *                 .putString("result", result)
 *                 .putLong("timestamp", System.currentTimeMillis())
 *                 .build()
 *             
 *             return Result.success(output)
 *             
 *         } catch (e: Exception) {
 *             // 记录错误信息
 *             Log.e("SafeWorker", "任务执行失败", e)
 *             
 *             // 返回错误信息，供后续任务处理
 *             val errorOutput = Data.Builder()
 *                 .putString("error", e.message)
 *                 .putString("errorType", e.javaClass.simpleName)
 *                 .putLong("errorTime", System.currentTimeMillis())
 *                 .build()
 *             
 *             return Result.failure(errorOutput)
 *         }
 *     }
 * }
 * ```
 *
 * ========================================
 * 9. 性能优化建议
 * ========================================
 *
 * 【优化1：任务粒度控制】
 * - 单个任务执行时间控制在 30 秒以内
 * - 避免在 Worker 中执行耗时操作（如网络请求）
 * - 使用 CoroutineWorker 进行异步操作
 *
 * 【优化2：数据缓存策略】
 * ```kotlin
 * // 使用 SharedPreferences 缓存中间结果
 * class CachingWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
 *     override fun doWork(): Result {
 *         val cacheKey = inputData.getString("cache_key") ?: "default"
 *         
 *         // 检查缓存
 *         val cachedResult = getCachedResult(cacheKey)
 *         if (cachedResult != null) {
 *             return Result.success(Data.Builder()
 *                 .putString("result", cachedResult)
 *                 .putBoolean("from_cache", true)
 *                 .build())
 *         }
 *         
 *         // 执行任务并缓存结果
 *         val result = performTask()
 *         cacheResult(cacheKey, result)
 *         
 *         return Result.success(Data.Builder()
 *             .putString("result", result)
 *             .putBoolean("from_cache", false)
 *             .build())
 *     }
 * }
 * ```
 *
 * 【优化3：任务优先级管理】
 * ```kotlin
 * // 根据业务重要性设置任务优先级
 * val highPriorityWork = OneTimeWorkRequestBuilder<CriticalWorker>()
 *     .setInputData(inputData)
 *     .setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ofMinutes(1))
 *     .setConstraints(Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .build())
 *     .build()
 * 
 * // 设置高优先级
 * workManager.enqueueUniqueWork(
 *     "critical_task",
 *     ExistingWorkPolicy.REPLACE,
 *     highPriorityWork
 * )
 * ```
 *
 * ========================================
 * 10. 监控与调试
 * ========================================
 *
 * 【监控1：任务执行状态】
 * ```kotlin
 * // 监控整个任务链的执行状态
 * workManager.getWorkInfosByIdsLiveData(
 *     listOf(download.id, unzip.id, process.id, upload.id)
 * ).observe(lifecycleOwner) { workInfos ->
 *     val statusMap = workInfos.associate { workInfo ->
 *         workInfo.tags.first() to workInfo.state
 *     }
 *     
 *     Log.d("TaskChain", "任务链状态: $statusMap")
 *     
 *     // 检查是否有失败的任务
 *     val failedTasks = workInfos.filter { it.state == WorkInfo.State.FAILED }
 *     if (failedTasks.isNotEmpty()) {
 *         Log.e("TaskChain", "失败的任务: ${failedTasks.map { it.tags.first() }}")
 *     }
 * }
 * ```
 *
 * 【监控2：性能指标收集】
 * ```kotlin
 * // 收集任务执行时间等性能指标
 * class PerformanceWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
 *     override fun doWork(): Result {
 *         val startTime = System.currentTimeMillis()
 *         
 *         try {
 *             // 执行主要逻辑
 *             val result = performTask()
 *             
 *             val executionTime = System.currentTimeMillis() - startTime
 *             
 *             // 记录性能指标
 *             val output = Data.Builder()
 *                 .putString("result", result)
 *                 .putLong("execution_time_ms", executionTime)
 *                 .putLong("start_time", startTime)
 *                 .putLong("end_time", System.currentTimeMillis())
 *                 .build()
 *             
 *             return Result.success(output)
 *             
 *         } catch (e: Exception) {
 *             val executionTime = System.currentTimeMillis() - startTime
 *             
 *             // 记录失败时的性能指标
 *             val errorOutput = Data.Builder()
 *                 .putString("error", e.message)
 *                 .putLong("execution_time_ms", executionTime)
 *                 .putLong("start_time", startTime)
 *                 .build()
 *             
 *             return Result.failure(errorOutput)
 *         }
 *     }
 * }
 * ```
 *
 * ========================================
 * 总结
 * ========================================
 *
 * 数据任务是 WorkManager 最强大的特性之一，通过合理设计任务链和数据流，
 * 可以构建复杂而可靠的异步处理系统。关键是要根据具体业务场景选择合适
 * 的任务粒度、数据传递方式和错误处理策略，确保系统的可维护性和性能。
 */
