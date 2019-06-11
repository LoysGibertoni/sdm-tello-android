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
    }

    private fun showToast(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mTello = null
    }
}
