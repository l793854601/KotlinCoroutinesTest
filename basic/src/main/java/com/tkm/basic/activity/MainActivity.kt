package com.tkm.basic.activity

import android.annotation.SuppressLint
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.TextView
import com.tkm.basic.R
import com.tkm.basic.api.User
import com.tkm.basic.api.githubApi
//  协程上层框架
import kotlinx.coroutines.*
//  协程基础设施
import kotlin.coroutines.*

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var tvUser: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvUser = findViewById(R.id.tv_user)
    }

    @SuppressLint("StaticFieldLeak")
    fun asyncTaskTest(view: View) {
        object : AsyncTask<Void, Void, User>() {
            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg params: Void?): User? {
                return githubApi.getUserCallback("bennyhuo").execute().body()
            }
            @Deprecated("Deprecated in Java")
            override fun onPostExecute(result: User?) {
                result?.let {
                    tvUser.text = it.toString()
                }
            }
        }.execute()
    }

    //  协程使异步逻辑同步化
    fun coroutineTest(view: View) {
        //  GlobalScope：顶级协程，一般不常用
        //  Activity中使用lifecycleScope、ViewModel中使用viewModelScope
        //  Dispatchers.Main：保证协程在主线程上执行
        GlobalScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "before request: ${Thread.currentThread()}")
                //  Dispatchers.IO：切换线程
                //  使用suspend关键字修饰的函数叫做挂起函数
                //  挂起函数只能在协程体内或者其他挂起函数内调用（Continuation）
                val user = withContext(Dispatchers.IO) {
                    Log.d(TAG, "in request: ${Thread.currentThread()}")
                    githubApi.getUserSuspend("bennyhuo")
                }
                Log.d(TAG, "after request: ${Thread.currentThread()}")
                tvUser.text = user.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reset(view: View) {
        tvUser.text = null
    }

    fun suspendTest(view: View) {
        GlobalScope.launch(Dispatchers.Main) {
            Log.d(TAG, "before delay")
            //  挂起不会阻塞当前线程
            delay(10000)
            Log.d(TAG, "after delay")
        }
        Log.d(TAG, "after global scope")
    }

    fun blockTest(view: View) {
        GlobalScope.launch(Dispatchers.Main) {
            //  阻塞当前线程
            SystemClock.sleep(10000)
        }
        Log.d(TAG, "after global scope")
    }

    //  使用协程基础设施创建协程
    fun createCoroutineBasic(view: View) {
        //  创建协程体
        val continuation = suspend {
            delay(1000)
            5
        }.createCoroutine(object : Continuation<Int> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<Int>) {
                Log.d(TAG, "resumeWith: ${result.getOrNull()}, thread: ${Thread.currentThread()}")
            }
        })
        //  启动协程
        continuation.resume(Unit)
    }
}