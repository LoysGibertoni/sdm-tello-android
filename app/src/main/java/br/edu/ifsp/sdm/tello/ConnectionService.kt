package br.edu.ifsp.sdm.tello

import android.app.IntentService
import android.content.Intent
import java.net.DatagramPacket
import java.net.DatagramSocket

class ConnectionService : IntentService(this::class.java.simpleName) {

    companion object {
        val EXTRA_MESSAGE = "ConnectionService.Message"
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_MESSAGE)?.let { message ->
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                socket.send(DatagramPacket(message.toByteArray(), message.length, Constants.IP, Constants.PORT))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket?.close()
            }
        }
    }
}