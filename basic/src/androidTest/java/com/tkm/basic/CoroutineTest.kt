package com.tkm.basic

import kotlinx.coroutines.delay

class CoroutineTest {
    suspend fun request(): String {
        println("before delay")
        delay(2000)
        println("after delay")
        return "result from request"
    }
}