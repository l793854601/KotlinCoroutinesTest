package com.tkm.flow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

//  Flow的异常处理
class CoroutineTest03 {

    private fun simpleFlow() = flow {
        for (i in 0..3) {
            println("Emitting $i")
            emit(i)
        }
    }

    //  收集Flow时的异常处理
    @Test
    fun testFlowException() = runBlocking {
        try {
            val flow = simpleFlow()
            flow.collect {
                println(it)
                check(it <= 1) { "collected $it" }
            }
        } catch (e: Exception) {
            println("caught: $e")
        }
    }

    //  发射Flow值的异常处理
    @Test
    fun testFlowException2() = runBlocking {
        flow {
            emit(1)
            throw ArithmeticException()
        }.catch {
            println("caught: $it")
            emit(10)
        }.flowOn(Dispatchers.IO)
            .collect(::println)
    }

    private fun simpleFlow2() = (1..3).asFlow()

    private fun simpleFlow3() = flow {
        emit(1)
        throw RuntimeException()
    }

    @Test
    fun testFlowCompletedInFinally() = runBlocking {
        try {
            val flow = simpleFlow2()
            flow.collect(::println)
        } finally {
            println("done")
        }
    }

    @Test
    fun testFlowCompletedInOnCompletion() = runBlocking {
        val flow = simpleFlow3()
        flow.onCompletion { exception ->
            //  onCompletion可以取到异常，但不能捕获异常
            //  想要捕获异常，还得使用catch
            if (exception != null) {
                println("done")
            }
        }.catch { exception ->
            println("caught: $exception")
        }.collect(::println)
    }
}