package br.edu.ifsp.sdm.tello

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_virtual_stick.view.*
import java.util.*

class VirtualStickView : LinearLayout {

    companion object {
        const val MAX_MOVEMENT  = 500
        const val MAX_ROTATION = 3600
    }

    private var sendVirtualStickDataTimer: Timer? = null
    private var sendVirtualStickDataTask: SendVirtualStickDataTask? = null

    private var pitch: Int = 0
    private var roll: Int = 0
    private var yaw: Int = 0
    private var throttle: Int = 0

    var onDataUpdateListener: ((pitch: Int, roll: Int, yaw: Int, throttle: Int) -> Unit)? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.view_virtual_stick, this, true)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setUpListeners()
        sendVirtualStickDataTask = SendVirtualStickDataTask()
        sendVirtualStickDataTimer = Timer()
        sendVirtualStickDataTimer?.schedule(sendVirtualStickDataTask, 0, 200)
    }

    override fun onDetachedFromWindow() {
        sendVirtualStickDataTask?.cancel()
        sendVirtualStickDataTimer?.apply {
            cancel()
            purge()
        }
        sendVirtualStickDataTimer = null
        sendVirtualStickDataTask = null
        tearDownListeners()
        super.onDetachedFromWindow()
    }

    private fun setUpListeners() {
        joystickLeft.setJoystickListener(object : OnScreenJoystickListener {

            override fun onTouch(joystick: OnScreenJoystick, pX: Float, pY: Float) {
                pitch = if (Math.abs(pY) < 0.02) 0 else (MAX_MOVEMENT * pY).toInt()
                roll = if (Math.abs(pX) < 0.02) 0 else (MAX_MOVEMENT * pX).toInt()
            }
        })

        joystickRight.setJoystickListener(object : OnScreenJoystickListener {

            override fun onTouch(joystick: OnScreenJoystick, pX: Float, pY: Float) {
                throttle = if (Math.abs(pY) < 0.02) 0 else (MAX_MOVEMENT * pY).toInt()
                yaw = if (Math.abs(pX) < 0.02) 0 else (MAX_ROTATION * pX).toInt()
            }
        })
    }

    private fun tearDownListeners() {
        joystickLeft.setJoystickListener(null)
        joystickRight.setJoystickListener(null)
    }

    private inner class SendVirtualStickDataTask : TimerTask() {

        override fun run() {
            onDataUpdateListener?.invoke(pitch, roll, yaw, throttle)
        }
    }
}
