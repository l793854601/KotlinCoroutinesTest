package com.tkm.basic.api

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

val githubApi = Retrofit.Builder()
    .client(OkHttpClient.Builder().build())
    .baseUrl("https://api.github.com")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(GithubApi::class.java)

interface GithubApi {
    @GET("users/{login}")
    fun getUserCallback(@Path("login") login: String): Call<User>

    @GET("users/{login}")
    suspend fun getUserSuspend(@Path("login") login: String): User
}

data class User(val id: Int, val name: String?, val url: String?)