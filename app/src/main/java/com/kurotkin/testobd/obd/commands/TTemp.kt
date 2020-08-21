package com.kurotkin.testobd.obd.commands

class TTemp : ObdCommand("22 1e3f") {
    var temp: Int = 0

    override fun getName() =  "TTemp"

    override fun getFormattedResult() = String.format("%d", temp)

    override fun performCalculations() {
        temp = buffer[1]
    }

    override fun getCalculatedResult(): String {
        return ""
    }
}