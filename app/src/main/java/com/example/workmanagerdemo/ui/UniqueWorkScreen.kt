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
 * 唯一任务链（beginUniqueWork）演示页面
 *
 * 主要功能与说明：
 * 1. 通过唯一名称（workName）创建任务链，保证同类型任务只保留一个，避免重复执行。
 * 2. 可选择不同策略：
 *    - REPLACE：新任务链替换旧任务链。
 *    - KEEP：已有任务链未完成时忽略新任务链。
 *    - APPEND：新任务链追加到旧任务链后（仅限任务链）。
 *    - APPEND_OR_REPLACE：已有为链则追加，否则替换（Android 12+）。
 * 3. 适合如同步、上传等只需执行一次或顺序追加的场景。
 * 4. 可通过 cancelUniqueWork(workName) 取消同名任务链。
 * 5. 支持监听所有节点状态。
 * 
 * ========================================
 * 【APPEND 策略详解】
 * 
 * APPEND 策略的核心特点：
 * 1. 仅适用于任务链（WorkContinuation），不能追加到单个任务
 * 2. 新任务链会追加到现有任务链的末尾
 * 3. 如果现有任务不是任务链，则无法追加（会失败）
 * 4. 适合扩展现有工作流的场景
 * 
 * 使用场景：
 * - 文件处理流程：下载 → 解压 → 处理 → 上传
 * - 数据同步流程：本地同步 → 云端同步 → 状态更新
 * - 工作流扩展：基础流程 + 可选步骤
 */

@Composable
fun UniqueWorkScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    var status by remember { mutableStateOf("未开始") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "唯一任务链演示 (beginUniqueWork)", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // REPLACE 策略演示
        Button(onClick = {
            val workA = OneTimeWorkRequestBuilder<UniqueWorker>().build()
            workManager.beginUniqueWork("unique_demo", ExistingWorkPolicy.REPLACE, workA).enqueue()
            status = "REPLACE策略：新任务链已替换旧任务链"
        }) {
            Text("REPLACE策略：替换旧任务链")
        }
        Spacer(modifier = Modifier.height(8.dp))

        // KEEP 策略演示
        Button(onClick = {
            val workA = OneTimeWorkRequestBuilder<UniqueWorker>().build()
            workManager.beginUniqueWork("unique_demo", ExistingWorkPolicy.KEEP, workA).enqueue()
            status = "KEEP策略：已有任务链未完成则忽略新任务链"
        }) {
            Text("KEEP策略：忽略新任务链")
        }
        Spacer(modifier = Modifier.height(8.dp))

        // APPEND 策略演示（追加任务链）
        Button(onClick = {
            val workA = OneTimeWorkRequestBuilder<UniqueWorker>().build()
            val workB = OneTimeWorkRequestBuilder<UniqueWorker>().build()
            workManager.beginUniqueWork("unique_demo", ExistingWorkPolicy.APPEND, listOf(workA, workB)).enqueue()
            status = "APPEND策略：新任务链已追加到旧任务链后"
        }) {
            Text("APPEND策略：追加任务链")
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 详细的 APPEND 策略演示
        Text(text = "APPEND 策略详细演示", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        // 步骤1：创建基础任务链
        Button(onClick = {
            val download = OneTimeWorkRequestBuilder<UniqueDownloadWorker>().build()
            val unzip = OneTimeWorkRequestBuilder<UniqueUnzipWorker>().build()
            
            // 创建基础任务链：下载 → 解压
            workManager.beginUniqueWork(
                "file_processing", 
                ExistingWorkPolicy.REPLACE, 
                download
            ).then(unzip).enqueue()
            
            status = "步骤1：创建基础任务链（下载→解压）"
        }) {
            Text("步骤1：创建基础任务链")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // 步骤2：追加处理任务
        Button(onClick = {
            val process = OneTimeWorkRequestBuilder<UniqueProcessWorker>().build()
            
            // 追加处理任务到现有任务链
            workManager.beginUniqueWork(
                "file_processing", 
                ExistingWorkPolicy.APPEND, 
                process
            ).enqueue()
            
            status = "步骤2：追加处理任务到现有任务链"
        }) {
            Text("步骤2：追加处理任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // 步骤3：追加上传任务
        Button(onClick = {
            val upload = OneTimeWorkRequestBuilder<UniqueUploadWorker>().build()
            
            // 再次追加上传任务
            workManager.beginUniqueWork(
                "file_processing", 
                ExistingWorkPolicy.APPEND, 
                upload
            ).enqueue()
            
            status = "步骤3：追加上传任务，最终链：下载→解压→处理→上传"
        }) {
            Text("步骤3：追加上传任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // 演示 APPEND 失败的情况（追加到单个任务）
        Text(text = "APPEND 策略限制演示：只能追加到任务链，不能追加到单个任务", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val singleTask = OneTimeWorkRequestBuilder<UniqueWorker>().build()
            
            // 先创建一个单个任务（不是任务链）
            workManager.beginUniqueWork(
                "single_task_demo", 
                ExistingWorkPolicy.REPLACE, 
                singleTask
            ).enqueue()
            
            status = "步骤1：创建单个任务（不是任务链）"
        }) {
            Text("步骤1：创建单个任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val appendTask = OneTimeWorkRequestBuilder<UniqueWorker>().build()
            
            // 尝试追加到单个任务（会失败）
            // 尝试 APPEND 操作到单个任务（会失败）
            workManager.beginUniqueWork(
                "single_task_demo", 
                ExistingWorkPolicy.APPEND, 
                appendTask
            ).enqueue()
            
            // 由于 APPEND 到单个任务会静默失败，我们直接显示结果
            status = "步骤2：APPEND 操作已执行，但由于目标不是任务链，操作被忽略"
        }) {
            Text("步骤2：尝试追加到单个任务（会失败）")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // 对比：创建任务链然后追加（会成功）
        Button(onClick = {
            val chainTask1 = OneTimeWorkRequestBuilder<UniqueWorker>().build()
            val chainTask2 = OneTimeWorkRequestBuilder<UniqueWorker>().build()
            
            // 创建任务链：任务1 → 任务2
            workManager.beginUniqueWork(
                "chain_demo", 
                ExistingWorkPolicy.REPLACE, 
                chainTask1
            ).then(chainTask2).enqueue()
            
            status = "步骤1：创建任务链（任务1→任务2）"
        }) {
            Text("步骤1：创建任务链")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = {
            val appendTask = OneTimeWorkRequestBuilder<UniqueWorker>().build()
            
            // 追加到任务链（会成功）
            workManager.beginUniqueWork(
                "chain_demo", 
                ExistingWorkPolicy.APPEND, 
                appendTask
            ).enqueue()
            
            status = "步骤2：成功追加到任务链！最终链：任务1→任务2→追加任务"
        }) {
            Text("步骤2：追加到任务链（会成功）")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 取消同名任务链
        Button(onClick = {
            workManager.cancelUniqueWork("unique_demo")
            status = "已取消所有 unique_demo 任务链"
        }) {
            Text("取消同名任务链")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 状态监听
        Button(onClick = {
            workManager.getWorkInfosForUniqueWorkLiveData("unique_demo").observeForever {
                status = "unique_demo链所有节点状态: " + it.joinToString { info -> info.state.name }
            }
        }) {
            Text("监听所有节点状态")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        // APPEND 策略使用说明
        Text(text = "APPEND 策略使用说明", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "APPEND 策略核心要点：\n" +
                   "1. 只能追加到任务链，不能追加到单个任务\n" +
                   "2. 新任务会追加到现有任务链的末尾\n" +
                   "3. 适合动态扩展工作流的场景\n" +
                   "4. 如果现有任务不是任务链，APPEND 会失败\n\n" +
                   "典型使用场景：\n" +
                   "• 文件处理流程：下载 → 解压 → 处理 → 上传\n" +
                   "• 数据同步流程：本地同步 → 云端同步 → 状态更新\n" +
                   "• 工作流扩展：基础流程 + 可选步骤",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // 代码示例说明
        Text(text = "代码示例说明", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "步骤1：创建基础任务链\n" +
                   "workManager.beginUniqueWork(\"file_processing\", ExistingWorkPolicy.REPLACE, download).then(unzip).enqueue()\n\n" +
                   "步骤2：追加处理任务\n" +
                   "workManager.beginUniqueWork(\"file_processing\", ExistingWorkPolicy.APPEND, process).enqueue()\n\n" +
                   "步骤3：追加上传任务\n" +
                   "workManager.beginUniqueWork(\"file_processing\", ExistingWorkPolicy.APPEND, upload).enqueue()\n\n" +
                   "最终执行顺序：下载 → 解压 → 处理 → 上传",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(200.dp))
    }
}

class UniqueWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("UniqueWorker", "唯一任务链节点执行")
        return Result.success()
    }
}

/**
 * 下载任务 Worker（用于唯一任务链演示）
 */
class UniqueDownloadWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("UniqueDownloadWorker", "下载任务执行")
        // 模拟下载过程
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            return Result.failure()
        }
        return Result.success()
    }
}

/**
 * 解压任务 Worker（用于唯一任务链演示）
 */
class UniqueUnzipWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("UniqueUnzipWorker", "解压任务执行")
        // 模拟解压过程
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            return Result.failure()
        }
        return Result.success()
    }
}

/**
 * 处理任务 Worker（用于唯一任务链演示）
 */
class UniqueProcessWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("UniqueProcessWorker", "处理任务执行")
        // 模拟处理过程
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            return Result.failure()
        }
        return Result.success()
    }
}

/**
 * 上传任务 Worker（用于唯一任务链演示）
 */
class UniqueUploadWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(ctx, params) {
    override fun doWork(): Result {
        android.util.Log.d("UniqueUploadWorker", "上传任务执行")
        // 模拟上传过程
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            return Result.failure()
        }
        return Result.success()
    }
}

/**
 * ========================================
 * 【APPEND 策略详细使用场景与代码示例】
 * ========================================
 * 
 * APPEND 策略的核心价值在于能够动态扩展现有工作流，以下是具体的使用场景：
 * 
 * ========================================
 * 1. 动态扩展工作流场景
 * ========================================
 * 
 * 【场景：文件处理流程的渐进式构建】
 * 
 * // 用户先选择基础处理
 * workManager.beginUniqueWork(
 *     "file_processing", 
 *     ExistingWorkPolicy.REPLACE, 
 *     downloadTask
 * ).then(unzipTask).enqueue()
 * 
 * // 用户后来决定添加处理步骤
 * workManager.beginUniqueWork(
 *     "file_processing", 
 *     ExistingWorkPolicy.APPEND, 
 *     processTask
 * ).enqueue()
 * 
 * // 用户最后决定添加上传步骤
 * workManager.beginUniqueWork(
 *     "file_processing", 
 *     ExistingWorkPolicy.APPEND, 
 *     uploadTask
 * ).enqueue()
 * 
 * // 最终执行顺序：下载 → 解压 → 处理 → 上传
 * 
 * ========================================
 * 2. 用户交互驱动的任务扩展
 * ========================================
 * 
 * 【场景：用户逐步选择功能】
 * 
 * // 基础同步流程
 * workManager.beginUniqueWork(
 *     "data_sync", 
 *     ExistingWorkPolicy.REPLACE, 
 *     localSyncTask
 * ).then(cloudSyncTask).enqueue()
 * 
 * // 用户选择添加数据验证
 * if (userWantsValidation) {
 *     workManager.beginUniqueWork(
 *         "data_sync", 
 *         ExistingWorkPolicy.APPEND, 
 *         validationTask
 *     ).enqueue()
 * }
 * 
 * // 用户选择添加通知
 * if (userWantsNotification) {
 *     workManager.beginUniqueWork(
 *         "data_sync", 
 *         ExistingWorkPolicy.APPEND, 
 *         notificationTask
 *     ).enqueue()
 * }
 * 
 * // 最终流程：本地同步 → 云端同步 → 数据验证 → 通知用户
 * 
 * ========================================
 * 3. 条件性任务添加
 * ========================================
 * 
 * 【场景：根据运行时条件添加任务】
 * 
 * // 基础备份流程
 * workManager.beginUniqueWork(
 *     "backup_process", 
 *     ExistingWorkPolicy.REPLACE, 
 *     backupTask
 * ).then(compressTask).enqueue()
 * 
 * // 根据存储空间决定是否加密
 * if (availableStorage < requiredSpace) {
 *     workManager.beginUniqueWork(
 *         "backup_process", 
 *         ExistingWorkPolicy.APPEND, 
 *         encryptTask
 *     ).enqueue()
 * }
 * 
 * // 根据网络状态决定是否上传
 * if (isNetworkAvailable) {
 *     workManager.beginUniqueWork(
 *         "backup_process", 
 *         ExistingWorkPolicy.APPEND, 
 *         uploadTask
 *     ).enqueue()
 * }
 * 
 * ========================================
 * 4. 插件式架构
 * ========================================
 * 
 * 【场景：模块化功能扩展】
 * 
 * // 核心处理流程
 * workManager.beginUniqueWork(
 *     "core_processing", 
 *     ExistingWorkPolicy.REPLACE, 
 *     coreTask
 * ).enqueue()
 * 
 * // 动态加载插件功能
 * plugins.forEach { plugin ->
 *     if (plugin.isEnabled) {
 *         workManager.beginUniqueWork(
 *             "core_processing", 
 *             ExistingWorkPolicy.APPEND, 
 *             plugin.createTask()
 *         ).enqueue()
 *     }
 * }
 * 
 * ========================================
 * 5. 实时任务调度
 * ========================================
 * 
 * 【场景：根据用户操作实时添加任务】
 * 
 * // 用户开始编辑文档
 * workManager.beginUniqueWork(
 *     "document_workflow", 
 *     ExistingWorkPolicy.REPLACE, 
 *     openDocumentTask
 * ).enqueue()
 * 
 * // 用户保存文档
 * workManager.beginUniqueWork(
 *     "document_workflow", 
 *     ExistingWorkPolicy.APPEND, 
 *     saveDocumentTask
 * ).enqueue()
 * 
 * // 用户选择导出
 * workManager.beginUniqueWork(
 *     "document_workflow", 
 *     ExistingWorkPolicy.APPEND, 
 *     exportTask
 * ).enqueue()
 * 
 * // 用户选择分享
 * workManager.beginUniqueWork(
 *     "document_workflow", 
 *     ExistingWorkPolicy.APPEND, 
 *     shareTask
 * ).enqueue()
 * 
 * ========================================
 * 6. 渐进式数据同步
 * ========================================
 * 
 * 【场景：分阶段同步不同类型的数据】
 * 
 * // 第一阶段：基础数据同步
 * workManager.beginUniqueWork(
 *     "progressive_sync", 
 *     ExistingWorkPolicy.REPLACE, 
 *     userProfileSync
 * ).then(settingsSync).enqueue()
 * 
 * // 第二阶段：内容数据同步（用户选择）
 * if (userWantsContentSync) {
 *     workManager.beginUniqueWork(
 *         "progressive_sync", 
 *         ExistingWorkPolicy.APPEND, 
 *         contentSync
 *     ).enqueue()
 * }
 * 
 * // 第三阶段：媒体数据同步（用户选择）
 * if (userWantsMediaSync) {
 *     workManager.beginUniqueWork(
 *         "progressive_sync", 
 *         ExistingWorkPolicy.APPEND, 
 *         mediaSync
 *     ).enqueue()
 * }
 * 
 * ========================================
 * 7. 错误恢复和重试
 * ========================================
 * 
 * 【场景：在现有流程中添加恢复步骤】
 * 
 * // 正常处理流程
 * workManager.beginUniqueWork(
 *     "data_processing", 
 *     ExistingWorkPolicy.REPLACE, 
 *     processTask
 * ).then(saveTask).enqueue()
 * 
 * // 如果出现错误，添加恢复步骤
 * if (hasError) {
 *     workManager.beginUniqueWork(
 *         "data_processing", 
 *         ExistingWorkPolicy.APPEND, 
 *         recoveryTask
 *     ).enqueue()
 *     
 *     // 添加清理步骤
 *     workManager.beginUniqueWork(
 *         "data_processing", 
 *         ExistingWorkPolicy.APPEND, 
 *         cleanupTask
 *     ).enqueue()
 * }
 * 
 * ========================================
 * 【APPEND 策略的优势总结】
 * ========================================
 * 
 * 1. 灵活性
 *    - 可以根据运行时条件动态扩展工作流
 *    - 支持用户交互驱动的任务添加
 * 
 * 2. 模块化
 *    - 核心流程和扩展功能分离
 *    - 便于维护和测试
 * 
 * 3. 用户体验
 *    - 用户可以选择需要的功能
 *    - 避免不必要的任务执行
 * 
 * 4. 资源优化
 *    - 只执行用户需要的步骤
 *    - 避免浪费系统资源
 * 
 * ========================================
 * 【使用注意事项】
 * ========================================
 * 
 * 1. 只能追加到任务链
 *    // ❌ 错误：不能追加到单个任务
 *    workManager.beginUniqueWork("single", ExistingWorkPolicy.REPLACE, task).enqueue()
 *    workManager.beginUniqueWork("single", ExistingWorkPolicy.APPEND, appendTask).enqueue() // 失败
 * 
 *    // ✅ 正确：可以追加到任务链
 *    workManager.beginUniqueWork("chain", ExistingWorkPolicy.REPLACE, task1).then(task2).enqueue()
 *    workManager.beginUniqueWork("chain", ExistingWorkPolicy.APPEND, appendTask).enqueue() // 成功
 * 
 * 2. 执行顺序保证
 *    - 新任务总是追加到现有任务链的末尾
 *    - 不会插入到中间位置
 * 
 * 3. 状态管理
 *    - 需要合理管理任务链的状态
 *    - 考虑任务失败时的处理策略
 * 
 * ========================================
 * 【总结】
 * ========================================
 * 
 * APPEND 策略特别适合需要动态扩展、用户交互驱动、模块化设计的场景，
 * 让工作流能够根据实际需求灵活调整，而不是预先定义所有步骤。
 * 
 * 这种设计模式在现代应用开发中非常有用，特别是在需要根据用户选择、
 * 运行时条件或插件系统来动态构建工作流的场景下。
 */
