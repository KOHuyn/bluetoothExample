package com.kohuyn.bluetoothexamplekotlin

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_detail_ble.*


/**
 * Created by KO Huyn on 1/14/2021.
 */
class DetailBleActivity : AppCompatActivity() {

    private var mBluetoothService: BleService? = null
    private var mConnected: Boolean = false
        set(value) {
            field = value
            txtDeviceStatus.text = "Status:${if (field) "Connected" else "Disconnected"}"
            btnConnectBle.text = "${if (field) "Disconnect" else "Connect"}"
        }
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private val bleName: String? by lazy { intent?.getStringExtra(BLE_NAME_ARG) }
    private val bleAddress: String? by lazy { intent?.getStringExtra(BLE_ADDRESS_ARG) }

    companion object {
        const val BLE_NAME_ARG = "BLE_NAME_ARG"
        const val BLE_ADDRESS_ARG = "BLE_ADDRESS_ARG"
        const val TAG = "BLUETOOTH"
    }

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mBluetoothService = (service as BleService.LocalBinder).getService()
            if (mBluetoothService?.initialize() == false) {
                logErr(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            mBluetoothService?.connect(bleAddress)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBluetoothService = null
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceive = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                BleService.ACTION_GATT_CONNECTED -> {
                    logErr(TAG, "ACTION_GATT_CONNECTED")
                    mConnected = true
                }
                BleService.ACTION_GATT_DISCONNECTED -> {
                    logErr(TAG, "ACTION_GATT_DISCONNECTED")
                    mConnected = false
                }
                BleService.ACTION_GATT_SERVICE_DISCOVERED -> {
                    logErr(TAG, "ACTION_GATT_SERVICE_DISCOVERED")
                }
                BleService.ACTION_DATA_AVAILABLE -> {
                    logErr(TAG, "ACTION_DATA_AVAILABLE")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_ble)
        setView()
        val gattServiceIntent = Intent(this, BleService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        btnConnectBle.setOnClickListener {
            if (mConnected) {
                mBluetoothService?.disconnect()
            } else {
                mBluetoothService?.connect(bleAddress)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceive, makeGattUpdateIntentFilter())
        if (mBluetoothService != null) {
            val result = mBluetoothService?.connect(bleAddress)
            logErr(TAG, "result:$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceive)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothService = null
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BleService.ACTION_GATT_SERVICE_DISCOVERED)
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }

    @SuppressLint("SetTextI18n")
    private fun setView() {
        txtDeviceName.text = "NameBle:${intent?.getStringExtra(BLE_NAME_ARG) ?: "No name"}"
        txtDeviceAddress.text = "Address:${intent?.getStringExtra(BLE_ADDRESS_ARG) ?: "NoAddress"}"
    }
}