package com.kurotkin.testobd.obd.commands.protocol

class SelectHeaderCommand(val header: String): ObdProtocolCommand("AT SH " + header) {

    override fun getName() = getResult()

    override fun getFormattedResult() = ""
}