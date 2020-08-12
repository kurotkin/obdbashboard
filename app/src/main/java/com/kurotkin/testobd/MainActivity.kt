package com.kurotkin.testobd

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.anastr.speedviewlib.AwesomeSpeedometer
import com.github.anastr.speedviewlib.PointerSpeedometer
import com.github.anastr.speedviewlib.SpeedView
import com.kurotkin.testobd.obd.commands.Speed
import com.kurotkin.testobd.obd.commands.SpeedCommand
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
import com.kurotkin.testobd.obd.enums.ObdProtocols
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private var deviceSelectAddress :String? = null
    private lateinit var speedTextView: TextView
    private lateinit var rpmTextView: TextView
    private lateinit var loadTextView: TextView
    private lateinit var airTextView: TextView
    private lateinit var fuelTextView: TextView
    private lateinit var voltageTextView: TextView
    private lateinit var oilTempTextView: TextView
    private lateinit var airTempTextView: TextView
    private lateinit var consumptionRateView: TextView

    private lateinit var speedView: SpeedView
    private lateinit var awesomeSpeedometer: AwesomeSpeedometer
    private lateinit var pointerSpeedometer: PointerSpeedometer

    private var carrentTime: Long = Date().time

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val typeFont: Typeface = Typeface.createFromAsset(assets, "fonts/menlo-regular.ttf")
        speedTextView = findViewById(R.id.speed)
        speedTextView.typeface = typeFont
        rpmTextView = findViewById(R.id.rpm)
        loadTextView = findViewById(R.id.load)
        airTextView = findViewById(R.id.air)
        fuelTextView = findViewById(R.id.fuel)
        voltageTextView = findViewById(R.id.voltage)
        oilTempTextView = findViewById(R.id.oil_temp)
        airTempTextView = findViewById(R.id.air_temp)
        consumptionRateView = findViewById(R.id.consumption_rate)

        speedView = findViewById<SpeedView>(R.id.speedView)
        speedView.setMinMaxSpeed(0F, 150F)
        speedView.speedTo(0F, 100)

        awesomeSpeedometer = findViewById<AwesomeSpeedometer>(R.id.awesomeSpeedometer)
        awesomeSpeedometer.setMinMaxSpeed(0F, 150F)
        //awesomeSpeedometer.setSpeedometerColor(Color.RED)
        awesomeSpeedometer.speedTo(0F, 2000)

        pointerSpeedometer = findViewById<PointerSpeedometer>(R.id.pointerSpeedometer)
        pointerSpeedometer.setMinMaxSpeed(0F, 150F)
        pointerSpeedometer.speedTo(0F, 100)

        carrentTime = Date().time
        text.text = "Инициализация"
        bluetooth()
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
            val t = Date().time
            text.text = "Опрос датчиков"
            this.title = "${controlTime()} ms"
            if (deviceSelectAddress != null){
                Toast.makeText(this, "Выбран $deviceSelectAddress", Toast.LENGTH_SHORT).show()

                bluetoothWork(deviceSelectAddress!!)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if(it != null) write(it)
                        text.text = "Опрос..."
                        this.title = "${controlTime()} ms"
                    }, {
                        text.text = "${it.message}"
                        this.title = "${controlTime()} ms"
                    }, {}, {})
            }
        }

        alertDialog.setTitle("Выбирите Bluetooth устройство")
        alertDialog.show()
    }

    fun controlTime() : String {
        val t = Date().time
        val str  = "${t - carrentTime}"
        carrentTime = t
        return str
    }

    fun write(eparam: EParam){
        speedTextView.text = "$eparam.speed"
        rpmTextView.text = eparam.rpm
        loadTextView.text = eparam.load
        airTextView.text = eparam.massAirFlow
        fuelTextView.text = eparam.fuel
        voltageTextView.text = eparam.voltage
        oilTempTextView.text = eparam.oilTemp
        airTempTextView.text = eparam.airTemperature
        consumptionRateView.text = eparam.consumptionRate

        try {
            val speed = eparam.speed.toFloat()
            speedView.speedTo(speed, 100)
            awesomeSpeedometer.speedTo(speed, 100)
            pointerSpeedometer.speedTo(speed, 100)
        } catch (e: java.lang.Exception){}

    }

    fun bluetoothWork(deviceSelectAddress: String): Observable<EParam> =
        Observable.create{ str ->
            var socket :BluetoothSocket? = null
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
            } catch (e: Exception){
                str.onError(e)
            }

            try {
                val engineRpmCommand = RPMCommand()
                val speedCommand = Speed()
                val loadCommand = LoadCommand()
                val moduleVoltageCommand = ModuleVoltageCommand()
                val massAirFlowCommand = MassAirFlowCommand()
//                val oilTempCommand = OilTempCommand()
                val fuelLevelCommand = FuelLevelCommand()
                val ambientAirTemperatureCommand = AmbientAirTemperatureCommand()
                val consumptionRateCommand = ConsumptionRateCommand()

                while (!Thread.currentThread().isInterrupted && socket != null) {
                    engineRpmCommand.run(socket.inputStream, socket.outputStream)
                    speedCommand.run(socket.inputStream, socket.outputStream)
                    loadCommand.run(socket.inputStream, socket.outputStream)
                    moduleVoltageCommand.run(socket.inputStream, socket.outputStream)
                    massAirFlowCommand.run(socket.inputStream, socket.outputStream)
//                    oilTempCommand.run(socket.inputStream, socket.outputStream)
                    fuelLevelCommand.run(socket.inputStream, socket.outputStream)
                    ambientAirTemperatureCommand.run(socket.inputStream, socket.outputStream)
                    consumptionRateCommand.run(socket.inputStream, socket.outputStream)

//                    str.onNext(EParam(
//                        speed = speedCommand.formattedResult,
//                        rpm = engineRpmCommand.formattedResult,
//                        load = loadCommand.formattedResult,
//                        voltage = moduleVoltageCommand.formattedResult,
//                        massAirFlow = massAirFlowCommand.formattedResult,
//                        oilTemp = oilTempCommand.formattedResult,
//                        fuel = fuelLevelCommand.formattedResult,
//                        airTemperature = ambientAirTemperatureCommand.formattedResult
//                    ))

                    str.onNext(EParam(
                        speed = speedCommand.getSpeed(),
                        rpm = engineRpmCommand.formattedResult,
                        load = loadCommand.formattedResult,
                        voltage = moduleVoltageCommand.formattedResult,
                        massAirFlow = massAirFlowCommand.formattedResult,
                        oilTemp = "0",
                        fuel = fuelLevelCommand.formattedResult,
                        airTemperature = ambientAirTemperatureCommand.formattedResult,
                        consumptionRate = consumptionRateCommand.formattedResult
                    ))
                }

            } catch (e: IOException){

            } catch (e: Exception){
                str.onError(e)
            }

            str.onComplete()
        }

}

