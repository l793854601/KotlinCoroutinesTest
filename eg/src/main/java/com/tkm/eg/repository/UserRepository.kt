package com.tkm.eg.repository

import com.tkm.basic.api.githubApi

class UserRepository {
    suspend fun getUser(name: String) = githubApi.getUserSuspend(name)
}