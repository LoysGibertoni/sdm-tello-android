package br.edu.ifsp.sdm.tello

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ServiceConnection {

    private var tello: ConnectionService.Tello? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindService(Intent(this, ConnectionService::class.java), this, Context.BIND_AUTO_CREATE)

        btConnect.setOnClickListener {
            tello?.command()
        }
        btTakeOff.setOnClickListener {
            tello?.takeOff()
        }
        btLand.setOnClickListener {
            tello?.land()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }

    override fun onServiceConnected(name: ComponentName?, tello: IBinder?) {
        this.tello = tello as ConnectionService.Tello
        this.tello?.setOnMessageReceiveListener(::showToast)
        this.tello?.setOnErrorListener {
            showToast(it.message)
        }
        vsView.onDataUpdateListener = { pitch, roll, yaw, throttle ->
            pitch.takeIf { it > 0 }?.let { this.tello?.forward(it) }
            pitch.takeIf { it < 0 }?.let { this.tello?.back(Math.abs(it)) }
            roll.takeIf { it > 0 }?.let { this.tello?.right(it) }
            roll.takeIf { it < 0 }?.let { this.tello?.left(Math.abs(it)) }
            yaw.takeIf { it > 0 }?.let { this.tello?.rotateRight(it) }
            yaw.takeIf { it < 0 }?.let { this.tello?.rotateLeft(Math.abs(it)) }
            throttle.takeIf { it > 0 }?.let { this.tello?.up(it) }
            throttle.takeIf { it < 0 }?.let { this.tello?.down(Math.abs(it)) }
        }

        vsView.setOnClickListener {  }
    }

    private fun showToast(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        tello = null
        vsView.onDataUpdateListener = null
    }
}
