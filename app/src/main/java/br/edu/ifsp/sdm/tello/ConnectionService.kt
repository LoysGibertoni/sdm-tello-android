package br.edu.ifsp.sdm.tello

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.net.DatagramPacket
import java.net.DatagramSocket

class ConnectionService : Service() {

    companion object {
        private const val BUFFER_SIZE = 1518
    }

    private val socket: DatagramSocket = DatagramSocket(Constants.PORT)
    private val compositeDisposable = CompositeDisposable()
    private var onMessageReceiveListener: ((String) -> Unit)? = null
    private var onErrorListener: ((Throwable) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        startServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        socket.close()
    }

    override fun onBind(intent: Intent?): IBinder {
        return Tello()
    }

    private fun startServer() {
        Observable.create<String> {
                try {
                    val buffer = ByteArray(BUFFER_SIZE)
                    val packet = DatagramPacket(buffer, buffer.size)
                    while (true) {
                        socket.receive(packet)
                        it.onNext(String(buffer, 0, packet.length))
                    }
                } catch (e: Exception) {
                    it.tryOnError(e)
                }
            }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(this::onMessageReceived)
            .doOnError(this::onError)
            .subscribe()
            .also { compositeDisposable.add(it) }
    }

    private fun sendMessage(message: String) {
        Completable.fromAction {
                socket.send(DatagramPacket(message.toByteArray(), message.length, Constants.HOST, Constants.PORT))
            }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete { onMessageSent(message) }
            .doOnError(this::onError)
            .subscribe()
            .also { compositeDisposable.add(it) }
    }

    private fun onMessageReceived(message: String) {
        Log.d(javaClass.simpleName, "Received: $message")
        onMessageReceiveListener?.invoke(message)
    }

    private fun onMessageSent(message: String) {
        Log.d(javaClass.simpleName, "Sent: $message")
    }

    private fun onError(error: Throwable) {
        Log.d(javaClass.simpleName, "Error: ${error.message}", error)
        onErrorListener?.invoke(error)
    }

    inner class Tello : Binder() {

        fun command() {
            sendMessage("command")
        }

        fun takeOff() {
            sendMessage("takeoff")
        }

        fun land() {
            sendMessage("land")
        }

        fun forward(centimeters: Int) {
            sendMessage("forward $centimeters")
        }

        fun back(centimeters: Int) {
            sendMessage("back $centimeters")
        }

        fun left(centimeters: Int) {
            sendMessage("left $centimeters")
        }

        fun right(centimeters: Int) {
            sendMessage("right $centimeters")
        }

        fun up(centimeters: Int) {
            sendMessage("up $centimeters")
        }

        fun down(centimeters: Int) {
            sendMessage("down $centimeters")
        }

        fun rotateLeft(degrees: Int) {
            sendMessage("ccw $degrees")
        }

        fun rotateRight(degrees: Int) {
            sendMessage("cw $degrees")
        }

        fun setOnMessageReceiveListener(listener: ((String) -> Unit)?) {
            onMessageReceiveListener = listener
        }

        fun setOnErrorListener(listener: ((Throwable) -> Unit)?) {
            onErrorListener = listener
        }
    }
}