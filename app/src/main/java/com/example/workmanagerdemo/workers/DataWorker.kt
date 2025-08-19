package com.example.workmanagerdemo.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.Data
import android.util.Log

/**
 * 数据传递任务 Worker 示例。
 * 用于演示 WorkManager 的输入输出数据能力。
 * 可在任务间安全传递数据。
 *
 * 使用场景：
 * 1. 需要将参数或配置信息传递给后台任务（如上传文件路径、同步账号等）。
 * 2. 任务链中，前一个任务的结果作为下一个任务的输入（如下载后解压、处理后上传等）。
 * 3. 需要将任务执行结果返回给主线程或其他任务（如处理结果、状态、数据等）。
 *
 * 方法说明：
 * - 输入数据：通过 WorkRequest 的 setInputData(Data) 方法传递，Worker 内通过 inputData 获取。
 * - 输出数据：Worker 执行完毕后通过 Result.success(Data) 返回，主线程可通过 WorkInfo.outputData 获取。
 * - 数据类型：Data 支持基本类型（String、Int、Boolean、Long、Double）和数组，适合轻量级数据传递。
 *
 * 示例：
 * // 构造输入数据
 * val inputData = Data.Builder().putString("input_key", "测试数据").build()
 * // 创建任务请求并设置输入数据
 * val request = OneTimeWorkRequestBuilder<DataWorker>()
 *     .setInputData(inputData)
 *     .build()
 * // 入队任务
 * WorkManager.getInstance(context).enqueue(request)
 * // 监听任务结果
 * WorkManager.getInstance(context).getWorkInfoByIdLiveData(request.id).observe(...) {
 *     val output = it?.outputData?.getString("output_key")
 * }
 *
 * 任务链场景：
 * val workA = OneTimeWorkRequestBuilder<WorkerA>().build()
 * val workB = OneTimeWorkRequestBuilder<WorkerB>().build()
 * WorkManager.getInstance(context)
 *     .beginWith(workA)
 *     .then(workB)
 *     .enqueue()
 * // WorkerA 的输出可作为 WorkerB 的输入
 */
class DataWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        // 获取输入数据
        val input = inputData.getString("input_key")
        android.util.Log.d("DataWorker", "数据任务执行，收到输入: $input")
        // 处理并返回输出数据
        val output = Data.Builder().putString("output_key", "收到: $input").build()
        return Result.success(output)
    }
}
