package com.tkm.flow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

//  Flow操作符
class CoroutineTest02 {

    private suspend fun performRequest(request: Int): String {
        delay(1000)
        return "response: $request"
    }

    @Test
    fun testTransformFlowOperator() = runBlocking {
//        (1..3).asFlow()
//            .map { request -> performRequest(request) }
//            .collect(::println)

        //  使用transform，可以发射任意值任意次
        (1..3).asFlow()
            .transform {
                emit("Making request: $it")
                emit(performRequest(it))
            }.collect(::println)
    }

    private fun numbers() = flow<Int> {
        try {
            emit(1)
            emit(2)
            println("This line will not execute")
            emit(3)
        } catch (e: Exception) {
            //  kotlinx.coroutines.flow.internal.AbortFlowException
            e.printStackTrace()
        } finally {
            println("Finally in numbers")
        }
    }

    //  take：限制长度
    @Test
    fun testLimitLengthOperator() = runBlocking {
        numbers().take(2)
            .collect(::println)
    }

    //  末端操作符
    //  末端操作符是在Flow上用于Flow收集的挂起函数，collect是最基础的末端操作符
    //  转化为各种集合，例如toList、toSet
    //  获取第一个值（first）
    //  确保Flow发射单个值（single）
    //  使用reduce、fold将Flow规约到单个值
    @Test
    fun testTerminalOperator() = runBlocking {
        val sum = (1..5).asFlow().map { it * it }
            .reduce { accumulator, value -> accumulator + value }
        println(sum)
    }

    //  使用zip组合两个Flow中的相关值
    @Test
    fun testZip() = runBlocking<Unit> {
        val numbers = flowOf(1, 2, 3).onEach { delay(300) }
        val strings = flowOf("a", "b", "c").onEach { delay(400) }
        val startTime = System.currentTimeMillis()
        val zip = numbers.zip(strings) { number, string -> "$number -> $string" }
        zip.collect {
            println("$it at ${System.currentTimeMillis() - startTime}")
        }
    }

    private fun requestFlow(i: Int) = flow {
        emit("$i: First")
        delay(500)
        emit("$i: Second")
    }

    //  展平流（参考Rx中的flatMap）
    @OptIn(FlowPreview::class)
    @Test
    fun testFlatConcat() = runBlocking {
        val startTime = System.currentTimeMillis()
        (1..3).asFlow()
            .flatMapConcat { requestFlow(it) }
            .onEach { delay(100) }
            .collect {
                println("$it at ${System.currentTimeMillis() - startTime}")
            }
    }

    @OptIn(FlowPreview::class)
    @Test
    fun testFlatMerge() = runBlocking {
        val startTime = System.currentTimeMillis()
        (1..3).asFlow()
            .flatMapMerge { requestFlow(it) }
            .onEach { delay(100) }
            .collect {
                println("$it at ${System.currentTimeMillis() - startTime}")
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testFlatLatest() = runBlocking {
        val startTime = System.currentTimeMillis()
        (1..3).asFlow()
            .flatMapLatest { requestFlow(it) }
            .onEach { delay(100) }
            .collect {
                println("$it at ${System.currentTimeMillis() - startTime}")
            }
    }
}