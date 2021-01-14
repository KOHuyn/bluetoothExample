package com.kohuyn.bluetoothexamplekotlin

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.kohuyn.bluetoothexamplekotlin.adapter.ScanAdapter
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanning: Boolean = false
        set(value) {
            field = value
            btnScanDevice?.text = if (field) "Stop" else "Scan"
        }
    private val mHandler by lazy { Handler() }
    private val btnScans by lazy { btnScanDevice }
    private val rcv by lazy { rcvScan }
    private val adapterDevice by lazy { ScanAdapter() }

    companion object {
        const val TAG = "BLUETOOTH"
        const val REQUEST_ENABLE_BT = 1
        const val SCAN_PERIOD = 30000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        setupRcv()
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            toast("Bluetooth not supported.")
            finish()
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        mBluetoothAdapter = bluetoothManager?.adapter
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            toast("Bluetooth not supported.")
            finish()
        }
        btnScans.setOnClickListener {
            scanBleDevice(!mScanning)
            if (!mScanning) {
                adapterDevice.items.clear()
                adapterDevice.notifyDataSetChanged()
            }
        }
    }

    private fun checkPermission() {
        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Toast.makeText(
                    this,
                    "The permission to get BLE location data is required",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ), 1
                    )
                }
            }
        } else {
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRcv() {
        rcv.layoutManager = LinearLayoutManager(this)
        rcv.adapter = adapterDevice
        rcv.itemAnimator = null
        adapterDevice.onClickDevice = ScanAdapter.OnClickDeviceBleListener { _, pos ->
            val device = adapterDevice.items[pos]
            val intent = Intent(this, DetailBleActivity::class.java).apply {
                putExtra(DetailBleActivity.BLE_NAME_ARG, device.name)
                putExtra(DetailBleActivity.BLE_ADDRESS_ARG, device.address)
            }
            if (mScanning) {
                mBluetoothAdapter?.stopLeScan(mLeScanCallback)
                mScanning = false
            }
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            toast("Bạn không đồng ý mở bluetooth")
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun scanBleDevice(enable: Boolean) {
        if (enable) {
            mHandler.postDelayed({
                mScanning = false
                mBluetoothAdapter?.stopLeScan(mLeScanCallback)
            }, SCAN_PERIOD)
            mScanning = true
            mBluetoothAdapter?.startLeScan(mLeScanCallback)
        } else {
            mScanning = false
            mBluetoothAdapter?.stopLeScan(mLeScanCallback)
        }
    }


    private val mLeScanCallback: BluetoothAdapter.LeScanCallback =
        BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            runOnUiThread {
                logErr(TAG, "Add:${device.address}")
                adapterDevice.addDeviceBluetooth(device)
            }
        }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}