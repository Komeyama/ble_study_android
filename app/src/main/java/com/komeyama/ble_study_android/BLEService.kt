package com.komeyama.ble_study_android

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import timber.log.Timber
import java.util.*

class BLEService(private val application: Application) {

    private val deviceName = ""

    // private val deviceAddress = ""
    private val serviceUUID = ""
    private val rxCharacteristicUUID = ""
    private val txCharacteristicUUID = ""
    private val descriptorUUID = ""

    // BLE Scanner
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    // BLE Gatt
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothGattService: BluetoothGattService? = null
    var isGattConnected = false

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    fun scanDevice() {
        bluetoothLeScanner?.startScan(scanCallback)
    }

    fun reConnect() {
        reSetup()
        scanDevice()
    }

    fun sendMessage(message: String) {
        val txCharacteristic =
            bluetoothGattService?.getCharacteristic(UUID.fromString(txCharacteristicUUID))
        txCharacteristic?.setValue(message)
        txCharacteristic?.apply {
            bluetoothGatt?.writeCharacteristic(this)
        }
    }

    fun closeBLEService() {
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            bluetoothAdapter = null
        } catch (e: Exception) {
            Timber.e("close BLE Service error: $e")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Timber.d("BLE Scan result name: ${result?.device?.name}, address: ${result?.device?.address}")

            val isFoundDevice = result?.device?.name?.equals(deviceName) ?: return
            // val isFoundDevice = result?.device?.address?.equals(deviceAddress) ?: return

            if (isFoundDevice && !isGattConnected) {
                isGattConnected = true
                bluetoothGatt = result.device?.connectGatt(application, false, gattCallback)
                bluetoothLeScanner?.stopScan(this)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.e("BLE Scan callback error: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    gatt?.discoverServices()
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    closeBLEService()
                    reConnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_FAILURE || gatt == null) return
            Timber.d("BLE Service discovered: ${gatt.services}")

            bluetoothGattService = gatt.getService(UUID.fromString(serviceUUID))
            Timber.d("Get BLE Gatt Service: $bluetoothGattService")

            val rxCharacteristic =
                bluetoothGattService?.getCharacteristic(UUID.fromString(rxCharacteristicUUID))
            Timber.d("Get BLE RX Characteristic: $rxCharacteristic")

            if (!gatt.setCharacteristicNotification(rxCharacteristic, true)) return
            Timber.d("Set BLE RX Characteristic success!")

            val descriptor =
                rxCharacteristic?.getDescriptor(UUID.fromString(descriptorUUID)) ?: return
            Timber.d("Get BLE Descriptor: $descriptor")

            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            // descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

            gatt.writeDescriptor(descriptor)
            Timber.d("BLE descriptor setup completed!")
        }
    }

    private fun reSetup() {
        isGattConnected = false
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

}