package com.example.workmanagerdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.example.workmanagerdemo.workers.*
import java.util.concurrent.TimeUnit
import com.example.workmanagerdemo.ui.theme.WorkManagerDemoTheme
import com.example.workmanagerdemo.ui.SingleWorkScreen
import com.example.workmanagerdemo.ui.PeriodicWorkScreen
import com.example.workmanagerdemo.ui.ConstraintWorkScreen
import com.example.workmanagerdemo.ui.DataWorkScreen
import com.example.workmanagerdemo.ui.CancelUniqueWorkScreen
import com.example.workmanagerdemo.ui.TagWorkScreen
import com.example.workmanagerdemo.ui.StatusObserveScreen
import com.example.workmanagerdemo.ui.ForegroundWorkScreen
import com.example.workmanagerdemo.ui.RetryWorkScreen
import com.example.workmanagerdemo.ui.ParallelBranchWorkScreen
import com.example.workmanagerdemo.ui.ExpeditedWorkScreen
import com.example.workmanagerdemo.ui.WorkManagerObserveScreen
import com.example.workmanagerdemo.ui.WorkerTypeDemoScreen
import com.example.workmanagerdemo.ui.HistoricalMessageTestScreen

import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.workmanagerdemo.ui.UniqueWorkScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkManagerDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WorkManagerDemoScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}


@Composable
fun WorkManagerDemoScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main", modifier = modifier) {
        composable("main") {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "WorkManager 能力演示", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.navigate("single") }) { Text("一次性任务演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("periodic") }) { Text("周期性任务演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("constraint") }) { Text("约束任务演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("data") }) { Text("数据传递/任务链演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("cancelunique") }) { Text("任务取消与唯一任务演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("tag") }) { Text("任务标签演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("statusobserve") }) { Text("任务状态监听演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("foreground") }) { Text("前台服务任务演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("retry") }) { Text("任务重试演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("parallelbranch") }) { Text("任务链并行与分支演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("expedited") }) { Text("加急任务调度演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("observe") }) { Text("WorkManager 监听演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("unique") }) { Text("唯一任务链演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("workerTypeDemo") }) { Text("Worker 类型用法演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("compatibility") }) { Text("兼容性测试演示") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("historical") }) { Text("历史消息测试演示") }
                Spacer(modifier = Modifier.height(200.dp))
            }
        }
    composable("single") { SingleWorkScreen() }
    composable("periodic") { PeriodicWorkScreen() }
    composable("constraint") { ConstraintWorkScreen() }
    composable("data") { DataWorkScreen() }
    composable("cancelunique") { CancelUniqueWorkScreen() }
    composable("tag") { TagWorkScreen() }
    composable("statusobserve") { StatusObserveScreen() }
    composable("foreground") { ForegroundWorkScreen() }
    composable("retry") { RetryWorkScreen() }
    composable("parallelbranch") { ParallelBranchWorkScreen() }
    composable("expedited") { ExpeditedWorkScreen() }
    composable("observe") { WorkManagerObserveScreen() }
    composable("unique") { UniqueWorkScreen() }
    composable("workerTypeDemo") { WorkerTypeDemoScreen() }
    composable("historical") { HistoricalMessageTestScreen() }
    }
}

@Preview(showBackground = true)
@Composable
fun WorkManagerDemoPreview() {
    WorkManagerDemoTheme {
        WorkManagerDemoScreen()
    }
}