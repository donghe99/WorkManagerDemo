package com.example.workmanagerdemo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*

/**
 * 任务标签与唯一名称演示页面
 * 
 * 演示内容：
 * 1. Tag（标签）的使用：分类、批量操作、状态监听
 * 2. WorkName（唯一名称）的使用：避免重复、精确控制
 * 3. 组合使用：既有唯一性又有分类管理
 * 
 * ========================================
 * 【Tag（标签）使用场景详解】
 * 
 * 1. 功能分类管理
 *    - 按业务模块分类：如 "sync"、"upload"、"cleanup"
 *    - 按优先级分类：如 "high"、"normal"、"low"
 *    - 按用户分类：如 "user_123"、"user_456"
 * 
 * 2. 批量操作场景
 *    - 批量取消：取消所有同步任务、取消所有上传任务
 *    - 批量查询：统计某类任务数量、获取某类任务状态
 *    - 批量监听：监听某类任务的整体进度
 * 
 * 3. 实际应用示例
 *    - 用户退出登录时：cancelAllWorkByTag("user_123") 取消该用户所有任务
 *    - 网络切换时：cancelAllWorkByTag("network_dependent") 取消所有依赖网络的任务
 *    - 应用进入后台时：cancelAllWorkByTag("foreground_only") 取消所有前台任务
 * 
 * ========================================
 * 【WorkName（唯一名称）使用场景详解】
 * 
 * 1. 避免重复执行
 *    - 数据同步：同一用户只允许一个同步任务运行
 *    - 配置更新：新配置任务替换旧配置任务
 *    - 资源清理：避免多个清理任务同时运行
 * 
 * 2. 策略选择
 *    - REPLACE：新任务替换旧任务，适合配置更新、数据刷新
 *    - KEEP：忽略新任务，适合确保任务不被中断
 *    - APPEND：追加到任务链后，适合扩展现有工作流
 * 
 * 3. 实际应用示例
 *    - 用户数据同步：workName = "user_sync_123"，确保同一用户不重复同步
 *    - 应用配置更新：workName = "config_update"，新配置替换旧配置
 *    - 数据库备份：workName = "db_backup"，避免多个备份任务冲突
 * 
 * ========================================
 * 【组合使用最佳实践】
 * 
 * 1. 既有唯一性又有分类管理
 *    - 唯一名称：确保任务不重复
 *    - 标签：便于批量操作和状态管理
 * 
 * 2. 典型组合场景
 *    - 用户任务：workName = "user_task_123"，tags = ["user", "task", "user_123"]
 *    - 系统任务：workName = "system_sync", tags = ["system", "sync", "background"]
 * 
 * 3. 管理策略
 *    - 精确控制：通过 workName 精确取消特定任务
 *    - 批量管理：通过 tag 批量操作同类任务
 *    - 灵活组合：根据业务需求选择合适的组合方式
 */
@Composable
fun TagWorkScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }
    var tagStatus by remember { mutableStateOf("标签任务状态: 未开始") }
    var uniqueStatus by remember { mutableStateOf("唯一任务状态: 未开始") }
    var combinedStatus by remember { mutableStateOf("组合任务状态: 未开始") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "任务标签与唯一名称演示", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // 1. 基础标签演示
        Text(text = "1. 基础标签使用", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<TagWorker>()
                .addTag("demoTag")
                .addTag("background")
                .build()
            workManager.enqueue(request)
            status = "已启动带标签任务: demoTag, background"
        }) {
            Text("启动带标签任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<TagWorker>()
                .addTag("demoTag")
                .addTag("sync")
                .build()
            workManager.enqueue(request)
            status = "已启动另一个带标签任务: demoTag, sync"
        }) {
            Text("启动另一个带标签任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // 2. 标签批量操作演示
        Text(text = "2. 标签批量操作", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            workManager.cancelAllWorkByTag("demoTag")
            status = "已取消所有带 demoTag 标签的任务"
        }) {
            Text("取消所有 demoTag 任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            workManager.cancelAllWorkByTag("background")
            status = "已取消所有 background 标签任务"
        }) {
            Text("取消所有 background 任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            workManager.cancelAllWorkByTag("sync")
            status = "已取消所有 sync 标签任务"
        }) {
            Text("取消所有 sync 任务")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 3. 标签状态监听演示
        Text(text = "3. 标签状态监听", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<TagWorker>()
                .addTag("observeTag")
                .build()
            workManager.enqueue(request)
            
            // 监听标签任务状态
            workManager.getWorkInfosByTagLiveData("observeTag").observeForever { workInfos ->
                val statusText = workInfos.joinToString { info -> 
                    "ID:${info.id.toString().take(8)} -> ${info.state.name}" 
                }
                tagStatus = "标签任务状态: $statusText"
            }
            status = "已启动并监听标签任务状态"
        }) {
            Text("启动并监听标签任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(text = tagStatus, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // 4. 唯一名称演示
        Text(text = "4. 唯一名称使用", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<TagWorker>().build()
            workManager.enqueueUniqueWork(
                "unique_sync_task",
                ExistingWorkPolicy.REPLACE,
                request
            )
            uniqueStatus = "唯一任务已入队: unique_sync_task"
        }) {
            Text("启动唯一任务 (REPLACE)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<TagWorker>().build()
            workManager.enqueueUniqueWork(
                "unique_sync_task",
                ExistingWorkPolicy.KEEP,
                request
            )
            uniqueStatus = "尝试启动同名唯一任务 (KEEP) - 如果已存在则忽略"
        }) {
            Text("启动同名唯一任务 (KEEP)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            workManager.cancelUniqueWork("unique_sync_task")
            uniqueStatus = "已取消唯一任务: unique_sync_task"
        }) {
            Text("取消唯一任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(text = uniqueStatus, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // 5. 组合使用演示
        Text(text = "5. 标签 + 唯一名称组合使用", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val request = OneTimeWorkRequestBuilder<TagWorker>()
                .addTag("user_sync")
                .addTag("user_123")
                .build()
            workManager.enqueueUniqueWork(
                "user_sync_123",
                ExistingWorkPolicy.REPLACE,
                request
            )
            combinedStatus = "组合任务已入队: 唯一名称 + 标签"
        }) {
            Text("启动组合任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            // 通过标签批量取消
            workManager.cancelAllWorkByTag("user_sync")
            combinedStatus = "已通过标签批量取消所有 user_sync 任务"
        }) {
            Text("通过标签批量取消")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            // 通过唯一名称精确取消
            workManager.cancelUniqueWork("user_sync_123")
            combinedStatus = "已通过唯一名称精确取消任务"
        }) {
            Text("通过唯一名称精确取消")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(text = combinedStatus, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // 6. 使用说明
        Text(text = "使用说明", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "• Tag（标签）：用于分类和批量操作，一个任务可有多个标签\n" +
                   "• WorkName（唯一名称）：确保任务唯一性，避免重复执行\n" +
                   "• 组合使用：既有唯一性控制，又有分类管理能力",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

class TagWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        val tags = tags.joinToString(", ")
        android.util.Log.d("TagWorker", "标签任务执行，标签: $tags")
        
        // 模拟工作
        try {
            Thread.sleep(2000) // 模拟2秒工作
        } catch (e: InterruptedException) {
            return Result.failure()
        }
        
        return Result.success()
    }
}
