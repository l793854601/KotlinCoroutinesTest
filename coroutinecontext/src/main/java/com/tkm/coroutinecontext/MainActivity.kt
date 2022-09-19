package com.tkm.coroutinecontext

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClick(view: View) {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            Log.d(TAG, "caught exception: $exception")
        }
        GlobalScope.launch(exceptionHandler) {
            Log.d(TAG, "onClick")
            "abc".substring(100)
        }
    }
}