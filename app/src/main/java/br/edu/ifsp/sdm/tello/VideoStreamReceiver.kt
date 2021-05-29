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
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.File

@SuppressLint("NewApi")
class VideoStreamReceiver(
    private val commandSender: CommandSender,
    private val directory: File,
    private val listener: OnReceiveListener
) : FileObserver(directory, CLOSE_WRITE), LifecycleObserver {

    private var fFmpegSession: FFmpegSession? = null
    private val compositeDisposable = CompositeDisposable()

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        startWatching()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        stopWatching()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        commandSender.streamon(CommandSender.OnCommandSuccessListener {
            startFFmpeg()
        })
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        commandSender.streamoff(CommandSender.OnCommandSuccessListener {
            stopFFmpeg()
        })
        compositeDisposable.clear()
    }

    private fun startFFmpeg() {
        val file = directory.resolve(FILE).apply { createNewFile() }
        fFmpegSession = FFmpegKit.executeAsync("-y -i udp://127.0.0.1:11111?reuse=1 -vf fps=$PREVIEW_FPS -update 1 ${file.absolutePath}") {
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
            Single.fromCallable { directory.resolve(it).readBytes() }
                .map { BitmapFactory.decodeByteArray(it, 0, it.size) }
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
        private const val PREVIEW_FPS = 15
        private const val FILE = "frame.bmp"
    }
}