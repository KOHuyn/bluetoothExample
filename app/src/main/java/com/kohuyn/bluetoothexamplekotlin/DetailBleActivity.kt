package com.kohuyn.bluetoothexamplekotlin

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.kohuyn.bluetoothexamplekotlin.adapter.CharacteristicAdapter
import kotlinx.android.synthetic.main.activity_detail_ble.*
import java.lang.StringBuilder


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
    private val adapterCharacteristic by lazy { CharacteristicAdapter() }

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
        val response = StringBuilder()
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
                    getListCharacteristics(mBluetoothService?.getSupportedGattService())
                    logErr(TAG, "ACTION_GATT_SERVICE_DISCOVERED")
                }
                BleService.ACTION_DATA_AVAILABLE -> {
                    logErr(TAG, "ACTION_DATA_AVAILABLE")
                    response.append(intent.getStringExtra(BleService.EXTRA_DATA) ?: "NULL DATA")
                    logErr(
                        "DATA RESPONSE",response.toString()
                    )
                }
            }
        }
    }

    private fun getListCharacteristics(data: List<BluetoothGattService>?) {
        if (data != null && data.isNotEmpty()) {
            val items = mutableListOf<Pair<String, String>>()
            data.forEach { bleGattService ->
                items.add(Pair(bleGattService.uuid.toString(), "PARENT"))
                if (bleGattService.characteristics.isNotEmpty()) {
                    bleGattService.characteristics.forEach { child ->
                        items.add(
                            Pair(
                                child.uuid.toString(),
                                "CHILD-NOTIFY:${child.isNotifiable}-READ:${child.isReadable}-WRITE:${child.isWriteable}"
                            )
                        )
                        if (child.isWriteable && child.isReadable && child.isNotifiable) {
                            mBluetoothService?.setCharacteristicNotification(child, true)
                            logErr(TAG, "writeCharacteristic")
                            mNotifyCharacteristic = child
                        }
                    }
                }
            }
            adapterCharacteristic.items = items
        }
    }

    private val BluetoothGattCharacteristic.isNotifiable: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

    private val BluetoothGattCharacteristic.isReadable: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_READ != 0

    private val BluetoothGattCharacteristic.isWriteable: Boolean
        get() = properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_ble)
        setView()
        clickButton()
        setupRcv()
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

    private fun clickButton() {
        btnStartConnect.setOnClickListener {
            mBluetoothService?.writeCharacteristic(mNotifyCharacteristic, "0,69,100~")
        }
        btnStartPractice.setOnClickListener {
            mBluetoothService?.writeCharacteristic(mNotifyCharacteristic, "6,1610623922,1,bb5241111~-2927-3764-4921-7449-8578-1970-1210-5850-6340~")
        }
        btnDonePractice.setOnClickListener {
            mBluetoothService?.writeCharacteristic(mNotifyCharacteristic, "2")
        }
        btnClearData.setOnClickListener {
            mBluetoothService?.writeCharacteristic(mNotifyCharacteristic, "3")
        }
    }

    private fun setupRcv() {
        rcvCharacteristics.run {
            layoutManager = LinearLayoutManager(this@DetailBleActivity)
            adapter = adapterCharacteristic
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