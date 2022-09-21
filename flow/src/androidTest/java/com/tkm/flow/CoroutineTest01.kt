package com.tkm.flow

import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import kotlin.system.measureTimeMillis

class CoroutineTest01 {

    //  集合返回了多个值，但不是异步的
    private fun simpleList(): List<Int> = listOf(1, 2, 3, 4, 5)

    private fun simpleSequence(): Sequence<Int> = sequence {
        for (i in 1..3) {
            //  阻塞，非挂起
            SystemClock.sleep(1000)
            //  由于@RestrictsSuspension的限制，只能调用SequenceScope中的挂起函数，不能调用其他挂起函数
//            delay(1000)
            yield(i)
        }
    }

    @Test
    fun testMultipleValues() {
        val list = simpleList()
        list.forEach(::println)

        val sequence = simpleSequence()
        sequence.forEach(::println)
    }

    //  返回了多个值，也是异步，但是一次性返回多个值
    private suspend fun simpleList2(): List<Int> {
        delay(1000)
        return listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun testMultipleValues2() = runBlocking {
        val list = simpleList2()
        list.forEach(::println)
    }

    //  使用Flow返回多个值，而且是异步的
    //  Flow与其他方式的区别：
    //  名为flow的Flow类型构建器函数
    //  flow{...}构建块中的代码可以挂起
    //  函数simpleFlow不再标有suspend
    //  流使用emit函数发射值
    //  流使用collect函数收集值
    private suspend fun simpleFlow(): Flow<Int> = flow {
        for (i in 1..5) {
            delay(1000)
            emit(i)
        }
    }

    @Test
    fun testMultipleValues3() = runBlocking {
        launch {
            for (i in 1..5) {
                println("I'm not blocked: $i")
                delay(1000)
            }
        }
        val flow = simpleFlow()
        flow.collect {
            println("${Thread.currentThread().name}, $it")
        }
    }

    private suspend fun simpleFlow2() = flow {
        println("flow started")
        for (i in 0..5) {
            delay(1000)
            emit(i)
        }
    }

    //  Flow是一种类似于序列的冷流，flow构建器中的代码直到流被收集（collect）的时候才会运行
    @Test
    fun testFlowIsCode() = runBlocking {
        val flow = simpleFlow2()
        println("calling collect")
        flow.collect(::println)

        println("calling collect again")
        flow.collect(::println)
    }

    //  流的连续性（对比Rx中流的特性）
    //  流的每次单独收集都是按顺序执行的，除非使用特殊操作符
    //  从上游到下游，每个过渡操作符（接收一个Flow，返回一个Flow）都会处理每个发射出的值，饭后再交给末端操作符
    @Test
    fun testFlowContinuation() = runBlocking<Unit> {
        (1..5).asFlow().filter {
            println("filter: $it")
            it % 2 == 0
        }.map {
            //  根据Flow是冷流的特性，只有从filter返回的值，才会传到map中
            println("map: $it")
            "$it"
        }.collect {
            println(it)
        }
    }

    @Test
    fun testFlowBuilder() = runBlocking<Unit> {
        //  使用flowOf构建Flow（对比listOf）
        flowOf("one", "two", "three")
            .onEach { delay(1000) }
            .collect(::println)

        //  使用asFlow()扩展函数，可以将各种集合与序列转换为Flow
        (1..3).asFlow()
            .collect(::println)
    }

    //  Flow的收集总是在调用协程的上下文中发生，Flow的该属性成为上下文保存
    //  flow{...}构建器中的代码必须遵循上下文保存属性，并且不允许从其他上下文中发射（不能再flow{...}内部使用withContext切换上下文）
    //  使用flowOn修改Flow发射元素的上下文（线程）
    private fun simpleFlow3() = flow {
        println("flow started: ${Thread.currentThread().name}")
        for (i in 0..5) {
            delay(1000)
            emit(i)
        }
    }.flowOn(Dispatchers.IO)

    @Test
    fun testFlowContext() = runBlocking {
        val flow = simpleFlow3()
        flow.collect {
            println("collected value: $it, ${Thread.currentThread().name}")
        }
    }

    //  模拟事件源
    private fun eventSource() = (1..5)
        .asFlow()
        .onEach { delay(1000) }
        .flowOn(Dispatchers.Default)

    //  使用launchIn替换collect，我们可以在单独的协程中启动流的收集
    @Test
    fun testFlowLaunch() = runBlocking<Unit> {
        val eventSource = eventSource()
        eventSource
            .onEach { println("Event: $it, ${Thread.currentThread().name}") }
//            .launchIn(this)
            .launchIn(CoroutineScope(Dispatchers.IO))
            .join()


//        val coroutineScope = CoroutineScope(Dispatchers.IO)
//        coroutineScope.launch {
//            eventSource.collect { element -> println("Event: $element, ${Thread.currentThread().name}") }
//        }.join()
    }

    private fun simpleFlow4() = flow {
        for (i in 0..5) {
            delay(1000)
            emit(i)
            println("emitting: $i")
        }
    }

    //  Flow采用与协程同样的协作取消
    //  像往常一样，Flow的收集可以是当Flow在一个可取消的挂起函数（例如delay）中挂起的时候取消
    @Test
    fun testCancelFlow() = runBlocking<Unit> {
        withTimeoutOrNull(2000) {
            val flow = simpleFlow4()
            flow.collect(::println)
        }
        println("Done")
    }

    //  为方便起见，Flow构建器对每个发射值执行附加的ensureActive检查以进行取消
    //  这意味着从flow{...}发出的繁忙循环是可以取消的
    //  出于性能原因，大多数其他Flow操作不会自行执行其他取消检测，在协程出于繁忙循环的情况下，必须明确检测是否取消
    //  通过cancellable操作符来执行此操作
    @Test
    fun testCancelFlowCheck() = runBlocking<Unit> {
//        val flow = simpleFlow4()
//        flow.collect {
//            println(it)
//            if (it == 3) {
//                cancel()
//            }
//        }


        (1..5).asFlow().cancellable().collect {
            println(it)
            if (it == 3) {
                cancel()
            }
        }

        println("Done")
    }

    private fun simpleFlow5() = flow {
        for (i in 0..5) {
            delay(100)
            emit(i)
            println("emitting: $i, ${Thread.currentThread().name}")
        }
    }

    //  处理Flow中背压的几种方式
    @Test
    fun testFlowBackPressure() = runBlocking {
        val time = measureTimeMillis {
            val flow = simpleFlow5()
            flow
//                .conflate()
//                .flowOn(Dispatchers.Default)
//                .buffer(50)
                .collectLatest {
                delay(300)
                println("collected: $it, ${Thread.currentThread().name}")
            }
        }
        println("total time: $time")
    }
}