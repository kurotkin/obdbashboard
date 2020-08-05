package com.kurotkin.testobd

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kurotkin.testobd.obd.commands.SpeedCommand
import com.kurotkin.testobd.obd.commands.engine.RPMCommand
import com.kurotkin.testobd.obd.commands.protocol.EchoOffCommand
import com.kurotkin.testobd.obd.commands.protocol.LineFeedOffCommand
import com.kurotkin.testobd.obd.commands.protocol.SelectProtocolCommand
import com.kurotkin.testobd.obd.commands.protocol.TimeoutCommand
import com.kurotkin.testobd.obd.enums.ObdProtocols
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private var deviceSelectAddress :String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bluetooth()
        if (deviceSelectAddress != null) Toast.makeText(this, "Выбран $deviceSelectAddress", Toast.LENGTH_SHORT).show()
    }

    fun bluetooth(){
        val deviceStrs = ArrayList<String>()
        val devices = ArrayList<String>()

        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = btAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                deviceStrs.add("${device.name} \n${device.address}")
                devices.add(device.address)
            }
        }

        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)

        val adapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs.toArray(arrayOfNulls(deviceStrs.size)))

        alertDialog.setSingleChoiceItems(adapter, -1) { dialog, _ ->
            dialog.dismiss()
            val position: Int = (dialog as AlertDialog).getListView().getCheckedItemPosition()
            val deviceAddress = devices[position]
            deviceSelectAddress = deviceAddress
            if (deviceSelectAddress != null){
                Toast.makeText(this, "Выбран $deviceSelectAddress", Toast.LENGTH_SHORT).show()
                bluetoothWork(deviceSelectAddress!!)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        text.text = it
                    }, {}, {}, {})
            }
        }

        alertDialog.setTitle("Choose Bluetooth device")
        alertDialog.show()
    }

    fun bluetoothWork(deviceSelectAddress: String): Observable<String> =
        Observable.create{ str ->
            try {
                val btAdapter = BluetoothAdapter.getDefaultAdapter()
                val device: BluetoothDevice = btAdapter.getRemoteDevice(deviceSelectAddress)
                val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                val socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                socket.connect()

                EchoOffCommand().run(socket.inputStream, socket.outputStream)
                LineFeedOffCommand().run(socket.inputStream, socket.outputStream)
                TimeoutCommand(125).run(socket.inputStream, socket.outputStream)
                SelectProtocolCommand(ObdProtocols.AUTO).run(socket.inputStream, socket.outputStream)

                val engineRpmCommand = RPMCommand()
                val speedCommand = SpeedCommand()
                while (!Thread.currentThread().isInterrupted) {
                    engineRpmCommand.run(socket.inputStream, socket.outputStream)
                    speedCommand.run(socket.inputStream, socket.outputStream)
                    str.onNext("Speed: " + speedCommand.formattedResult + " RPM: " + engineRpmCommand.formattedResult)
                }

            } catch (e: Exception){
                str.onNext(e.toString())
                str.onComplete()
            }

            str.onComplete()
        }

}