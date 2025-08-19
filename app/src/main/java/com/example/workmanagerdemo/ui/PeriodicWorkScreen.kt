package com.example.workmanagerdemo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.example.workmanagerdemo.workers.PeriodicWorker
import java.util.concurrent.TimeUnit


/**
 * context 应用级别或activity 级别？
 */
@Composable
fun PeriodicWorkScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "周期性任务演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val request = PeriodicWorkRequestBuilder<PeriodicWorker>(15, TimeUnit.MINUTES).build()
            workManager.enqueue(request)
            workManager.getWorkInfoByIdLiveData(request.id).observeForever {
                status = "周期性任务状态: ${it?.state}"
            }
        }) {
            Text("启动周期性任务")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * ========================================
 * 【周期性任务详细使用场景与代码示例】
 * ========================================
 *
 * 周期性任务是 WorkManager 的重要特性，支持按照固定时间间隔重复执行任务。
 * 适用于需要定期执行的后台维护、数据同步、状态检查等场景。
 *
 * ========================================
 * 1. 数据同步与备份
 * ========================================
 *
 * 【场景：定期数据同步】
 * 应用场景：云盘应用、笔记应用、通讯录同步、邮件同步
 * 
 * 执行频率：15分钟 - 24小时
 * 数据流：本地数据检查 → 网络状态验证 → 数据同步 → 冲突解决 → 状态更新
 * 
 * 代码示例：
 * ```kotlin
 * // 通讯录同步任务
 * val contactsSyncWork = PeriodicWorkRequestBuilder<ContactsSyncWorker>(
 *     1, TimeUnit.HOURS // 每小时同步一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .setRequiresBatteryNotLow(true)
 *         .build()
 * ).setBackoffCriteria(
 *     BackoffPolicy.LINEAR, 
 *     Duration.ofMinutes(5)
 * ).build()
 * 
 * // 云盘数据同步任务
 * val cloudSyncWork = PeriodicWorkRequestBuilder<CloudSyncWorker>(
 *     6, TimeUnit.HOURS // 每6小时同步一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.UNMETERED) // 仅WiFi
 *         .setRequiresCharging(true) // 充电时执行
 *         .build()
 * ).build()
 * 
 * // 数据库备份任务
 * val backupWork = PeriodicWorkRequestBuilder<DatabaseBackupWorker>(
 *     24, TimeUnit.HOURS // 每天备份一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiresDeviceIdle(true) // 设备空闲时执行
 *         .setRequiresBatteryNotLow(true)
 *         .build()
 * ).build()
 * ```
 *
 * ========================================
 * 2. 系统维护与清理
 * ========================================
 *
 * 【场景：定期系统维护】
 * 应用场景：缓存清理、日志清理、临时文件清理、数据库优化
 * 
 * 执行频率：1小时 - 7天
 * 数据流：存储空间检查 → 清理策略选择 → 执行清理 → 结果统计 → 状态报告
 * 
 * 代码示例：
 * ```kotlin
 * // 缓存清理任务
 * val cacheCleanupWork = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
 *     12, TimeUnit.HOURS // 每12小时清理一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiresDeviceIdle(true)
 *         .setRequiresBatteryNotLow(true)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putLong("maxCacheSize", 100 * 1024 * 1024) // 100MB
 *         .putString("cleanupStrategy", "lru") // LRU策略
 *         .build()
 * ).build()
 * 
 * // 日志清理任务
 * val logCleanupWork = PeriodicWorkRequestBuilder<LogCleanupWorker>(
 *     7, TimeUnit.DAYS // 每周清理一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiresDeviceIdle(true)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putInt("maxLogDays", 30) // 保留30天日志
 *         .putLong("maxLogSize", 50 * 1024 * 1024) // 50MB
 *         .build()
 * ).build()
 * 
 * // 数据库优化任务
 * val dbOptimizeWork = PeriodicWorkRequestBuilder<DatabaseOptimizeWorker>(
 *     24, TimeUnit.HOURS // 每天优化一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiresDeviceIdle(true)
 *         .setRequiresBatteryNotLow(true)
 *         .build()
 * ).build()
 * ```
 *
 * ========================================
 * 3. 内容更新与推送
 * ========================================
 *
 * 【场景：定期内容更新】
 * 应用场景：新闻应用、社交媒体、电商应用、游戏更新
 * 
 * 执行频率：30分钟 - 12小时
 * 数据流：内容检查 → 更新检测 → 内容下载 → 本地存储 → 推送通知
 * 
 * 代码示例：
 * ```kotlin
 * // 新闻内容更新任务
 * val newsUpdateWork = PeriodicWorkRequestBuilder<NewsUpdateWorker>(
 *     2, TimeUnit.HOURS // 每2小时更新一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putString("category", "technology")
 *         .putInt("maxArticles", 50)
 *         .putBoolean("enablePush", true)
 *         .build()
 * ).build()
 * 
 * // 商品价格监控任务
 * val priceMonitorWork = PeriodicWorkRequestBuilder<PriceMonitorWorker>(
 *     30, TimeUnit.MINUTES // 每30分钟检查一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putStringArray("productIds", arrayOf("prod1", "prod2", "prod3"))
 *         .putDouble("priceThreshold", 100.0)
 *         .putBoolean("enableNotification", true)
 *         .build()
 * ).build()
 * 
 * // 游戏资源更新任务
 * val gameUpdateWork = PeriodicWorkRequestBuilder<GameResourceUpdateWorker>(
 *     6, TimeUnit.HOURS // 每6小时检查一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.UNMETERED)
 *         .setRequiresBatteryNotLow(true)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putString("gameVersion", "1.2.3")
 *         .putBoolean("autoDownload", false)
 *         .build()
 * ).build()
 * ```
 *
 * ========================================
 * 4. 健康检查与监控
 * ========================================
 *
 * 【场景：系统健康监控】
 * 应用场景：应用性能监控、错误统计、用户行为分析、崩溃报告
 * 
 * 执行频率：1小时 - 24小时
 * 数据流：性能数据收集 → 异常检测 → 数据分析 → 报告生成 → 远程上传
 * 
 * 代码示例：
 * ```kotlin
 * // 应用性能监控任务
 * val performanceMonitorWork = PeriodicWorkRequestBuilder<PerformanceMonitorWorker>(
 *     4, TimeUnit.HOURS // 每4小时监控一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiresDeviceIdle(true)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putBoolean("collectMemoryInfo", true)
 *         .putBoolean("collectCpuInfo", true)
 *         .putBoolean("collectBatteryInfo", true)
 *         .putString("uploadEndpoint", "https://analytics.example.com")
 *         .build()
 * ).build()
 * 
 * // 错误统计任务
 * val errorReportWork = PeriodicWorkRequestBuilder<ErrorReportWorker>(
 *     24, TimeUnit.HOURS // 每天报告一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putString("appVersion", "2.1.0")
 *         .putString("deviceModel", Build.MODEL)
 *         .putString("osVersion", Build.VERSION.RELEASE)
 *         .build()
 * ).build()
 * 
 * // 用户行为分析任务
 * val userAnalyticsWork = PeriodicWorkRequestBuilder<UserAnalyticsWorker>(
 *     12, TimeUnit.HOURS // 每12小时分析一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .setRequiresDeviceIdle(true)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putBoolean("trackScreenViews", true)
 *         .putBoolean("trackUserActions", true)
 *         .putBoolean("trackPerformance", true)
 *         .build()
 * ).build()
 * ```
 *
 * ========================================
 * 5. 安全与隐私
 * ========================================
 *
 * 【场景：定期安全检查】
 * 应用场景：安全扫描、隐私数据清理、证书更新、权限检查
 * 
 * 执行频率：1小时 - 7天
 * 数据流：安全检查 → 威胁检测 → 安全更新 → 日志记录 → 状态报告
 * 
 * 代码示例：
 * ```kotlin
 * // 安全扫描任务
 * val securityScanWork = PeriodicWorkRequestBuilder<SecurityScanWorker>(
 *     6, TimeUnit.HOURS // 每6小时扫描一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiresDeviceIdle(true)
 *         .setRequiresBatteryNotLow(true)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putBoolean("scanFiles", true)
 *         .putBoolean("scanNetwork", true)
 *         .putBoolean("scanPermissions", true)
 *         .putString("scanMode", "quick") // quick, full, custom
 *         .build()
 * ).build()
 * 
 * // 隐私数据清理任务
 * val privacyCleanupWork = PeriodicWorkRequestBuilder<PrivacyCleanupWorker>(
 *     24, TimeUnit.HOURS // 每天清理一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiresDeviceIdle(true)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putBoolean("clearCache", true)
 *         .putBoolean("clearCookies", true)
 *         .putBoolean("clearHistory", false) // 用户选择
 *         .putInt("retentionDays", 7)
 *         .build()
 * ).build()
 * 
 * // 证书更新任务
 * val certificateUpdateWork = PeriodicWorkRequestBuilder<CertificateUpdateWorker>(
 *     7, TimeUnit.DAYS // 每周检查一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putString("certificateAuthority", "https://ca.example.com")
 *         .putBoolean("autoUpdate", true)
 *         .putString("updateEndpoint", "https://certs.example.com/update")
 *         .build()
 * ).build()
 * ```
 *
 * ========================================
 * 6. 业务逻辑与定时任务
 * ========================================
 *
 * 【场景：业务定时任务】
 * 应用场景：定时提醒、预约管理、订阅续费、积分过期
 * 
 * 执行频率：1分钟 - 24小时
 * 数据流：业务规则检查 → 条件匹配 → 任务执行 → 结果记录 → 状态更新
 * 
 * 代码示例：
 * ```kotlin
 * // 定时提醒任务
 * val reminderWork = PeriodicWorkRequestBuilder<ReminderWorker>(
 *     15, TimeUnit.MINUTES // 每15分钟检查一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiresBatteryNotLow(true)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putString("reminderType", "medication")
 *         .putLong("lastReminderTime", 0L)
 *         .putBoolean("enableSound", true)
 *         .putBoolean("enableVibration", true)
 *         .build()
 * ).build()
 * 
 * // 订阅续费检查任务
 * val subscriptionCheckWork = PeriodicWorkRequestBuilder<SubscriptionCheckWorker>(
 *     24, TimeUnit.HOURS // 每天检查一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putInt("advanceNoticeDays", 7)
 *         .putBoolean("autoRenewal", true)
 *         .putString("paymentMethod", "credit_card")
 *         .build()
 * ).build()
 * 
 * // 积分过期检查任务
 * val pointsExpiryWork = PeriodicWorkRequestBuilder<PointsExpiryWorker>(
 *     12, TimeUnit.HOURS // 每12小时检查一次
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiresDeviceIdle(true)
 *         .build()
 * ).setInputData(
 *     Data.Builder()
 *         .putInt("expiryNoticeDays", 30)
 *         .putBoolean("enableNotification", true)
 *         .putString("notificationTitle", "积分即将过期")
 *         .build()
 * ).build()
 * ```
 *
 * ========================================
 * 7. 高级周期性任务模式
 * ========================================
 *
 * 【模式1：动态频率调整】
 * ```kotlin
 * // 根据网络状态动态调整同步频率
 * class AdaptiveSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
 *     override fun doWork(): Result {
 *         val networkType = getNetworkType()
 *         val newInterval = when (networkType) {
 *             NetworkType.WIFI -> 15 // 15分钟
 *             NetworkType.CONNECTED -> 30 // 30分钟
 *             else -> 60 // 60分钟
 *         }
 *         
 *         // 动态调整任务频率
 *         val newWork = PeriodicWorkRequestBuilder<AdaptiveSyncWorker>(
 *             newInterval.toLong(), TimeUnit.MINUTES
 *         ).build()
 *         
 *         WorkManager.getInstance(applicationContext)
 *             .enqueueUniquePeriodicWork(
 *                 "adaptive_sync",
 *                 ExistingPeriodicWorkPolicy.REPLACE,
 *                 newWork
 *             )
 *         
 *         return Result.success()
 *     }
 * }
 * ```
 *
 * 【模式2：条件执行控制】
 * ```kotlin
 * // 根据用户行为模式调整执行时间
 * class SmartMaintenanceWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
 *     override fun doWork(): Result {
 *         val userActivityPattern = getUserActivityPattern()
 *         val currentTime = System.currentTimeMillis()
 *         
 *         // 只在用户不活跃时执行维护任务
 *         if (isUserActive(currentTime, userActivityPattern)) {
 *             return Result.retry() // 延迟执行
 *         }
 *         
 *         // 执行维护任务
 *         performMaintenance()
 *         return Result.success()
 *     }
 * }
 * ```
 *
 * 【模式3：任务链组合】
 * ```kotlin
 * // 周期性任务 + 一次性任务链
 * val periodicWork = PeriodicWorkRequestBuilder<DataCollectorWorker>(
 *     1, TimeUnit.HOURS
 * ).build()
 * 
 * val processingChain = workManager
 *     .beginWith(OneTimeWorkRequestBuilder<DataProcessorWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<DataUploaderWorker>().build())
 *     .then(OneTimeWorkRequestBuilder<NotificationWorker>().build())
 * 
 * // 在周期性任务中触发任务链
 * class DataCollectorWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
 *     override fun doWork(): Result {
 *         val data = collectData()
 *         
 *         if (data.isNotEmpty()) {
 *             // 触发处理链
 *             val processingWork = OneTimeWorkRequestBuilder<DataProcessorWorker>()
 *                 .setInputData(Data.Builder().putString("collected_data", data).build())
 *                 .build()
 *             
 *             workManager.beginWith(processingWork)
 *                 .then(OneTimeWorkRequestBuilder<DataUploaderWorker>().build())
 *                 .then(OneTimeWorkRequestBuilder<NotificationWorker>().build())
 *                 .enqueue()
 *         }
 *         
 *         return Result.success()
 *     }
 * }
 * ```
 *
 * ========================================
 * 8. 最佳实践与注意事项
 * ========================================
 *
 * 【最佳实践1：频率选择】
 * - 最小间隔：15分钟（WorkManager 限制）
 * - 网络相关：30分钟 - 2小时
 * - 系统维护：6小时 - 24小时
 * - 用户相关：根据用户活跃时间调整
 *
 * 【最佳实践2：约束条件设置】
 * ```kotlin
 * // 推荐的约束组合
 * val constraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.CONNECTED) // 网络相关任务
 *     .setRequiresBatteryNotLow(true) // 电池相关任务
 *     .setRequiresCharging(false) // 避免过度限制
 *     .setRequiresDeviceIdle(true) // 系统维护任务
 *     .build()
 * ```
 *
 * 【最佳实践3：错误处理与重试】
 * ```kotlin
 * // 设置合理的重试策略
 * val workRequest = PeriodicWorkRequestBuilder<MyWorker>(
 *     1, TimeUnit.HOURS
 * ).setBackoffCriteria(
 *     BackoffPolicy.EXPONENTIAL, // 指数退避
 *     Duration.ofMinutes(5) // 初始延迟5分钟
 * ).build()
 * ```
 *
 * 【注意事项1：资源消耗】
 * - 避免过于频繁的执行
 * - 合理设置约束条件
 * - 监控任务执行时间
 * - 避免在任务中执行耗时操作
 *
 * 【注意事项2：电池优化】
 * - 使用 Doze 模式感知
 * - 避免在低电量时执行
 * - 合理利用设备空闲时间
 * - 考虑用户使用习惯
 *
 * ========================================
 * 9. 监控与调试
 * ========================================
 *
 * 【监控1：任务执行状态】
 * ```kotlin
 * // 监控周期性任务状态
 * workManager.getWorkInfosForUniqueWorkLiveData("periodic_task_name")
 *     .observe(lifecycleOwner) { workInfos ->
 *         workInfos.forEach { workInfo ->
 *             Log.d("PeriodicWork", "任务状态: ${workInfo.state}")
 *             Log.d("PeriodicWork", "执行次数: ${workInfo.runAttemptCount}")
 *             Log.d("PeriodicWork", "下次执行: ${workInfo.nextScheduledRunTime}")
 *         }
 *     }
 * ```
 *
 * 【监控2：性能指标】
 * ```kotlin
 * // 记录任务执行性能
 * class PerformancePeriodicWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
 *     override fun doWork(): Result {
 *         val startTime = System.currentTimeMillis()
 *         
 *         try {
 *             performTask()
 *             
 *             val executionTime = System.currentTimeMillis() - startTime
 *             
 *             // 记录性能指标
 *             recordPerformanceMetrics(executionTime)
 *             
 *             return Result.success()
 *         } catch (e: Exception) {
 *             val executionTime = System.currentTimeMillis() - startTime
 *             recordErrorMetrics(executionTime, e)
 *             return Result.failure()
 *         }
 *     }
 * }
 * ```
 *
 * ========================================
 * 总结
 * ========================================
 *
 * 周期性任务是 WorkManager 的重要特性，通过合理设计执行频率、约束条件和
 * 业务逻辑，可以构建高效、节能的后台任务系统。关键是要根据具体业务需求
 * 选择合适的时间间隔，设置合理的约束条件，并实现可靠的错误处理机制。
 *
 * 使用建议：
 * 1. 优先考虑用户使用习惯和设备状态
 * 2. 合理设置约束条件，避免过度限制
 * 3. 实现智能的频率调整机制
 * 4. 监控任务执行状态和性能指标
 * 5. 注意电池优化和系统资源管理
 */
