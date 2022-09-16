package com.tkm.eg.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tkm.basic.api.User
import com.tkm.eg.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainViewModel(private val repository: UserRepository) : ViewModel() {

    val userData by lazy { MutableLiveData<User>() }

    val errorData by lazy { MutableLiveData<Throwable>() }

    fun getUser() {
        viewModelScope.launch {
            runCatching {
                repository.getUser("bennyhuo")
            }.onSuccess {
                userData.value = it
            }.onFailure {
                it.printStackTrace()
                if (it !is CancellationException) {
                    errorData.value = it
                }
            }
        }
    }
}