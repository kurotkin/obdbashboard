package com.kurotkin.testobd

import android.R
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Toast
import com.kurotkin.testobd.obd.commands.Speed
import com.kurotkin.testobd.obd.commands.control.ModuleVoltageCommand
import com.kurotkin.testobd.obd.commands.engine.LoadCommand
import com.kurotkin.testobd.obd.commands.engine.MassAirFlowCommand
import com.kurotkin.testobd.obd.commands.engine.RPMCommand
import com.kurotkin.testobd.obd.commands.fuel.ConsumptionRateCommand
import com.kurotkin.testobd.obd.commands.fuel.FuelLevelCommand
import com.kurotkin.testobd.obd.commands.protocol.EchoOffCommand
import com.kurotkin.testobd.obd.commands.protocol.LineFeedOffCommand
import com.kurotkin.testobd.obd.commands.protocol.SelectProtocolCommand
import com.kurotkin.testobd.obd.commands.protocol.TimeoutCommand
import com.kurotkin.testobd.obd.commands.temperature.AmbientAirTemperatureCommand
import com.kurotkin.testobd.obd.commands.temperature.EngineCoolantTemperatureCommand
import com.kurotkin.testobd.obd.enums.ObdProtocols
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class ObdProvider {

    fun bluetoothWork(deviceSelectAddress: String): Observable<EParam> =
        Observable.create{ str ->
            var socket : BluetoothSocket? = null
            try {
                val btAdapter = BluetoothAdapter.getDefaultAdapter()
                val device: BluetoothDevice = btAdapter.getRemoteDevice(deviceSelectAddress)
                val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                socket.connect()

                EchoOffCommand().run(socket.inputStream, socket.outputStream)                           // "AT E0"
                LineFeedOffCommand().run(socket.inputStream, socket.outputStream)                       // "AT L0"
                TimeoutCommand(200).run(socket.inputStream, socket.outputStream)                // "AT ST " + timeout
                SelectProtocolCommand(ObdProtocols.AUTO).run(socket.inputStream, socket.outputStream)   // "AT SP " + protocol
                //val engineRpmCommand = RPMCommand()
                val speedCommand = Speed()
                val loadCommand = LoadCommand()
                //val moduleVoltageCommand = ModuleVoltageCommand()
                //val massAirFlowCommand = MassAirFlowCommand()
                //val engineCoolantTemperatureCommand = EngineCoolantTemperatureCommand()
                val fuelLevelCommand = FuelLevelCommand()
                //val ambientAirTemperatureCommand = AmbientAirTemperatureCommand()
                //val consumptionRateCommand = ConsumptionRateCommand()

                while (!Thread.currentThread().isInterrupted && socket != null) {
                    //engineRpmCommand.run(socket.inputStream, socket.outputStream)
                    speedCommand.run(socket.inputStream, socket.outputStream)
                    loadCommand.run(socket.inputStream, socket.outputStream)
                    //moduleVoltageCommand.run(socket.inputStream, socket.outputStream)
                    //massAirFlowCommand.run(socket.inputStream, socket.outputStream)
                    //engineCoolantTemperatureCommand.run(socket.inputStream, socket.outputStream)
                    fuelLevelCommand.run(socket.inputStream, socket.outputStream)
                    //ambientAirTemperatureCommand.run(socket.inputStream, socket.outputStream)
                    //consumptionRateCommand.run(socket.inputStream, socket.outputStream)

                    str.onNext(EParam(
                        speed = speedCommand.getSpeed(),
                        rpm = "",
                        load = loadCommand.formattedResult,
                        voltage = "",
                        massAirFlow = "",
                        oilTemp = "",
                        fuel = fuelLevelCommand.formattedResult,
                        airTemperature = "",
                        consumptionRate = ""
                    ))
                }

            } catch (e: IOException){

            } catch (e: Exception){
                str.onError(e)
            }

            str.onComplete()
        }
}