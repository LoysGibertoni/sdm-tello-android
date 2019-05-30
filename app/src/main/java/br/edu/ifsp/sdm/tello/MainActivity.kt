package br.edu.ifsp.sdm.tello

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, ConnectionService::class.java)
            .putExtra(ConnectionService.EXTRA_MESSAGE, "command"))

        findViewById<Button>(R.id.btTakeOff).setOnClickListener {
            startService(Intent(this, ConnectionService::class.java)
                .putExtra(ConnectionService.EXTRA_MESSAGE, "takeoff"))
        }

        findViewById<Button>(R.id.btLand).setOnClickListener {
            startService(Intent(this, ConnectionService::class.java)
                .putExtra(ConnectionService.EXTRA_MESSAGE, "land"))
        }
    }
}
