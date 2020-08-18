package com.kurotkin.testobd.ui.main

import android.R
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModel
import com.kurotkin.testobd.NotFoundDeviceExeption
import io.reactivex.Single
import java.util.*
import kotlin.collections.ArrayList

class MainViewModel : ViewModel() {
    private var deviceSelectAddress :String? = null
    private var carrentTime: Long = Date().time

    fun bluetooth(context: Context): Single<String?>{
        return Single.create{ anser ->
            val deviceStrs = ArrayList<String>()
            val devices = ArrayList<String>()

            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            val pairedDevices = btAdapter.bondedDevices

            if (pairedDevices.size == 0){
                anser.onError(NotFoundDeviceExeption())
            }

            if (pairedDevices.size == 1) {
                val deviceAddress = pairedDevices.first().address
                deviceSelectAddress = deviceAddress
                anser.onSuccess(deviceAddress)
            }

            if (pairedDevices.size > 1) {
                for (device in pairedDevices) {
                    deviceStrs.add("${device.name} \n${device.address}")
                    devices.add(device.address)
                }

                val alertDialog: AlertDialog.Builder = AlertDialog.Builder(context)

                val adapter = ArrayAdapter<String>(context, R.layout.select_dialog_singlechoice,
                    deviceStrs.toArray(arrayOfNulls(deviceStrs.size)))

                alertDialog.setSingleChoiceItems(adapter, -1) { dialog, _ ->
                    dialog.dismiss()
                    val position: Int = (dialog as AlertDialog).getListView().getCheckedItemPosition()
                    val deviceAddress = devices[position]
                    deviceSelectAddress = deviceAddress
                    anser.onSuccess(deviceAddress)
                }

                alertDialog.setTitle("Выбирите Bluetooth устройство")
                alertDialog.show()
            }
        }
    }

    fun controlTime() : String {
        val t = Date().time
        val str  = "${t - carrentTime}"
        carrentTime = t
        return str
    }
}