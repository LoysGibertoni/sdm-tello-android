package br.edu.ifsp.sdm.tello

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ServiceConnection {

    private var mTello: ConnectionService.Tello? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindService(Intent(this, ConnectionService::class.java), this, Context.BIND_AUTO_CREATE)

        btConnect.setOnClickListener {
            mTello?.command()
        }
        btTakeOff.setOnClickListener {
            mTello?.takeOff()
        }
        btLand.setOnClickListener {
            mTello?.land()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }

    override fun onServiceConnected(name: ComponentName?, tello: IBinder?) {
        mTello = tello as ConnectionService.Tello
        mTello?.setOnMessageReceiveListener(::showToast)
        mTello?.setOnErrorListener {
            showToast(it.message)
        }
        vsView.onDataUpdateListener = { pitch, roll, yaw, throttle ->
            pitch.takeIf { it > 0 }?.let { mTello?.forward(it) }
            pitch.takeIf { it < 0 }?.let { mTello?.back(Math.abs(it)) }
            roll.takeIf { it > 0 }?.let { mTello?.right(it) }
            roll.takeIf { it < 0 }?.let { mTello?.left(Math.abs(it)) }
            yaw.takeIf { it > 0 }?.let { mTello?.rotateRight(it) }
            yaw.takeIf { it < 0 }?.let { mTello?.rotateLeft(Math.abs(it)) }
            throttle.takeIf { it > 0 }?.let { mTello?.up(it) }
            throttle.takeIf { it < 0 }?.let { mTello?.down(Math.abs(it)) }
        }

        vsView.setOnClickListener {  }
    }

    private fun showToast(message: String?) {
        Log.d("Tello", message)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mTello = null
        vsView.onDataUpdateListener = null
    }
}
