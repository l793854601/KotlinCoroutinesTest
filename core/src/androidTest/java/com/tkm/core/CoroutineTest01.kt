package com.tkm.core

import kotlinx.coroutines.*
import org.junit.Test
import kotlin.system.measureTimeMillis

class CoroutineTest01 {
    //  runBlocking：运行一个新的协程，并阻塞当前线程，直到该协程执行完毕（前提是子协程必须继承父协程的上下文）
    //  不应该在协程中使用此方法，此方法一般用于测试
    @Test
    fun testCoroutineBuilder() = runBlocking {
        val job1 = launch {
            delay(2000)
            println("job1 finished")
        }

        val job2 = async {
            delay(2000)
            println("job2 finished")
            "job2 result"
        }

        val job2Result = job2.await()
        println("job2Result: $job2Result")
    }

    @Test
    fun testCoroutineJoin() = runBlocking {
        val job1 = launch {
            delay(2000)
            println("job1 finished")
        }
        job1.join()

        val job2 = launch {
            delay(200)
            println("job2 finished")
        }

        val job3 = launch {
            delay(200)
            println("job3 finished")
        }
    }

    @Test
    fun testCoroutineAwait() = runBlocking {
        val job1 = async {
            delay(2000)
            println("job1 finished")
        }
        job1.await()

        val job2 = async {
            delay(200)
            println("job2 finished")
        }

        val job3 = async {
            delay(200)
            println("job3 finished")
        }
    }

    @Test
    fun testSync() = runBlocking {
        val time = measureTimeMillis {
            val num1 = task1()
            val num2 = task2()
            val result = num1 + num2
            println("the result: $result")
        }
        println("completed time: $time")
    }

    @Test
    fun testCombineAsync() = runBlocking {
        val time = measureTimeMillis {
            val job1 = async { task1() }
            val job2 = async { task2() }
            val result = job1.await() + job2.await()
            println("the result: $result")
        }
        println("completed time: $time")
    }

    private suspend fun task1(): Int {
        delay(1000)
        return 2
    }

    private suspend fun task2(): Int {
        delay(2000)
        return 3
    }

    @Test
    fun testStartModeDefault() = runBlocking {
        //  DEFAULT：协程创建后，立即开始调度，在调度前如果协程被取消，其将直接进入取消响应的状态
        val job = launch(start = CoroutineStart.DEFAULT) {
            println("before delay")
            delay(50000)
            println("job finished")
        }
        delay(1000)
        job.cancel()
        println("is completed: ${job.isCompleted}")
        println("is cancelled: ${job.isCancelled}")
    }

    @Test
    fun testStartModeAtomic() = runBlocking {
        //  ATOMIC：协程创建后，立即开始调度，协程执行到第一个挂起点之前不响应取消
        val job = launch(start = CoroutineStart.ATOMIC) {
            //  第一个挂起点之前的逻辑，会被执行
            println("before delay")
            //  第一个挂起点
            delay(1000)
            println("job finished")
        }
        job.cancel()
        println("is completed: ${job.isCompleted}")
        println("is cancelled: ${job.isCancelled}")
    }

    @Test
    fun testStartModeLazy() = runBlocking {
        //  LAZY：只有协程被需要时，包括主动调用start、join、await等函数时才会开始调度
        //  如果调度前被取消，则协程直接进入异常结束状态
        val job = launch(start = CoroutineStart.LAZY) {
            //  只有调用join后，才开始调度执行
            println("job finished")
        }

        job.join()
        println("is completed: ${job.isCompleted}")
    }

    @Test
    fun testStartModeUndispatched() = runBlocking {
        //  UNDISPATCHED：协程创建后，立即在当前函数调用栈执行，直到遇到第一个真正的挂起点
        val job = launch(
            context = Dispatchers.IO,
            start = CoroutineStart.UNDISPATCHED
        ) {
            //  主线程（当前函数调用栈）
            println("before delay, thread: ${Thread.currentThread()}")
            //  第一个挂起点
            delay(1000)
            //  子线程
            println("job finished, thread: ${Thread.currentThread()}")
        }
        println("after job finished")
    }

    @Test
    fun testCoroutineScopeBuilder() = runBlocking {
        //  coroutineScope：挂起函数，不会阻塞当前线程（区别于runBLocking，runBlocking会阻塞当前线程）
        //  coroutineScope会等待内部协程都执行完毕，才会结束
        //  coroutineScope内部的协程如果有一个失败了，则其他兄弟协程也会被取消
        coroutineScope {
            val job1 = launch {
                delay(400)
                println("job1 finished")
            }

            val job2 = async {
                delay(200)
                println("job2 finished")
                throw IllegalStateException()
                "job2 result"
            }
        }
        println("after coroutine scope")
    }

    @Test
    fun testSupervisorScopeBuilder() = runBlocking {
        //  supervisorScope：内部的协程失败了，不会影响其他兄弟协程的执行
        supervisorScope {
            val job1 = launch {
                delay(400)
                println("job1 finished")
            }

            val job2 = async {
                delay(200)
                println("job2 finished")
                throw IllegalStateException()
                "job2 result"
            }
        }
        println("after coroutine scope")
    }
}