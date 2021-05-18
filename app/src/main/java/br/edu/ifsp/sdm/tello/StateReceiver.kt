package br.edu.ifsp.sdm.tello

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class StateReceiver : LifecycleObserver {

    private val socket = DatagramSocket()
    private val compositeDisposable = CompositeDisposable()

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        socket.bind(InetSocketAddress("0.0.0.0", 8890))
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
                .subscribe(this::onReceive, this::onError)
                .also { compositeDisposable.add(it) }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        compositeDisposable.clear()
    }

    private fun onReceive(message: String) {
        Log.d(javaClass.simpleName, "Received: $message")
    }

    private fun onError(error: Throwable) {
        Log.d(javaClass.simpleName, "Error: ${error.message}", error)
    }

    companion object {
        private const val BUFFER_SIZE = 1518
    }
}