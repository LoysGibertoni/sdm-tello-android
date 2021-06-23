package br.edu.ifsp.sdm.tello

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.FileObserver
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit

@SuppressLint("NewApi")
class VideoStreamReceiver(
    private val commandSender: CommandSender,
    private val directory: File,
    private val listener: OnReceiveListener
) : FileObserver(directory, CLOSE_WRITE), LifecycleObserver {

    private var fFmpegSession: FFmpegSession? = null
    private val compositeDisposable = CompositeDisposable()

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        startWatching()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        stopWatching()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        startHeartbeat()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        streamOff()
        compositeDisposable.clear()
    }

    private fun streamOn() {
        if (fFmpegSession != null) {
            return
        }

        Completable.timer(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                commandSender.streamon(CommandSender.OnCommandSuccessListener {
                    startFFmpeg()
                })
            }
            .also { compositeDisposable.add(it) }

    }

    private fun streamOff() {
        commandSender.streamoff(CommandSender.OnCommandSuccessListener {
            stopFFmpeg()
        })
    }

    private fun startHeartbeat() {
        Observable.interval(0, 5, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                commandSender.command(CommandSender.OnCommandSuccessListener {
                    streamOn()
                })
            }
            .also { compositeDisposable.add(it) }
    }

    private fun startFFmpeg() {
        val file = directory.resolve(FILE).apply { createNewFile() }
        fFmpegSession = FFmpegKit.executeAsync("-i udp://127.0.0.1:11111?reuse=1 ${file.absolutePath}") {
            when {
                ReturnCode.isSuccess(it.returnCode) -> Log.d("fFmpegSession", "Command successful")
                ReturnCode.isCancel(it.returnCode) -> Log.d("fFmpegSession", "Command cancelled")
                else -> Log.d("fFmpegSession", "Command failed with state ${it.state} and rc ${it.returnCode}.${it.failStackTrace}")
            }
        }
    }

    private fun stopFFmpeg() {
        fFmpegSession?.cancel()
        fFmpegSession = null
        FFmpegKit.cancel()
    }

    override fun onEvent(event: Int, path: String?) {
        path?.let {
            Single.fromCallable { directory.resolve(it) }
                .map {
                    val bytes = it.readBytes()
                    it.delete()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(listener::onFrameReceived) { Log.e(javaClass.simpleName, "Error updating ImageView", it) }
                .also { compositeDisposable.add(it) }
        }
    }

    fun interface OnReceiveListener {
        fun onFrameReceived(bitmap: Bitmap)
    }

    companion object {
        private const val PREVIEW_FPS = 30
        private const val FILE = "frame_%d.bmp"
    }
}