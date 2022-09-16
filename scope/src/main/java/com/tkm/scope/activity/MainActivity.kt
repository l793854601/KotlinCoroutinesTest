package com.tkm.scope.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.tkm.basic.api.githubApi
import com.tkm.scope.R
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var tvResult: TextView
    private lateinit var btnMainScope: Button

    private val mainScope by lazy { MainScope() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tv_result)
        btnMainScope = findViewById(R.id.btn_main_scope)

        btnMainScope.setOnClickListener {
            mainScope.launch {
                runCatching {
                    delay(10000)
                    githubApi.getUserSuspend("bennyhuo")
                }.onSuccess {
                    tvResult.text = it.toString()
                }.onFailure {
                    //  协程被取消后，会抛出异常：CancellationException，一般不用理会
                    it.printStackTrace()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}