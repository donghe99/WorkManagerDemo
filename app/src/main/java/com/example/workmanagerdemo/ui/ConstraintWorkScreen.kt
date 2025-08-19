package com.example.workmanagerdemo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.example.workmanagerdemo.workers.ConstraintWorker

@Composable
fun ConstraintWorkScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "约束任务演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(true)
                .build()
            val request = OneTimeWorkRequestBuilder<ConstraintWorker>()
                .setConstraints(constraints)
                .build()
            workManager.enqueue(request)
            workManager.getWorkInfoByIdLiveData(request.id).observeForever {
                status = "约束任务状态: ${it?.state}"
            }
        }) {
            Text("启动约束任务")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        // 约束任务使用说明
        Text(text = "约束任务使用说明", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "约束任务用于控制任务在什么条件下执行，确保任务在最佳条件下运行。",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * ========================================
 * 【约束任务详细使用场景与代码示例】
 * ========================================
 * 
 * 约束任务是 WorkManager 的重要特性，用于根据设备状态、网络条件、资源状况等
 * 来智能调度任务，确保任务在最佳条件下执行。
 * 
 * ========================================
 * 1. 网络相关约束
 * ========================================
 * 
 * 【场景：需要网络连接的任务】
 * 
 * // 数据同步任务：只有在网络连接时才执行
 * val constraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.CONNECTED)  // 需要网络连接
 *     .build()
 * 
 * // 使用场景：
 * // - 数据上传/下载
 * // - 云端同步
 * // - API 调用
 * // - 推送通知
 * 
 * 【场景：WiFi 网络任务】
 * 
 * // 大文件上传：只在 WiFi 网络下执行
 * val constraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.UNMETERED)  // 需要 WiFi（非计费网络）
 *     .build()
 * 
 * // 使用场景：
 * // - 大文件上传
 * // - 视频同步
 * // - 应用更新下载
 * // - 备份数据上传
 * 
 * ========================================
 * 2. 设备状态约束
 * ========================================
 * 
 * 【场景：充电状态约束】
 * 
 * // 耗电任务：只在充电时执行
 * val constraints = Constraints.Builder()
 *     .setRequiresCharging(true)  // 需要充电
 *     .build()
 * 
 * // 使用场景：
 * // - 大量数据处理
 * // - 文件压缩/解压
 * // - 数据库重建
 * // - 机器学习训练
 * 
 * 【场景：设备空闲约束】
 * 
 * // 低优先级任务：只在设备空闲时执行
 * val constraints = Constraints.Builder()
 *     .setRequiresDeviceIdle(true)  // 需要设备空闲
 *     .build()
 * 
 * // 使用场景：
 * // - 数据清理
 * // - 缓存优化
 * // - 日志分析
 * // - 系统维护
 * 
 * ========================================
 * 3. 资源状态约束
 * ========================================
 * 
 * 【场景：电量约束】
 * 
 * // 重要任务：只在电量充足时执行
 * val constraints = Constraints.Builder()
 *     .setRequiresBatteryNotLow(true)  // 电量不低
 *     .build()
 * 
 * // 使用场景：
 * // - 关键数据同步
 * // - 安全更新
 * // - 用户重要操作
 * // - 系统关键任务
 * 
 * 【场景：存储空间约束】
 * 
 * // 存储相关任务：只在存储空间充足时执行
 * val constraints = Constraints.Builder()
 *     .setRequiresStorageNotLow(true)  // 存储空间不低
 *     .build()
 * 
 * // 使用场景：
 * // - 文件下载
 * // - 数据备份
 * // - 缓存更新
 * // - 日志写入
 * 
 * ========================================
 * 4. 组合约束场景
 * ========================================
 * 
 * 【场景：智能同步策略】
 * 
 * // 智能同步：在最佳条件下执行
 * val constraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.UNMETERED)  // WiFi 网络
 *     .setRequiresCharging(true)                      // 充电状态
 *     .setRequiresDeviceIdle(true)                    // 设备空闲
 *     .setRequiresBatteryNotLow(true)                 // 电量充足
 *     .build()
 * 
 * // 使用场景：
 * // - 完整数据同步
 * // - 大文件传输
 * // - 系统更新
 * // - 深度数据清理
 * 
 * 【场景：渐进式约束策略】
 * 
 * // 基础同步：基本网络即可
 * val basicConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.CONNECTED)
 *     .build()
 * 
 * // 增强同步：需要 WiFi 和充电
 * val enhancedConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.UNMETERED)
 *     .setRequiresCharging(true)
 *     .build()
 * 
 * // 完整同步：最佳条件
 * val fullConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.UNMETERED)
 *     .setRequiresCharging(true)
 *     .setRequiresDeviceIdle(true)
 *     .setRequiresBatteryNotLow(true)
 *     .build()
 * 
 * ========================================
 * 5. 实际应用场景
 * ========================================
 * 
 * 【场景1：社交媒体应用】
 * 
 * // 照片上传：WiFi + 充电
 * val photoUploadConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.UNMETERED)
 *     .setRequiresCharging(true)
 *     .build()
 * 
 * // 消息同步：基本网络即可
 * val messageSyncConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.CONNECTED)
 *     .build()
 * 
 * 【场景2：文件管理应用】
 * 
 * // 大文件下载：WiFi + 电量充足
 * val downloadConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.UNMETERED)
 *     .setRequiresBatteryNotLow(true)
 *     .build()
 * 
 * // 文件压缩：充电 + 设备空闲
 * val compressConstraints = Constraints.Builder()
 *     .setRequiresCharging(true)
 *     .setRequiresDeviceIdle(true)
 *     .build()
 * 
 * 【场景3：数据备份应用】
 * 
 * // 增量备份：基本网络
 * val incrementalBackupConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.CONNECTED)
 *     .build()
 * 
 * // 完整备份：WiFi + 充电 + 空闲
 * val fullBackupConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.UNMETERED)
 *     .setRequiresCharging(true)
 *     .setRequiresDeviceIdle(true)
 *     .build()
 * 
 * ========================================
 * 6. 约束策略的优势
 * ========================================
 * 
 * 【1. 用户体验优化】
 * - 避免在用户使用设备时执行耗电任务
 * - 减少网络流量费用
 * - 提高设备响应性
 * 
 * 【2. 系统资源管理】
 * - 合理分配系统资源
 * - 避免资源冲突
 * - 优化任务执行时机
 * 
 * 【3. 成本控制】
 * - 避免在计费网络下执行大流量任务
 * - 减少不必要的电量消耗
 * - 优化存储空间使用
 * 
 * ========================================
 * 7. 约束组合的最佳实践
 * ========================================
 * 
 * 【高优先级任务（最小约束）】
 * 
 * // 关键同步：只需要网络
 * val criticalConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.CONNECTED)
 *     .build()
 * 
 * 【中优先级任务（适中约束）】
 * 
 * // 一般同步：WiFi + 电量
 * val normalConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.UNMETERED)
 *     .setRequiresBatteryNotLow(true)
 *     .build()
 * 
 * 【低优先级任务（严格约束）】
 * 
 * // 维护任务：最佳条件
 * val maintenanceConstraints = Constraints.Builder()
 *     .setRequiredNetworkType(NetworkType.UNMETERED)
 *     .setRequiresCharging(true)
 *     .setRequiresDeviceIdle(true)
 *     .setRequiresBatteryNotLow(true)
 *     .build()
 * 
 * ========================================
 * 【总结】
 * ========================================
 * 
 * 约束任务的使用场景非常广泛，主要用于根据设备状态、网络条件、资源状况等
 * 来智能调度任务，确保任务在最佳条件下执行，提升用户体验并优化系统资源使用。
 * 
 * 通过合理设置约束条件，可以实现：
 * 1. 智能任务调度
 * 2. 用户体验优化
 * 3. 系统资源管理
 * 4. 成本控制
 * 5. 任务优先级管理
 * 
 * 约束任务是构建智能、高效、用户友好的后台任务系统的关键组件。
 */
