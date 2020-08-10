package com.kurotkin.testobd.obd.commands

class Speed: SpeedCommand() {

    fun getSpeedString() = String.format("%d", metricSpeed)
    fun getSpeed() = metricSpeed
}