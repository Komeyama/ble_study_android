package com.komeyama.ble_study_android

import android.app.Application
import androidx.lifecycle.ViewModel

class MainViewModel(private val application: Application) : ViewModel() {

    private val bleService: BLEService = BLEService(application)

    fun startBLEScan() {
        if (bleService.isGattConnected) return
        bleService.scanDevice()
    }
}