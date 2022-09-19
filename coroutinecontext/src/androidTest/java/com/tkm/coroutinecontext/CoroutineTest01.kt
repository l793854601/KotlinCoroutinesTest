package com.tkm.coroutinecontext

import kotlinx.coroutines.*
import org.junit.Test

/*
    CoroutineContext是一组用于定义协程行为的元素，它由如下几项构成：
    1.Job：控制协程的生命周期
    2.CoroutineDispatcher：向合适的线程分发任务
    3.CoroutineName：协程的名称，调试的时候很有用
    4.CoroutineExceptionHandler：处理未捕获的异常
 */
class CoroutineTest01 {

    @Test
    fun testCoroutineContext() = runBlocking<Unit> {
        launch(Dispatchers.Default + CoroutineName("test")) {
            println("I'm working in thread: ${Thread.currentThread()}")
        }
    }

    //  对于新创建的协程，它的CoroutineContext会包含一个全新的Job实例，它会帮助我们控制协程的生命周期
    //  而剩下的元素会从CoroutineContext的父类继承，该父类可能是另外一个协程或者创建该协程的CoroutineScope
    @Test
    fun testCoroutineContextExtend() = runBlocking<Unit> {
        //  result由job创建，job由scope创建
        //  result、job继承了scope的Dispatchers、CoroutineName
        val scope = CoroutineScope(Job() + Dispatchers.Default + CoroutineName("test"))
        val job = scope.launch {
            println("launch - ${coroutineContext[Job]}, ${Thread.currentThread().name}, ${coroutineContext[CoroutineName]}")
            val result = async {
                println("async - ${coroutineContext[Job]}, ${Thread.currentThread().name}, ${coroutineContext[CoroutineName]}")
                "OK"
            }.await()
        }
        job.join()
    }

    //  协程上下文 = 默认值 + 继承的CoroutineContext + 参数
    //  一些元素包含默认值：Dispatchers.DEFAULT是默认的CoroutineDispatcher，以及“coroutine”作为默认的CoroutineName
    //  继承的CoroutineContext是CoroutineScope或者其父协程的CoroutineContext
    //  传入协程构建起的产生逇优先级高于继承的上下文参数，因此会覆盖对应的参数值
    @Test
    fun testCoroutineContextExtend2() = runBlocking {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("caught exception: $exception")
        }
        val scope = CoroutineScope(Job() + Dispatchers.Main + exceptionHandler)
        scope.launch(Dispatchers.IO) {
            throw IllegalStateException("Just test")
        }.join()
    }

    //  根协程的异常传播：
    //  协程构建器有两种形式：自动传播异常（launch、actor），向用户暴露异常（async、produce）
    //  当这些构建器用于创建根协程时，自动传播异常会在它发生的第一时间被抛出，向用户暴露异常依赖用户最终消费（await、receive）
    @Test
    fun testExceptionPropagation() = runBlocking<Unit> {
        //  自动传播异常
        val job = GlobalScope.launch {
            //  launch启动的根协程，在此可以捕获异常
            try {
                println("in launch")
                throw IndexOutOfBoundsException()
            } catch (e: Exception) {
                println("caught exception: $e")
            }
        }
        job.join()

        //  向用户暴露异常
        val deferred = GlobalScope.async {
            println("in async")
            throw ArithmeticException()
        }
        try {
            //  async启动的异常，在此可以捕获异常
            deferred.await()
        } catch (e: Exception) {
            println("caught exception: $e")
        }
    }

    //  其他协程所创建的协程中，产生的异常总是会被传播
    @Test
    fun testExceptionPropagation2() = runBlocking<Unit> {
        val scope = CoroutineScope(Job())
        val job = scope.launch {
            async {
                throw IllegalArgumentException()
            }
        }
        job.join()
    }

    @Test
    fun testSupervisorJob() = runBlocking {
        val supervisor = CoroutineScope(SupervisorJob())
        val job1 = supervisor.launch {
            delay(100)
            println("child1")
            throw IllegalArgumentException()
        }

        val job2 = supervisor.launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("child2 finished")
            }
        }

        joinAll(job1, job2)
    }

    @Test
    fun testSupervisorScope() = runBlocking<Unit> {
        supervisorScope {
            launch {
                delay(100)
                println("child1")
                throw IllegalArgumentException()
            }
        }

        try {
            delay(Long.MAX_VALUE)
        } finally {
            println("child2 finished")
        }
    }

    //  supervisorScope：自身执行失败时，所有的子作业将会被全部取消
    @Test
    fun testSupervisorScope2() = runBlocking<Unit> {
        supervisorScope {
            val child = launch {
                try {
                    println("The child is sleeping")
                    delay(Long.MAX_VALUE)
                } finally {
                    println("The child is cancelled")
                }
            }
            yield()
            println("Throwing exception from the scopr")
            throw AssertionError()
        }
    }

    //  使用CoroutineExceptionHandler对协程的异常进行捕获：
    //  以下条件被满足时，异常就会被捕获
    //  时机：异常是被自动抛出异常（launch、actor）的协程所抛出的
    //  位置：在CoroutineScope的CoroutineContext中或者在一个根协程（CoroutineScope或者supervisorScope的直接子协程）中
    @Test
    fun testCoroutineExceptionHandler() = runBlocking<Unit> {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("caught exception: $exception")
        }
        val job = GlobalScope.launch(exceptionHandler) {
            throw AssertionError()
        }
        val deferred = GlobalScope.async(exceptionHandler) {
            throw ArithmeticException()
        }

        job.join()
        deferred.await()
    }

    @Test
    fun testCoroutineExceptionHandler2() = runBlocking {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("caught exception: $exception")
        }
        val scope = CoroutineScope(Job())
        val job = scope.launch(exceptionHandler) {
            launch {
                throw IllegalArgumentException()
            }
        }
        job.join()
    }

    @Test
    fun testCoroutineExceptionHandler3() = runBlocking {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("caught exception: $exception")
        }
        val scope = CoroutineScope(Job())
        val job = scope.launch {
            launch(exceptionHandler) {
                throw IllegalArgumentException()
            }
        }
        job.join()
    }
}