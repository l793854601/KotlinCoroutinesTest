package com.tkm.basic.activity

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.tkm.basic.R
import com.tkm.basic.api.User
import com.tkm.basic.api.githubApi

class MainActivity : AppCompatActivity() {

    private lateinit var tvUser: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvUser = findViewById(R.id.tv_user)
    }

    fun asyncTaskTest(view: View) {
        object : AsyncTask<Void, Void, User>() {
            override fun doInBackground(vararg params: Void?): User? {
                return githubApi.getUserCallback("bennyhuo").execute().body()
            }

            override fun onPostExecute(result: User?) {
                result?.let {
                    tvUser.text = it.toString()
                }
            }
        }.execute()
    }

    fun coroutineTest(view: View) {

    }
}