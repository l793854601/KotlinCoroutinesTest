package com.tkm.coroutinecontext

import kotlinx.coroutines.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom

/*
    CoroutineContext是一组用于定义协程行为的元素，它由如下几项构成：
    1.Job：控制协程的生命周期
    2.CoroutineDispatcher：向合适的线程分发任务
    3.CoroutineName：协程的名称，调试的时候很有用
    4.CoroutineExceptionHandler：处理未捕获的异常
 */
class CoroutineTest01 {

    @Test
    fun testCoroutineDispatcher() = runBlocking<Unit> {
        launch {
            //  运行在父协程的上下文中，即runBlocking主协程
            println("launch: ${Thread.currentThread().name}")
        }

        launch(Dispatchers.Unconfined) {
            //  立即在当前函数调用栈中调用，直到遇到第一个挂起点
            //  因此会在主线程中执行
            println("launch unconfined: ${Thread.currentThread().name}")
        }

        launch(Dispatchers.Default) {
            //  默认调度器
            println("launch default: ${Thread.currentThread().name}")
        }

        launch(newSingleThreadContext("MyOwnThread")) {
            //  为协程的运行启动一个线程，当不再需要时，需要调用close释放
            println("launch newSingleThreadContext: ${Thread.currentThread().name}")
        }
        println("after launches")
    }

    @Test
    fun testCoroutineContext() = runBlocking<Unit> {
        //  CoroutineContext本质为一个数据结构，重载了+运算符
        launch(Dispatchers.Default + CoroutineName("test")) {
            println("I'm working in thread: ${Thread.currentThread().name}")
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
        //  job会继承scope中的exceptionHandler
        val job = scope.launch(Dispatchers.IO) {
            throw IllegalStateException("Just test")
        }
        job.join()
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
    //  异常的传播特性：
    //  当一个协程由于一个异常而运行失败时，它会传播这个异常病传递给它的父协程，接下来父协程会进行下面的操作：
    //  1.取消它自己的子协程
    //  2.取消它自己
    //  3.将异常传播并传毒给它的父协程
    @Test
    fun testExceptionPropagation2() = runBlocking<Unit> {
        val scope = CoroutineScope(Job())
        val job = scope.launch {
            async {
                //  async作为launch的子协程
                //  如果async内部抛出异常，则launch会立即抛出异常
                throw IllegalArgumentException()
            }
        }
        job.join()
    }

    //  使用SupervisorJob时，一个子协程的运行失败不会影响其到其他子协程
    //  SupervisorJob不会传播给它的父协程，它会让子协程自己处理异常
    @Test
    fun testSupervisorJob() = runBlocking {
        val supervisor = CoroutineScope(SupervisorJob())
        val job1 = supervisor.launch {
            delay(100)
            println("child1")
            //  job1抛出异常，不会传播给supervisor（父协程），进而也不会取消job2（兄弟协程）
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

    //  supervisorScope：当子协程抛出异常时，不会影响该子协程的兄弟协程
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
                } catch (e: Exception) {
                    //  此处抛出的异常为kotlinx.coroutines.JobCancellationException
                    println("caught child exception: $e")
                }
                finally {
                    println("The child is cancelled")
                }
            }
            yield()
            println("Throwing exception from the scopr")
            //  supervisorScope内部抛出异常，child（子协程）也会被取消
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
            //  会被exceptionHandler处理
            throw AssertionError()
        }
        val deferred = GlobalScope.async(exceptionHandler) {
            //  不会被exceptionHandler处理
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

    @Test
    fun testCancelAndException() = runBlocking<Unit> {
        val job = launch {
            val child = launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    println("child is cancelled")
                }
            }
            yield()
            println("cancelling child")
            //  取消子协程，不会影响父协程
            child.cancelAndJoin()
            yield()
            println("parent is not cancelled")
        }
        job.join()
    }

    //  子协程抛出异常，父协程要先取消子协程，知道子协程都取消完毕，才会处理这个异常
    @Test
    fun testCancelAndException2() = runBlocking<Unit> {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("caught exception: $exception")
        }

        val job = GlobalScope.launch(handler) {
            launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    //  NonCancellable不可被取消，知道执行完毕
                    withContext(NonCancellable) {
                        println("child is cancelled, but exception is not caught")
                        delay(100)
                        println("child is finished")
                    }
                }
            }

            launch {
                delay(10)
                println("before ArithmeticException")
                throw ArithmeticException()
            }
        }

        job.join()
    }

    //  当协程的多个子协程因为异常而失败时，一般情况下取第一个异常进行处理
    //  在第一个异常之后发生的其他异常，都将被绑定到第一个异常之上（Throwable.getSuppressed()）
    @Test
    fun testCancelAndException3() = runBlocking<Unit> {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("caught exception: $exception, ${exception.suppressed.contentToString()}")
        }

        val job = GlobalScope.launch(handler) {
            launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    throw ArithmeticException()
                }
            }

            launch {
                delay(10)
                throw IOException()
            }
        }

        job.join()
    }
}