package com.kohuyn.bluetoothexamplekotlin

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.util.*


/**
 * Created by KO Huyn on 1/14/2021.
 */
class BleService : Service() {

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionState: Int = STATE_DISCONNECTED

    companion object {
        const val TAG = "BLUETOOTH"
        val UUID_HEART_RATE_MEASUREMENT: UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        const val ACTION_GATT_CONNECTED = "com.kohuyn.bluetooth.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.kohuyn.bluetooth.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICE_DISCOVERED =
            "com.kohuyn.bluetooth.ACTION_GATT_SERVICE_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.kohuyn.bluetooth.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.kohuyn.bluetooth.EXTRA_DATA"
    }

    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            var intentAction: String
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = ACTION_GATT_CONNECTED
                    mConnectionState = STATE_CONNECTED
                    logErr(TAG, "Connected to GATT server.")
                    logErr(
                        TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt?.discoverServices()
                    )
                    broadcastUpdate(intentAction)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = ACTION_GATT_DISCONNECTED
                    mConnectionState = STATE_DISCONNECTED
                    logErr(TAG, "Disconnected from GATT server.")
                    broadcastUpdate(intentAction)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICE_DISCOVERED)
            } else {
                logErr(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic?) {
        val intent = Intent(action)
        if (UUID_HEART_RATE_MEASUREMENT == characteristic?.uuid) {
            var flag = characteristic.properties
            var format = -1
            if (flag and 0x01 != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16
                logErr(TAG, "Heart rate format UINT16.")
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8
                logErr(TAG, "Heart rate format UINT8.")
            }
            val heartRate = characteristic.getIntValue(format, 1)
            logErr(TAG, String.format("Received heart rate: %d", heartRate))
            intent.putExtra(EXTRA_DATA, heartRate.toString())
        } else {
            // For all other profiles, writes the data formatted in HEX.
            val data = characteristic?.value
            if (data != null && data.isNotEmpty()) {
                val stringBuilder = StringBuilder(data.size)
                for (byteChar in data) {
                    stringBuilder.append(String.format("%02X", byteChar))
                }
                intent.putExtra(EXTRA_DATA, String(data) + "\n" + stringBuilder.toString())
            }
        }
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        fun getService(): BleService {
            return this@BleService
        }
    }

    private val mBinder: IBinder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
            if (mBluetoothManager == null) {
                logErr(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }
        mBluetoothAdapter = mBluetoothManager?.adapter
        if (mBluetoothAdapter == null) {
            logErr(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            logErr(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
            logErr(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            if (mBluetoothGatt?.connect() == true) {
                mConnectionState = STATE_CONNECTING
                return true
            } else {
                return false
            }
        }
        val device: BluetoothDevice? = mBluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            logErr(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        logErr(TAG, "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */

    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            logErr(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt?.disconnect()
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    private fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            logErr(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt?.readCharacteristic(characteristic)
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic?,
        enable: Boolean
    ) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            logErr(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt?.setCharacteristicNotification(characteristic, enable)
        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT == characteristic?.uuid) {
            val descriptor: BluetoothGattDescriptor = characteristic.getDescriptor(
                UUID.fromString(
                    CLIENT_CHARACTERISTIC_CONFIG
                )
            )
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            mBluetoothGatt?.writeDescriptor(descriptor)
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    fun getSupportedGattService(): List<BluetoothGattService>? {
        if (mBluetoothGatt == null) return null
        return mBluetoothGatt?.services
    }
}