package com.tkm.core

import kotlinx.coroutines.*
import org.junit.Test

class CoroutineTest02 {
    //  取消作用域会取消的子协程
    @Test
    fun testScopeCancel() = runBlocking<Unit> {
        println("run blocking: ${Thread.currentThread()}")
        //  CoroutineScope没有继承runBlocking的上下文，因此runBlocking不会等待CoroutineScope执行完毕
        //  需要在结尾加上delay函数等待CoroutineScope执行完毕
        //  val scope = CoroutineScope(this.coroutineContext)：这样写会继承runBlocking的上下文
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            println("job1: ${Thread.currentThread()}")
            delay(1000)
            println("job1 finished")
        }

        scope.launch {
            println("job2: ${Thread.currentThread()}")
            delay(1000)
            println("job2 finished")
        }
        delay(100)
        scope.cancel()
        delay(2000)
    }

    //  被取消的子协程并不会影响其他兄弟协程
    @Test
    fun testBrotherCancel() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val job1 = scope.launch {
            delay(1000)
            println("job1 finished")
        }

        val job2 = scope.launch {
            delay(1000)
            println("job2 finished")
        }
        delay(100)
        job1.cancel()
        delay(2000)
    }

    //  协程通过抛出一个特殊的CancellationException来处理取消操作
    @Test
    fun testCancellationException() = runBlocking {
        val job1 = GlobalScope.launch {
            try {
                delay(1000)
                println("job1 finished")
            } catch (e: Exception) {
                //  kotlinx.coroutines.JobCancellationException: StandaloneCoroutine was cancelled;
                e.printStackTrace()
            }
        }
        delay(100)
        //  取消协程，此时job状态为cancelling，不会立即取消
        job1.cancel(CancellationException("取消异常"))
        //  等待协程执行完毕
        job1.join()
        //  取消一个协程并等待它结束
//        job1.cancelAndJoin()
    }

    //  所有kotlinx.coroutines中的挂起函数（withContext、delay等）都是可以取消的
    //  如果协程正在执行计算任务，并且没有检查取消的话，那么它是不能被取消的
    @Test
    fun testCancelCpuTaskByIsActive() = runBlocking {
        val startTime = System.currentTimeMillis()
        //  Dispatch.Default：用于CPU密集型任务，比如JSON解析，计算等
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            //  使用isActive判断协程是否还在运行（是否未被取消）
            //  参考Java中thread执行循环任务是如何取消的（thread.interrupt()，并使用标记位退出while循环）
            while (i < 5 && isActive) {
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("job: I'm sleeping: ${i++}")
                    nextPrintTime += 500
                }
            }
        }
        delay(1300)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit")
    }

    @Test
    fun testCancelCpuTaskByEnsureActive() = runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            try {
                var nextPrintTime = startTime
                var i = 0
                while (i < 5) {
                    //  使用ensureActive()判断协程是否取消
                    //  本质为判断isActive，如果为false，则抛出CancellationException
                    ensureActive()
                    if (System.currentTimeMillis() >= nextPrintTime) {
                        println("job: I'm sleeping: ${i++}")
                        nextPrintTime += 500
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        delay(1300)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit")
    }

    @Test
    fun testCancelCpuTaskByYield() = runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (i < 5) {
                //  让出执行权
                yield()
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("job: I'm sleeping: ${i++}")
                    nextPrintTime += 500
                }
            }
        }
        delay(1300)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit")
    }
}