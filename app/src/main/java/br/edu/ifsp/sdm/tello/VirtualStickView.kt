package br.edu.ifsp.sdm.tello

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_virtual_stick.view.*
import java.util.*
import kotlin.math.abs
import kotlin.math.sign

class VirtualStickView : LinearLayout {

    companion object {
        const val MIN_MOVEMENT = 20
        const val MAX_MOVEMENT = 50
        const val MIN_ROTATION = 1
        const val MAX_ROTATION = 36
        const val DEAD_ZONE = 0.2F
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

    private fun calculateValue(axis: Float, min: Int, max: Int): Int {
        val axisAbs = abs(axis)
        if (axisAbs < DEAD_ZONE) {
            return 0
        }

        val maxBias = (axisAbs - DEAD_ZONE) / (1 - DEAD_ZONE)
        val minBias = 1 - maxBias
        return ((minBias * min + maxBias * max) * sign(axis)).toInt()
    }

    private fun setUpListeners() {

        joystickLeft.setJoystickListener(object : OnScreenJoystickListener {

            override fun onTouch(joystick: OnScreenJoystick, pX: Float, pY: Float) {
                throttle = calculateValue(pY, MIN_MOVEMENT, MAX_MOVEMENT)
                yaw = calculateValue(pX, MIN_ROTATION, MAX_ROTATION)
            }
        })

        joystickRight.setJoystickListener(object : OnScreenJoystickListener {

            override fun onTouch(joystick: OnScreenJoystick, pX: Float, pY: Float) {
                pitch = calculateValue(pY, MIN_MOVEMENT, MAX_MOVEMENT)
                roll = calculateValue(pX, MIN_MOVEMENT, MAX_MOVEMENT)
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
