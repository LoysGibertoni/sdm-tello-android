package br.edu.ifsp.sdm.tello

/*
 * Copyright (c) 2014 Ville Saarinen

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
 */

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnTouchListener

class OnScreenJoystick(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs), SurfaceHolder.Callback, OnTouchListener {

    private val mJoystick: Bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.joystick)
    private val mHolder: SurfaceHolder = holder
    private val mThread: JoystickThread = JoystickThread()

    private var mKnobBounds: Rect? = null
    private var mKnobX: Int = 0
    private var mKnobY: Int = 0
    private var mKnobSize: Int = 0
    private var mBackgroundSize: Int = 0
    private var mRadius: Float = 0F

    private var mJoystickListener: OnScreenJoystickListener? = null

    private var isAutoCentering = true

    init {
        mHolder.addCallback(this)
        mHolder.setFormat(PixelFormat.TRANSPARENT)

        setZOrderOnTop(true)
        setOnTouchListener(this)

        isEnabled = true
        isAutoCentering = true
    }

    private fun initBounds(pCanvas: Canvas) {
        mBackgroundSize = pCanvas.height
        mKnobSize = Math.round(mBackgroundSize * 0.6f)

        mKnobBounds = Rect()

        mRadius = mBackgroundSize * 0.5f
        mKnobX = Math.round((mBackgroundSize - mKnobSize) * 0.5f)
        mKnobY = Math.round((mBackgroundSize - mKnobSize) * 0.5f)

    }

    fun setJoystickListener(pJoystickListener: OnScreenJoystickListener?) {
        mJoystickListener = pJoystickListener
    }

    override fun surfaceChanged(arg0: SurfaceHolder, arg1: Int, arg2: Int, arg3: Int) {

    }

    override fun surfaceCreated(arg0: SurfaceHolder) {
        mThread.start()

    }

    override fun surfaceDestroyed(arg0: SurfaceHolder) {
        var retry = true
        mThread.setRunning(false)

        while (retry) {
            try {
                // code to kill Thread
                this.mThread.join()
                retry = false
            } catch (e: InterruptedException) {
            }

        }

    }

    fun doDraw(pCanvas: Canvas) {
        if (mKnobBounds == null) {
            initBounds(pCanvas)
        }

        mKnobBounds?.set(mKnobX, mKnobY, mKnobX + mKnobSize, mKnobY + mKnobSize)
        pCanvas.drawBitmap(mJoystick, null, mKnobBounds!!, null)
    }

    override fun onTouch(arg0: View, pEvent: MotionEvent): Boolean {
        val x = pEvent.x
        val y = pEvent.y

        when (pEvent.action) {

            MotionEvent.ACTION_UP -> if (isAutoCentering) {
                mKnobX = Math.round((mBackgroundSize - mKnobSize) * 0.5f)
                mKnobY = Math.round((mBackgroundSize - mKnobSize) * 0.5f)
            }
            else ->
                // Check if coordinates are in bounds. If they aren't move the knob
                // to the closest coordinate inbounds.
                if (checkBounds(x, y)) {
                    mKnobX = Math.round(x - mKnobSize * 0.5f)
                    mKnobY = Math.round(y - mKnobSize * 0.5f)
                } else {
                    val angle = Math.atan2((y - mRadius).toDouble(), (x - mRadius).toDouble())
                    mKnobX =
                        (Math.round(mRadius + (mRadius - mKnobSize * 0.5f) * Math.cos(angle)) - mKnobSize * 0.5f).toInt()
                    mKnobY =
                        (Math.round(mRadius + (mRadius - mKnobSize * 0.5f) * Math.sin(angle)) - mKnobSize * 0.5f).toInt()
                }
        }

        if (mJoystickListener != null) {
            mJoystickListener?.onTouch(
                this,
                (0.5f - mKnobX / (mRadius * 2 - mKnobSize)) * -2,
                (0.5f - mKnobY / (mRadius * 2 - mKnobSize)) * 2
            )

        }

        return true
    }

    private fun checkBounds(pX: Float, pY: Float): Boolean {
        return Math.pow((mRadius - pX).toDouble(), 2.0) + Math.pow((mRadius - pY).toDouble(), 2.0) <= Math
            .pow((mRadius - mKnobSize * 0.5f).toDouble(), 2.0)
    }

    private inner class JoystickThread : Thread() {

        private var running = false

        @Synchronized
        override fun start() {
            if (!running && this.state == State.NEW) {
                running = true
                super.start()
            }
        }

        fun setRunning(pRunning: Boolean) {
            running = pRunning
        }

        override fun run() {
            while (running) {
                // draw everything to the canvas
                var canvas: Canvas? = null
                try {
                    canvas = mHolder.lockCanvas(null)
                    synchronized(mHolder) {
                        // reset canvas
                        canvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                        doDraw(canvas)
                    }
                } catch (e: Exception) {
                } finally {
                    if (canvas != null) {
                        mHolder.unlockCanvasAndPost(canvas)
                    }
                }
            }
        }
    }

}