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

/**
 * 兼容性测试页面
 * 
 * 演示 WorkStatusManager 在不同 Android 版本上的兼容性实现
 * - Android 12+ (API 31+): 使用 Flow API
 * - Android 11 及以下: 使用 LiveData API
 */
@Composable
fun CompatibilityTestScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    val scope = rememberCoroutineScope()
    
    var status by remember { mutableStateOf("未开始") }
    var currentTaskId by remember { mutableStateOf<UUID?>(null) }
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
            text = "WorkStatusManager 兼容性测试", 
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
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 创建测试任务
        Button(
            onClick = {
                val testWorker = OneTimeWorkRequestBuilder<TestCompatibilityWorker>()
                    .addTag("compatibility_test")
                    .setInputData(Data.Builder()
                        .putString("test_data", "兼容性测试数据")
                        .putLong("creation_time", System.currentTimeMillis())
                        .build())
                    .build()
                
                workManager.enqueue(testWorker)
                currentTaskId = testWorker.id
                status = "测试任务已创建，ID: ${testWorker.id.toString().take(8)}..."
                
                Log.d("CompatibilityTest", "创建测试任务: ${testWorker.id}")
            }
        ) {
            Text("创建测试任务")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 开始监听任务状态
        Button(
            onClick = {
                currentTaskId?.let { taskId ->
                    workStatusManager?.observeWorkStatus(taskId) { state ->
                        when (state) {
                            WorkInfo.State.ENQUEUED -> {
                                Log.d("WorkStatus", "任务已入队")
                                status = "任务已入队"
                            }
                            WorkInfo.State.RUNNING -> {
                                Log.d("WorkStatus", "任务正在运行")
                                status = "任务正在运行"
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                Log.d("WorkStatus", "任务已完成")
                                status = "任务已完成"
                                
                                // 获取任务结果
                                scope.launch {
                                    val workInfo = workManager.getWorkInfoById(taskId).get()
                                    val outputData = workInfo?.outputData
                                    if (outputData != null) {
                                        val result = outputData.getString("result") ?: "无结果"
                                        val executionTime = outputData.getLong("execution_time", 0)
                                        status = "任务完成！结果: $result, 执行时间: ${executionTime}ms"
                                    }
                                }
                            }
                            WorkInfo.State.FAILED -> {
                                Log.d("WorkStatus", "任务失败")
                                status = "任务失败"
                                
                                // 获取错误信息
                                scope.launch {
                                    val workInfo = workManager.getWorkInfoById(taskId).get()
                                    val errorData = workInfo?.outputData
                                    if (errorData != null) {
                                        val error = errorData.getString("error") ?: "未知错误"
                                        status = "任务失败！错误: $error"
                                    }
                                }
                            }
                            WorkInfo.State.CANCELLED -> {
                                Log.d("WorkStatus", "任务已取消")
                                status = "任务已取消"
                            }
                        }
                    }
                } ?: run {
                    status = "请先创建测试任务"
                }
            },
            enabled = currentTaskId != null
        ) {
            Text("开始监听任务状态")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 取消任务
        Button(
            onClick = {
                currentTaskId?.let { taskId ->
                    workManager.cancelWorkById(taskId)
                    status = "任务已取消"
                    currentTaskId = null
                }
            },
            enabled = currentTaskId != null
        ) {
            Text("取消任务")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 状态显示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "当前状态", 
                    style = MaterialTheme.typography.titleMedium
                )
                Text(status)
                
                currentTaskId?.let { taskId ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("任务ID: ${taskId.toString().take(8)}...")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 兼容性说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "兼容性说明", 
                    style = MaterialTheme.typography.titleMedium
                )
                Text("• Android 12+ (API 31+): 使用 Flow API，支持 distinctUntilChanged")
                Text("• Android 11 及以下: 使用 LiveData API，手动实现去重")
                Text("• 自动检测系统版本，选择最合适的 API")
                Text("• 避免 NoSuchMethodError 等兼容性问题")
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
                workManager.getWorkInfoByIdFlow(taskId)
                    .distinctUntilChanged { old, new -> old.state == new.state }
                    .catch { error -> 
                        Log.e("CompatibleWorkStatusManager", "Flow 观察错误", error)
                        // 发射错误状态
                        emit(WorkInfo.createForId(taskId, WorkInfo.State.FAILED))
                    }
                    .collect { workInfo -> 
                        Log.d("CompatibleWorkStatusManager", "Flow 状态变化: ${workInfo.state}")
                        onStateChange(workInfo.state)
                    }
            } catch (e: Exception) {
                Log.e("CompatibleWorkStatusManager", "Flow API 调用失败", e)
                // 降级到 LiveData API
                observeWithLiveData(taskId, onStateChange)
            }
        }
    }
    
    /**
     * 使用 LiveData API 观察任务状态（Android 11 及以下）
     */
    private fun observeWithLiveData(taskId: UUID, onStateChange: (WorkInfo.State) -> Unit) {
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
 * 测试兼容性的 Worker
 */
class TestCompatibilityWorker(
    context: android.content.Context, 
    params: androidx.work.WorkerParameters
) : Worker(context, params) {
    
    override fun doWork(): Result {
        val testData = inputData.getString("test_data") ?: "默认测试数据"
        val creationTime = inputData.getLong("creation_time", 0)
        
        Log.d("TestCompatibilityWorker", "开始执行兼容性测试任务")
        Log.d("TestCompatibilityWorker", "输入数据: $testData")
        Log.d("TestCompatibilityWorker", "创建时间: $creationTime")
        
        try {
            // 模拟工作过程
            val startTime = System.currentTimeMillis()
            
            // 模拟一些工作
            Thread.sleep(2000) // 2秒工作
            
            val executionTime = System.currentTimeMillis() - startTime
            
            // 返回成功结果
            val output = Data.Builder()
                .putString("result", "兼容性测试成功完成")
                .putLong("execution_time", executionTime)
                .putString("input_data", testData)
                .putLong("completion_time", System.currentTimeMillis())
                .build()
            
            Log.d("TestCompatibilityWorker", "任务执行成功，耗时: ${executionTime}ms")
            return Result.success(output)
            
        } catch (e: Exception) {
            Log.e("TestCompatibilityWorker", "任务执行失败", e)
            
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
