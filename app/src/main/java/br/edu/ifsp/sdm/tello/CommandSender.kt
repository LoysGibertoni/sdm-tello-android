package br.edu.ifsp.sdm.tello

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class CommandSender(
    //private val onConnectionChangeListener: OnConnectionChangeListener
) : LifecycleObserver {

    private val socket = DatagramSocket(PORT)
    private val compositeDisposable = CompositeDisposable()

    /*var connected = false
        set(value) {
            if (field != value) {
                when (value) {
                    true -> onConnectionChangeListener.onConnected()
                    false -> onConnectionChangeListener.onDisconnected()
                }
            }
            field = value
        }*/

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        receiveResponses()
        startHeartbeat()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        compositeDisposable.clear()
    }

    private fun startHeartbeat() {
        /*Observable.interval(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.computation())
            .subscribe { command() }*/

    }

    private fun receiveResponses() {
        Observable.create<String> {
            val buffer = ByteArray(BUFFER_SIZE)
            val packet = DatagramPacket(buffer, buffer.size)
            while (true) {
                socket.receive(packet)
                it.onNext(String(buffer, 0, packet.length))
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onMessageReceived, this::onError)
            .also { compositeDisposable.add(it) }
    }

    fun command() {
        send("command")
    }

    fun streamon() {
        send("streamon")
    }

    fun streamoff() {
        send("streamoff")
    }

    fun takeOff() {
        send("takeoff")
    }

    fun land() {
        send("land")
    }

    fun forward(centimeters: Int) {
        send("forward $centimeters")
    }

    fun back(centimeters: Int) {
        send("back $centimeters")
    }

    fun left(centimeters: Int) {
        send("left $centimeters")
    }

    fun right(centimeters: Int) {
        send("right $centimeters")
    }

    fun up(centimeters: Int) {
        send("up $centimeters")
    }

    fun down(centimeters: Int) {
        send("down $centimeters")
    }

    fun rotateLeft(degrees: Int) {
        send("ccw $degrees")
    }

    fun rotateRight(degrees: Int) {
        send("cw $degrees")
    }

    private fun send(command: String) {
        Completable.fromAction {
            socket.send(DatagramPacket(command.toByteArray(), command.length, HOST, PORT))
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onMessageSent(command) }, this::onError)
                .also { compositeDisposable.add(it) }
    }

    private fun onMessageReceived(message: String) {
        Log.d(javaClass.simpleName, "Received: $message")
    }

    private fun onMessageSent(message: String) {
        Log.d(javaClass.simpleName, "Sent: $message")
    }

    private fun onError(error: Throwable) {
        Log.d(javaClass.simpleName, "Error: ${error.message}", error)
    }

    interface OnConnectionChangeListener {
        fun onConnected()
        fun onDisconnected()
    }

    companion object {
        private val HOST = InetAddress.getByName("192.168.10.1")
        private const val PORT = 8889
        private const val BUFFER_SIZE = 1518
    }
}