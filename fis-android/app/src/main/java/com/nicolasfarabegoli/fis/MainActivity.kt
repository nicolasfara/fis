package com.nicolasfarabegoli.fis

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.nicolasfarabegoli.fis.utils.isLocationPermissionGranted
import com.nicolasfarabegoli.fis.utils.requestLocationPermission
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class MainActivity : AppCompatActivity() {
    private val rxBleClient = FisApplication.rxBleClient
    private var scanDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        onScanBle()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (isLocationPermissionGranted(requestCode, grantResults)) {
            scanBleDevices()
        }
    }

    private fun onScanBle() {
        if (rxBleClient.isScanRuntimePermissionGranted) {
            scanBleDevices()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally { dispose() }
                    .subscribe(
                            {
                                scanDisposable?.dispose()
                                startActivity(ControlActivity.newInstance(this, it.bleDevice.macAddress))
                            },
                            { Log.e("MainActivity", "Error: $it") }
                    )
                    .let { scanDisposable = it }
        } else {
            requestLocationPermission(rxBleClient)
        }
    }

    private fun scanBleDevices(): Observable<ScanResult> {
        val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()

        val scanFilter = ScanFilter.Builder()
                .setDeviceName("Fireworks ignition system")
                // add custom filters if needed
                .build()

        return rxBleClient.scanBleDevices(scanSettings, scanFilter)
    }

    private fun dispose() {
        scanDisposable = null
    }
}