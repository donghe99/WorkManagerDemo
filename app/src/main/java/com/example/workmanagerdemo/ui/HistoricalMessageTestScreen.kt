package com.example.workmanagerdemo.ui

import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import java.util.UUID
import kotlinx.coroutines.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 历史消息 + 兼容性融合测试页面
 * 
 * 将兼容性测试直接集成到历史消息测试流程中：
 * 1. 使用兼容性 WorkStatusManager 进行状态监听
 * 2. 在历史消息测试中验证 API 兼容性
 * 3. 一个流程测试两个特性
 * 
 * ========================================
 * 【历史消息机制详解】
 * ========================================
 * 
 * 历史消息是 WorkManager 的重要特性，用于在应用重启、进程重启等场景下
 * 恢复任务状态，确保状态的一致性和完整性。
 * 
 * ========================================
 * 1. 历史消息出现的详细场景
 * ========================================
 * 
 * 【应用生命周期变化场景】
 * - 应用进入后台 → 回到前台：通常不会收到历史消息
 *   * 原因：观察者连接未断开，LiveData 保持活跃状态
 *   * 状态无变化：WorkManager 不会重复发送相同状态
 *   * 缓存机制：LiveData 缓存最后一个值
 *   * 生命周期感知：Compose 组件生命周期管理
 * 
 * - 应用被系统杀死 → 重新启动：会收到历史消息
 *   * 原因：观察者被销毁，需要重新注册
 *   * 系统从数据库恢复任务状态
 *   * 重新注册观察者时发送历史状态
 * 
 * - 应用进程重启 → 重新注册观察者：会收到历史消息
 *   * 原因：进程级别的重启，所有观察者丢失
 *   * WorkManager 进程重新启动
 *   * 需要重新建立观察者连接
 * 
 * - 应用被强制停止 → 重新启动：会收到历史消息
 *   * 原因：强制停止会清理所有观察者
 *   * 应用完全重新初始化
 *   * 所有状态需要重新同步
 * 
 * 【系统级场景】
 * - 设备重启后应用启动：会收到历史消息
 *   * 原因：系统完全重启，所有进程重新创建
 *   * WorkManager 数据库从存储中恢复
 *   * 应用需要重新注册观察者
 * 
 * - 系统内存不足杀死应用：会收到历史消息
 *   * 原因：系统主动清理内存，杀死后台应用
 *   * 应用进程被强制终止
 *   * 重新启动时需要状态恢复
 * 
 * - 系统更新后重启：会收到历史消息
 *   * 原因：系统版本更新，所有应用重新初始化
 *   * 数据库结构可能发生变化
 *   * 需要重新建立观察者连接
 * 
 * - 电池优化杀死应用：会收到历史消息
 *   * 原因：系统电池优化策略
 *   * 应用被限制后台运行
 *   * 重新启动时需要状态同步
 * 
 * 【WorkManager 进程场景】
 * - WorkManager 进程被杀死：会收到历史消息
 *   * 原因：WorkManager 服务进程异常退出
 *   * 系统重新启动 WorkManager 服务
 *   * 需要重新加载任务状态
 * 
 * - WorkManager 服务重启：会收到历史消息
 *   * 原因：系统服务管理策略
 *   * 服务生命周期变化
 *   * 需要重新建立连接
 * 
 * - 数据库连接丢失：会收到历史消息
 *   * 原因：数据库文件损坏或权限问题
 *   * 系统重新初始化数据库
 *   * 需要重新加载任务信息
 * 
 * - 系统服务重启：会收到历史消息
 *   * 原因：系统级服务重启
 *   * 影响 WorkManager 运行环境
 *   * 需要重新建立服务连接
 * 
 * 【观察者管理场景】
 * - 观察者被销毁后重新创建：会收到历史消息
 *   * 原因：Compose 组件生命周期变化
 *   * LaunchedEffect 重新执行
 *   * 需要重新注册观察者
 * 
 * - LiveData 观察者重新注册：会收到历史消息
 *   * 原因：观察者被移除后重新添加
 *   * observe() 或 observeForever() 重新调用
 *   * 系统发送当前状态
 * 
 * - Flow 收集器重新启动：会收到历史消息
 *   * 原因：协程作用域重新创建
 *   * collect 操作重新执行
 *   * 需要重新建立数据流
 * 
 * - 手动断开并重新连接观察者：会收到历史消息
 *   * 原因：主动管理观察者生命周期
 *   * removeObserver() 后重新 observe()
 *   * 强制触发状态同步
 * 
 * ========================================
 * 2. 为什么应用回到前台时通常没有历史消息？
 * ========================================
 * 
 * 【主要原因分析】
 * 1. 观察者连接未断开
 *    - LiveData 保持活跃状态
 *    - 观察者仍然在监听任务状态
 *    - 没有重新注册的需求
 * 
 * 2. 状态无变化
 *    - WorkManager 不会重复发送相同状态
 *    - 任务状态没有发生新的变化
 *    - 避免不必要的状态通知
 * 
 * 3. 缓存机制
 *    - LiveData 缓存最后一个值
 *    - 观察者已经知道当前状态
 *    - 不需要重新发送状态信息
 * 
 * 4. 生命周期感知
 *    - Compose 组件生命周期管理
 *    - 组件没有完全销毁和重建
 *    - 观察者连接保持稳定
 * 
 * ========================================
 * 3. 如何强制触发历史消息？
 * ========================================
 * 
 * 【强制触发方法】
 * 1. 手动断开观察者
 *    - 调用 removeObserver() 方法
 *    - 主动清理观察者连接
 *    - 为重新注册做准备
 * 
 * 2. 重新创建观察者
 *    - 调用 observe() 或 observeForever()
 *    - 创建新的观察者实例
 *    - 系统自动发送当前状态
 * 
 * 3. 销毁并重建组件
 *    - 使用 Compose 的 LaunchedEffect
 *    - 组件完全重新创建
 *    - 观察者重新注册
 * 
 * 4. 模拟应用重启
 *    - 停止并重新开始监听
 *    - 模拟观察者断开重连
 *    - 测试历史消息机制
 * 
 * ========================================
 * 4. 实际应用场景示例
 * ========================================
 * 
 * 【场景1：文件下载应用】
 * 用户开始下载大文件 → 应用进入后台，下载继续 → 用户重新打开应用
 * → 收到历史消息：下载进度、完成状态等
 * 
 * 【场景2：数据同步应用】
 * 应用在后台同步数据 → 系统杀死应用进程 → 用户重新启动应用
 * → 收到历史消息：同步状态、错误信息等
 * 
 * 【场景3：消息推送应用】
 * 应用处理推送消息 → 设备重启 → 应用自动启动
 * → 收到历史消息：消息处理状态、失败重试等
 * 
 * 【场景4：游戏应用】
 * 游戏在后台更新资源 → 系统内存不足杀死应用 → 用户重新进入游戏
 * → 收到历史消息：更新进度、下载状态等
 * 
 * ========================================
 * 5. 历史消息机制原理
 * ========================================
 * 
 * 【数据持久化】
 * - WorkManager 使用 SQLite 数据库存储任务信息
 * - 任务状态、进度、结果等数据持久化保存
 * - 应用重启后从数据库恢复任务状态
 * - 支持任务链、约束条件等复杂信息
 * 
 * 【观察者注册机制】
 * - 首次注册观察者时，发送当前状态
 * - 观察者断开后重新连接，发送历史状态
 * - 系统自动管理观察者生命周期
 * - 支持多个观察者同时监听同一任务
 * 
 * 【状态同步策略】
 * - 避免重复发送相同状态
 * - 确保状态的一致性和完整性
 * - 支持增量状态更新
 * - 处理并发和竞态条件
 * 
 * 【性能优化】
 * - 延迟发送历史消息，避免阻塞UI
 * - 批量处理多个任务的状态更新
 * - 智能缓存和预加载机制
 * - 内存使用优化和垃圾回收
 * 
 * ========================================
 * 6. 最佳实践建议
 * ========================================
 * 
 * 【观察者管理】
 * - 在合适的生命周期中注册观察者
 * - 及时清理不需要的观察者
 * - 使用 Compose 的 LaunchedEffect 管理协程
 * - 避免内存泄漏和重复注册
 * 
 * 【状态处理】
 * - 处理所有可能的状态变化
 * - 实现状态去重和防抖机制
 * - 优雅处理错误和异常状态
 * - 提供用户友好的状态反馈
 * 
 * 【性能考虑】
 * - 避免在历史消息回调中执行耗时操作
 * - 使用适当的线程和调度器
 * - 实现智能的状态更新策略
 * - 监控和优化内存使用
 * 
 * 【用户体验】
 * - 提供清晰的状态指示器
 * - 实现平滑的状态转换动画
 * - 处理网络延迟和离线状态
 * - 支持用户手动刷新和重试
 * 
 * ========================================
 * 总结
 * ========================================
 * 
 * 历史消息机制是 WorkManager 的核心特性之一，通过合理的设计和实现，
 * 可以确保应用在各种异常情况下都能保持状态的一致性。关键是要理解
 * 历史消息出现的时机，正确管理观察者生命周期，并实现优雅的状态处理。
 */
@Composable
fun HistoricalMessageTestScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    val scope = rememberCoroutineScope()
    
    var taskId by remember { mutableStateOf<UUID?>(null) }
    var status by remember { mutableStateOf("未开始") }
    var isObserving by remember { mutableStateOf(false) }
    var workStatusManager by remember { mutableStateOf<CompatibleWorkStatusManager?>(null) }
    
    // 创建兼容性 WorkStatusManager
    LaunchedEffect(Unit) {
        workStatusManager = CompatibleWorkStatusManager(workManager)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "历史消息 + 兼容性融合测试", 
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // 系统版本信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "系统信息", 
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Android 版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                Text("推荐 API: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Flow API" else "LiveData API"}")
                Text("兼容性方案: 自动选择最合适的 API")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ==================== 融合测试区域 ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "融合测试：历史消息 + 兼容性", 
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 创建长时间运行的任务
                Button(onClick = {
                    val request = OneTimeWorkRequestBuilder<LongRunningWorker>()
                        .addTag("fusion_test")
                        .setInputData(Data.Builder()
                            .putString("test_type", "historical_message_compatibility_test")
                            .putLong("creation_time", System.currentTimeMillis())
                            .build())
                        .build()
                    
                    workManager.enqueue(request)
                    taskId = request.id
                    status = "任务已创建: ${request.id.toString().take(8)}..."
                    
                    Log.d("FusionTest", "创建融合测试任务: ${request.id}")
                }) {
                    Text("创建长时间运行的任务")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 开始兼容性状态监听（同时测试历史消息和兼容性）
                Button(
                    onClick = {
                        taskId?.let { id ->
                            workStatusManager?.observeWorkStatus(id) { state ->
                                when (state) {
                                    WorkInfo.State.ENQUEUED -> {
                                        Log.d("FusionTest", "任务已入队")
                                        status = "任务已入队"
                                    }
                                    WorkInfo.State.RUNNING -> {
                                        Log.d("FusionTest", "任务正在运行")
                                        status = "任务正在运行"
                                    }
                                    WorkInfo.State.SUCCEEDED -> {
                                        Log.d("FusionTest", "任务已完成")
                                        status = "任务已完成"
                                        
                                        // 获取任务结果
                                        scope.launch {
                                            val workInfo = workManager.getWorkInfoById(id).get()
                                            val outputData = workInfo?.outputData
                                            if (outputData != null) {
                                                val result = outputData.getString("result") ?: "无结果"
                                                val executionTime = outputData.getLong("execution_time", 0)
                                                status = "任务完成！结果: $result, 执行时间: ${executionTime}ms"
                                            }
                                        }
                                    }
                                    WorkInfo.State.FAILED -> {
                                        Log.d("FusionTest", "任务失败")
                                        status = "任务失败"
                                        
                                        // 获取错误信息
                                        scope.launch {
                                            val workInfo = workManager.getWorkInfoById(id).get()
                                            val errorData = workInfo?.outputData
                                            if (errorData != null) {
                                                val error = errorData.getString("error") ?: "未知错误"
                                                status = "任务失败！错误: $error"
                                            }
                                        }
                                    }
                                    WorkInfo.State.CANCELLED -> {
                                        Log.d("FusionTest", "任务已取消")
                                        status = "任务已取消"
                                    }
                                    WorkInfo.State.BLOCKED -> {
                                        Log.d("FusionTest", "任务被阻塞")
                                        status = "任务被阻塞"
                                    }
                                }
                            }
                            isObserving = true
                            
                            Log.d("FusionTest", "开始兼容性状态监听（同时测试历史消息）")
                        }
                    },
                    enabled = taskId != null && !isObserving
                ) {
                    Text("开始兼容性状态监听")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 停止监听（模拟应用进入后台）
                Button(
                    onClick = {
                        taskId?.let { id ->
                            workStatusManager?.stopObserving(id)
                            isObserving = false
                            status = "监听已停止（模拟应用进入后台）"
                            
                            Log.d("FusionTest", "停止监听任务状态")
                        }
                    },
                    enabled = isObserving
                ) {
                    Text("停止监听（模拟后台）")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 重新开始监听（模拟应用回到前台，测试历史消息）
                Button(
                    onClick = {
                        taskId?.let { id ->
                            workStatusManager?.observeWorkStatus(id) { state ->
                                Log.d("FusionTest", "重新监听 - 状态: $state")
                                status = "重新监听 - 状态: $state"
                                
                                // 检查是否收到历史消息
                                if (state == WorkInfo.State.SUCCEEDED) {
                                    Log.d("FusionTest", "收到历史消息：任务已完成")
                                    status = "收到历史消息：任务已完成"
                                }
                            }
                            isObserving = true
                            
                            Log.d("FusionTest", "重新开始监听（测试历史消息）")
                        }
                    },
                    enabled = taskId != null && !isObserving
                ) {
                    Text("重新开始监听（测试历史消息）")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 取消任务
                Button(
                    onClick = {
                        taskId?.let { id ->
                            workManager.cancelWorkById(id)
                            status = "任务已取消"
                            taskId = null
                            isObserving = false
                            
                            Log.d("FusionTest", "任务已取消")
                        }
                    },
                    enabled = taskId != null
                ) {
                    Text("取消任务")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 状态显示
                Text("任务状态: $status")
                Text("监听状态: ${if (isObserving) "正在监听" else "未监听"}")
                
                taskId?.let { id ->
                    Text("任务ID: ${id.toString().take(8)}...")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ==================== 测试说明区域 ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "融合测试说明", 
                    style = MaterialTheme.typography.titleMedium
                )
                Text("• 兼容性测试：自动选择 Flow 或 LiveData API")
                Text("• 历史消息测试：验证应用重启后的状态恢复")
                Text("• 两个特性在一个流程中同时测试")
                Text("• 观察日志中的 API 类型和历史消息")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 测试步骤
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "测试步骤", 
                    style = MaterialTheme.typography.titleMedium
                )
                Text("1. 创建长时间运行的任务（约10秒）")
                Text("2. 开始兼容性状态监听")
                Text("3. 观察使用的 API 类型（Flow 或 LiveData）")
                Text("4. 停止监听（模拟应用进入后台）")
                Text("5. 等待任务完成")
                Text("6. 重新开始监听（模拟应用回到前台）")
                Text("7. 查看是否收到历史消息")
                Text("8. 检查日志中的兼容性和历史消息信息")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 预期结果
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "预期结果", 
                    style = MaterialTheme.typography.titleMedium
                )
                Text("兼容性测试：")
                Text("• Android 12+: 日志显示 '使用 Flow API'")
                Text("• Android 11 及以下: 日志显示 '使用 LiveData API'")
                Text("• 如果 Flow API 失败，自动降级到 LiveData API")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("历史消息测试：")
                Text("• 重新监听后立即收到任务完成状态")
                Text("• 日志显示 '收到历史消息：任务已完成'")
                Text("• 验证状态恢复的一致性")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ==================== 历史消息详细场景说明 ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "历史消息出现的详细场景", 
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "1. 应用生命周期变化场景", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 应用进入后台 → 回到前台：通常不会收到历史消息")
                Text("• 应用被系统杀死 → 重新启动：会收到历史消息")
                Text("• 应用进程重启 → 重新注册观察者：会收到历史消息")
                Text("• 应用被强制停止 → 重新启动：会收到历史消息")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "2. 系统级场景", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 设备重启后应用启动：会收到历史消息")
                Text("• 系统内存不足杀死应用：会收到历史消息")
                Text("• 系统更新后重启：会收到历史消息")
                Text("• 电池优化杀死应用：会收到历史消息")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "3. WorkManager 进程场景", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• WorkManager 进程被杀死：会收到历史消息")
                Text("• WorkManager 服务重启：会收到历史消息")
                Text("• 数据库连接丢失：会收到历史消息")
                Text("• 系统服务重启：会收到历史消息")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "4. 观察者管理场景", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 观察者被销毁后重新创建：会收到历史消息")
                Text("• LiveData 观察者重新注册：会收到历史消息")
                Text("• Flow 收集器重新启动：会收到历史消息")
                Text("• 手动断开并重新连接观察者：会收到历史消息")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "5. 为什么应用回到前台时通常没有历史消息？", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 观察者连接未断开：LiveData 保持活跃状态")
                Text("• 状态无变化：WorkManager 不会重复发送相同状态")
                Text("• 缓存机制：LiveData 缓存最后一个值")
                Text("• 生命周期感知：Compose 组件生命周期管理")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "6. 如何强制触发历史消息？", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 手动断开观察者：removeObserver()")
                Text("• 重新创建观察者：observe() 或 observeForever()")
                Text("• 销毁并重建组件：Compose 组件的 LaunchedEffect")
                Text("• 模拟应用重启：停止并重新开始监听")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ==================== 实际应用场景示例 ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inversePrimary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "实际应用场景示例", 
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "场景1：文件下载应用", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 用户开始下载大文件")
                Text("• 应用进入后台，下载继续")
                Text("• 用户重新打开应用")
                Text("• 收到历史消息：下载进度、完成状态等")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "场景2：数据同步应用", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 应用在后台同步数据")
                Text("• 系统杀死应用进程")
                Text("• 用户重新启动应用")
                Text("• 收到历史消息：同步状态、错误信息等")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "场景3：消息推送应用", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 应用处理推送消息")
                Text("• 设备重启")
                Text("• 应用自动启动")
                Text("• 收到历史消息：消息处理状态、失败重试等")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "场景4：游戏应用", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 游戏在后台更新资源")
                Text("• 系统内存不足杀死应用")
                Text("• 用户重新进入游戏")
                Text("• 收到历史消息：更新进度、下载状态等")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ==================== 历史消息机制原理 ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "历史消息机制原理", 
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "1. 数据持久化", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• WorkManager 使用 SQLite 数据库存储任务信息")
                Text("• 任务状态、进度、结果等数据持久化保存")
                Text("• 应用重启后从数据库恢复任务状态")
                Text("• 支持任务链、约束条件等复杂信息")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "2. 观察者注册机制", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 首次注册观察者时，发送当前状态")
                Text("• 观察者断开后重新连接，发送历史状态")
                Text("• 系统自动管理观察者生命周期")
                Text("• 支持多个观察者同时监听同一任务")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "3. 状态同步策略", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 避免重复发送相同状态")
                Text("• 确保状态的一致性和完整性")
                Text("• 支持增量状态更新")
                Text("• 处理并发和竞态条件")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "4. 性能优化", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 延迟发送历史消息，避免阻塞UI")
                Text("• 批量处理多个任务的状态更新")
                Text("• 智能缓存和预加载机制")
                Text("• 内存使用优化和垃圾回收")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ==================== 最佳实践建议 ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "最佳实践建议", 
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "1. 观察者管理", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 在合适的生命周期中注册观察者")
                Text("• 及时清理不需要的观察者")
                Text("• 使用 Compose 的 LaunchedEffect 管理协程")
                Text("• 避免内存泄漏和重复注册")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "2. 状态处理", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 处理所有可能的状态变化")
                Text("• 实现状态去重和防抖机制")
                Text("• 优雅处理错误和异常状态")
                Text("• 提供用户友好的状态反馈")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "3. 性能考虑", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 避免在历史消息回调中执行耗时操作")
                Text("• 使用适当的线程和调度器")
                Text("• 实现智能的状态更新策略")
                Text("• 监控和优化内存使用")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "4. 用户体验", 
                    style = MaterialTheme.typography.titleSmall
                )
                Text("• 提供清晰的状态指示器")
                Text("• 实现平滑的状态转换动画")
                Text("• 处理网络延迟和离线状态")
                Text("• 支持用户手动刷新和重试")
            }
        }
    }
}

/**
 * 兼容性 WorkStatusManager
 * 根据 Android 版本自动选择最合适的 API
 */
class CompatibleWorkStatusManager(
    private val workManager: WorkManager
) {
    
    /**
     * 观察任务状态变化
     * @param taskId 任务ID
     * @param onStateChange 状态变化回调
     */
    fun observeWorkStatus(taskId: UUID, onStateChange: (WorkInfo.State) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 使用 Flow API
            observeWithFlow(taskId, onStateChange)
        } else {
            // Android 11 及以下使用 LiveData API
            observeWithLiveData(taskId, onStateChange)
        }
    }
    
    /**
     * 使用 Flow API 观察任务状态（Android 12+）
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private fun observeWithFlow(taskId: UUID, onStateChange: (WorkInfo.State) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("CompatibleWorkStatusManager", "使用 Flow API 观察任务状态")
                workManager.getWorkInfoByIdFlow(taskId)
                    .distinctUntilChanged { old, new -> old!!.state == new!!.state }
                    .catch { error -> 
                        Log.e("CompatibleWorkStatusManager", "Flow 观察错误", error)
                        // 错误处理：记录日志，不发射新值
                        // 如果需要，可以调用回调函数处理错误
                    }
                    .collect { workInfo -> 
                        Log.d("CompatibleWorkStatusManager", "Flow 状态变化: ${workInfo!!.state}")
                        onStateChange(workInfo!!.state)
                    }
            } catch (e: Exception) {
                Log.e("CompatibleWorkStatusManager", "Flow API 调用失败", e)
                // 降级到 LiveData API
                Log.d("CompatibleWorkStatusManager", "降级到 LiveData API")
                observeWithLiveData(taskId, onStateChange)
            }
        }
    }
    
    /**
     * 使用 LiveData API 观察任务状态（Android 11 及以下）
     */
    private fun observeWithLiveData(taskId: UUID, onStateChange: (WorkInfo.State) -> Unit) {
        Log.d("CompatibleWorkStatusManager", "使用 LiveData API 观察任务状态")
        var lastState: WorkInfo.State? = null
        var lastUpdateTime = 0L
        
        workManager.getWorkInfoByIdLiveData(taskId).observeForever { workInfo ->
            val currentState = workInfo?.state
            val currentTime = System.currentTimeMillis()
            
            // 状态去重 + 时间戳防抖（1秒间隔）
            if (currentState != null && 
                currentState != lastState && 
                currentTime - lastUpdateTime > 1000) {
                
                lastState = currentState
                lastUpdateTime = currentTime
                
                Log.d("CompatibleWorkStatusManager", "LiveData 状态变化: $currentState")
                onStateChange(currentState)
            }
        }
    }
    
    /**
     * 停止观察任务状态
     * @param taskId 任务ID
     */
    fun stopObserving(taskId: UUID) {
        // LiveData 的 observeForever 需要手动移除观察者
        // 这里简化处理，实际使用时需要保存观察者引用
        Log.d("CompatibleWorkStatusManager", "停止观察任务: $taskId")
    }
}

/**
 * 长时间运行的测试 Worker
 * 用于模拟需要较长时间完成的任务
 */
class LongRunningWorker(
    context: android.content.Context, 
    params: androidx.work.WorkerParameters
) : Worker(context, params) {
    
    override fun doWork(): Result {
        val testType = inputData.getString("test_type") ?: "unknown"
        val creationTime = inputData.getLong("creation_time", 0)
        
        Log.d("LongRunningWorker", "开始长时间运行的任务")
        Log.d("LongRunningWorker", "测试类型: $testType")
        Log.d("LongRunningWorker", "创建时间: $creationTime")
        
        try {
            // 模拟长时间工作过程
            val startTime = System.currentTimeMillis()
            
            // 分阶段工作，便于观察状态变化
            Log.d("LongRunningWorker", "阶段1: 数据准备")
            Thread.sleep(2000)
            
            Log.d("LongRunningWorker", "阶段2: 数据处理")
            Thread.sleep(3000)
            
            Log.d("LongRunningWorker", "阶段3: 结果验证")
            Thread.sleep(2000)
            
            Log.d("LongRunningWorker", "阶段4: 最终完成")
            Thread.sleep(3000)
            
            val totalTime = System.currentTimeMillis() - startTime
            
            Log.d("LongRunningWorker", "任务执行成功，总耗时: ${totalTime}ms")
            
            // 返回成功结果
            val output = Data.Builder()
                .putString("result", "长时间任务成功完成")
                .putLong("execution_time", totalTime)
                .putString("test_type", testType)
                .putLong("completion_time", System.currentTimeMillis())
                .build()
            
            return Result.success(output)
            
        } catch (e: Exception) {
            Log.e("LongRunningWorker", "任务执行失败", e)
            
            // 返回错误信息
            val errorOutput = Data.Builder()
                .putString("error", e.message ?: "未知错误")
                .putString("error_type", e.javaClass.simpleName)
                .putLong("error_time", System.currentTimeMillis())
                .build()
            
            return Result.failure(errorOutput)
        }
    }
}
