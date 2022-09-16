package com.tkm.eg.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.tkm.eg.viewmodel.MainViewModel
import com.tkm.eg.viewmodel.MainViewModelFactory
import com.tkm.eg.R
import com.tkm.eg.repository.UserRepository
import com.tkm.eg.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels(
        factoryProducer = { MainViewModelFactory(UserRepository()) }
    )

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        viewModel.userData.observe(this) {
            binding.tvUser.text = it.toString()
        }

        viewModel.errorData.observe(this) {
            binding.tvUser.text = it.toString()
        }

        binding.btnUser.setOnClickListener {
            viewModel.getUser()
        }
    }
}