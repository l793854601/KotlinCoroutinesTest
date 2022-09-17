package com.tkm.core

import kotlinx.coroutines.*
import org.junit.Test
import java.io.BufferedReader
import java.io.FileReader

class CoroutineTest03 {
    @Test
    fun testReleaseResources() = runBlocking<Unit> {
        val job = launch {
            try {
                repeat(1000) {
                    println("job: I'm sleeping $it")
                    delay(500)
                }
            } finally {
                //  可在finally中做一些释放资源的操作
                println("finally: release resources")
            }
        }

        delay(1300)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit")
    }

    //  使用use操作Closeable（I/O操作）
    @Test
    fun testUseFunction() = runBlocking {
//        val br = BufferedReader(FileReader(""))
//        with(br) {
//            var line: String?
//            try {
//                while (true) {
//                    line = readLine() ?: break
//                    println(line)
//                }
//            } finally {
//                try {
//                    br.close()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//        }

        val br = BufferedReader(FileReader(""))
        br.use {
            var line: String?
            while (true) {
                line = it.readLine() ?: break
                println(line)
            }
        }
    }

    //  处于取消中状态的协程不能够挂起（运行不能取消的代码）
    //  当协程被取消后需要调用挂起函数，我们需要将代码放置于NonCancellable CoroutineContext中
    @Test
    fun testCancelWithNonCancellable() = runBlocking {
        val job = launch {
            try {
                repeat(1000) {
                    println("job: I'm sleeping $it")
                    delay(500)
                }
            } finally {
                //  NonCancellable：isActive始终为true，isCompleted始终为false，isCancelled始终为false
                withContext(NonCancellable) {
                    println("job: I'm running finally")
                    delay(1000L)
                    println("job: And I've just delayed for 1 sec because I'm non-cancellable")
                }
            }
        }

        delay(1300)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit")
    }
}