package com.nicolasfarabegoli.fis

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_control.*
import com.jakewharton.rx.ReplayingShare
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.*

private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
private val IGNITION_0_CHAR = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a0")
private val IGNITION_1_CHAR = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a1")
private val IGNITION_2_CHAR = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a2")
private val IGNITION_3_CHAR = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a3")

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

class ControlActivity : AppCompatActivity() {
    private lateinit var bleDevice: RxBleDevice
    private lateinit var connectionObservable: Observable<RxBleConnection>
    private val disconnectTriggerSubject = PublishSubject.create<Unit>()
    private val connectionDisposable = CompositeDisposable()
    private var stateDisposable: Disposable? = null

    companion object {
        fun newInstance(context: Context, macAddress: String): Intent =
            Intent(context, ControlActivity::class.java).apply { putExtra(EXTRA_MAC_ADDRESS, macAddress) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        val macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        bleDevice = FisApplication.rxBleClient.getBleDevice(macAddress!!)

        bleDevice.observeConnectionStateChanges()
            .observeOn(Schedulers.io())
            .subscribe{ Log.i("ControlActivity", "State change: $it")}
            .let { stateDisposable = it }

        connectionObservable = prepareConnectionObservable()

        ignition_0_btn.setOnClickListener { ignite(IGNITION_0_CHAR) }
        ignition_1_btn.setOnClickListener { ignite(IGNITION_1_CHAR) }
        ignition_2_btn.setOnClickListener { ignite(IGNITION_2_CHAR) }
        ignition_3_btn.setOnClickListener { ignite(IGNITION_3_CHAR) }

        onConnect()
    }

    private fun onConnect() {
        if (bleDevice.connectionState == RxBleConnection.RxBleConnectionState.CONNECTED) {
            Log.i("ConnectActivity", "Ma dio cirsot")
        } else {
            connectionObservable
                .flatMapSingle { it.discoverServices() }
                .flatMapSingle {
                    it.getCharacteristic(IGNITION_0_CHAR)
                    it.getCharacteristic(IGNITION_1_CHAR)
                    it.getCharacteristic(IGNITION_2_CHAR)
                    it.getCharacteristic(IGNITION_3_CHAR)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { }
                .subscribe(
                    { Log.i("ControlActivity", "Hey, connection succeeded") },
                    { }
                )
                .let { connectionDisposable.add(it) }
        }
    }

    private fun prepareConnectionObservable(): Observable<RxBleConnection> =
        bleDevice
            .establishConnection(false)
            .takeUntil(disconnectTriggerSubject)
            .compose(ReplayingShare.instance())

    private fun dispose() {
        connectionDisposable.dispose()
    }

    private fun ignite(uuid: UUID) {
        if (bleDevice.connectionState == RxBleConnection.RxBleConnectionState.CONNECTED) {
            Log.i("ControlActivity", "IGNITION!!")
            connectionObservable
                .firstOrError()
                .flatMap {
                    it.writeCharacteristic(uuid, "s".toByteArray())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ bytes ->
                    Log.i("ControlActivity", "Writeddddd: $bytes")
                }, { Log.e("ControlActivity", "Error: $it") })
                .let { connectionDisposable.add(it) }
        }
    }
}