package com.maxim.shacamera.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.maxim.shacamera.R
import com.maxim.shacamera.core.App
import com.maxim.shacamera.core.sl.ProvideViewModel
import com.maxim.shacamera.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), ProvideViewModel {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel = viewModel(MainViewModel::class.java)

        viewModel.observe(this) {
            it.show(supportFragmentManager, R.id.container)
        }

        viewModel.init(savedInstanceState == null)
    }

    override fun onResume() {
        super.onResume()

        val permissionList = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionList.isNotEmpty()) {
            requestPermissions(
                permissionList.toTypedArray(), 1
            )
        }
    }

    override fun <T : ViewModel> viewModel(clasz: Class<T>): T {
        return (application as App).viewModel(clasz)
    }
}