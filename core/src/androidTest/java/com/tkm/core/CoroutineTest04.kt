package com.tkm.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test

class CoroutineTest04 {
    @Test
    fun testTimeoutTask() = runBlocking {
        //  超时，会抛出异常：kotlinx.coroutines.TimeoutCancellationException
        withTimeout(1300) {
            repeat(1000) {
                println("job: I'm sleeping $it")
                delay(500)
            }
        }
    }

    @Test
    fun testTimeoutTaskReturnNull() = runBlocking {
        val result = withTimeoutOrNull(1300) {
            repeat(1000) {
                println("job: I'm sleeping $it")
                delay(500)
            }
            //  未超时，则返回Done，超时了则返回null
            "Done"
        }
        println("result is $result")
    }
}