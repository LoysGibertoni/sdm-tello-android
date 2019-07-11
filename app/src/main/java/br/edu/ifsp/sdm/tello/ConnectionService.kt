package br.edu.ifsp.sdm.tello

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.net.DatagramPacket
import java.net.DatagramSocket

class ConnectionService : Service() {

    companion object {
        val BUFFER_SIZE = 1518
    }

    private var mSocket: DatagramSocket? = null
    private var mDisposable: Disposable? = null
    private var mOnMessageReceiveListener: ((String) -> Unit)? = null
    private var mOnErrorListener: ((Throwable) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        mSocket = DatagramSocket(Constants.PORT)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposable?.takeIf { !it.isDisposed }?.dispose()
        mDisposable = null
        mSocket?.close()
        mSocket = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Tello()
    }

    private fun startServer() {
        mDisposable = Observable.create<String> {
                try {
                    val buffer = ByteArray(BUFFER_SIZE)
                    val packet = DatagramPacket(buffer, buffer.size)
                    while (true) {
                        mSocket?.receive(packet)
                        it.onNext(String(buffer, 0, packet.length))
                    }
                } catch (e: Exception) {
                    it.tryOnError(e)
                }
            }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(mOnMessageReceiveListener, {
                mOnErrorListener?.invoke(it)
                mDisposable = null
            })
    }

    private fun sendMessage(message: String) {
        if (mDisposable == null) {
            this.startServer()
        }
        val disposable = Completable.fromAction {
                mSocket?.send(DatagramPacket(message.toByteArray(), message.length, Constants.HOST, Constants.PORT))
            }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d("ConnectionService", "Sent $message")
            }, {
                mOnErrorListener?.invoke(it)
                mDisposable?.takeIf { !it.isDisposed }?.dispose()
                mDisposable = null
            })
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
            mOnMessageReceiveListener = listener
        }

        fun setOnErrorListener(listener: ((Throwable) -> Unit)?) {
            mOnErrorListener = listener
        }
    }
}