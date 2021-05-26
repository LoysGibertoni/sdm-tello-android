package br.edu.ifsp.sdm.tello

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), VideoStreamReceiver.OnReceiveListener {

    private lateinit var commandSender: CommandSender
    private lateinit var videoStreamReceiver: VideoStreamReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vsView.onDataUpdateListener = { pitch, roll, yaw, throttle ->
            pitch.takeIf { it > 0 }?.let { this.commandSender.forward(it) }
            pitch.takeIf { it < 0 }?.let { this.commandSender.back(Math.abs(it)) }
            roll.takeIf { it > 0 }?.let { this.commandSender.right(it) }
            roll.takeIf { it < 0 }?.let { this.commandSender.left(Math.abs(it)) }
            yaw.takeIf { it > 0 }?.let { this.commandSender.rotateRight(it) }
            yaw.takeIf { it < 0 }?.let { this.commandSender.rotateLeft(Math.abs(it)) }
            throttle.takeIf { it > 0 }?.let { this.commandSender.up(it) }
            throttle.takeIf { it < 0 }?.let { this.commandSender.down(Math.abs(it)) }
        }

        btConnect.setOnClickListener {
            commandSender.command()
        }
        btTakeOff.setOnClickListener {
            commandSender.streamon()
        }
        btLand.setOnClickListener {
            commandSender.land()
        }

        commandSender = CommandSender().also {
            lifecycle.addObserver(it)
        }
        videoStreamReceiver = VideoStreamReceiver(cacheDir, this).also {
            lifecycle.addObserver(it)
        }
    }

    override fun onFrameReceived(bitmap: Bitmap) = ivImage.setImageBitmap(bitmap)
}
