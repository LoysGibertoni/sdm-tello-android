package br.edu.ifsp.sdm.tello.vr

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import br.edu.ifsp.sdm.tello.CommandSender
import br.edu.ifsp.sdm.tello.R
import br.edu.ifsp.sdm.tello.VideoStreamReceiver
import com.google.vr.sdk.base.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_vr_main.*
import javax.microedition.khronos.egl.EGLConfig
import kotlin.math.absoluteValue

class VrMainActivity : GvrActivity(), GvrView.StereoRenderer, VideoStreamReceiver.OnReceiveListener {

    private val commandSender by lazy { CommandSender() }
    private val videoStreamReceiver by lazy { VideoStreamReceiver(commandSender, cacheDir, this) } 

    private val camera = FloatArray(16)
    private val viewMatrix by lazy {
        FloatArray(16).apply {
            //Set the size and placement of the virtual screen.
            Matrix.setIdentityM(this, 0)
            val screenSize = resources.displayMetrics.density * 1.5f //Virtual screen height in meters.
            val aspectRatio = 960f / 720f //Image will be stretched to this ratio.
            Matrix.scaleM(this, 0, screenSize, screenSize / aspectRatio, 1f)
            Matrix.translateM(this, 0, 0.0f, 0.0f, -4f)
        }
    }
    private var textureVertexShader: Int = 0
    private var textureFragmentShader: Int = 0
    private lateinit var imageFull: OpenGLGeometryHelper
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vr_main)

        gvrView = gvr_view.apply {
            setEGLConfigChooser(8, 8, 8, 8, 16, 8)
            setRenderer(this@VrMainActivity)
            setTransitionViewEnabled(false)
            stereoModeEnabled = true
            if (setAsyncReprojectionEnabled(true)) {
                AndroidCompat.setSustainedPerformanceMode(this@VrMainActivity, true)
            }
        }

        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -3f, 0.0f, 1.0f, 0.0f)
        
        commandSender.onCreate()
        videoStreamReceiver.onCreate()
    }

    override fun onStart() {
        super.onStart()
        videoStreamReceiver.onStart()
    }

    override fun onStop() {
        videoStreamReceiver.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        videoStreamReceiver.onDestroy()
        commandSender.onDestroy()
        super.onDestroy()
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source == InputDevice.SOURCE_JOYSTICK && event.action == MotionEvent.ACTION_MOVE) {
            val roll = 100 * event.getAxis(MotionEvent.AXIS_X)
            val pitch = -100 * event.getAxis(MotionEvent.AXIS_Y)
            val throttle = -100 * event.getAxis(MotionEvent.AXIS_RZ)
            val yaw = 100 * event.getAxis(MotionEvent.AXIS_Z)

            commandSender.rc(
                roll.toInt(),
                pitch.toInt(),
                throttle.toInt(),
                yaw.toInt()
            )

            when(event.getAxis(MotionEvent.AXIS_HAT_Y)) {
                -1f -> commandSender.takeOff()
                1f -> commandSender.land()
            }

            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onNewFrame(headTransform: HeadTransform) {
        Log.d(javaClass.simpleName, "onNewFrame: start")

        bitmap?.let {
            imageFull.setBitmap(it)
            bitmap = null
        }

        Log.d(javaClass.simpleName, "onNewFrame: end")
    }

    override fun onDrawEye(eye: Eye) {
        Log.d(javaClass.simpleName, "onDrawEye: start")

        // Change some OpenGL flags?
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Utility.checkGLError(javaClass.simpleName, "colorParam")

        // Apply the eye transformation to the camera.
        //Matrix.multiplyMM(viewMatrix, 0, eye.eyeView, 0, camera, 0)

        // Build ModelView and ModelViewProjection matrices for calculating cube position and light.
        val zNear = 0.1f
        val zFar = 100f
        val perspective = eye.getPerspective(zNear, zFar)

        imageFull.draw(camera, perspective)

        Log.d(javaClass.simpleName, "onDrawEye: end")
    }

    override fun onFinishFrame(viewport: Viewport) {
        Log.d(javaClass.simpleName, "onFinishFrame")
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        Log.d(javaClass.simpleName, "onSurfaceChanged")
    }

    override fun onSurfaceCreated(config: EGLConfig) {
        Log.d(javaClass.simpleName, "onSurfaceCreated: start")

        GLES20.glClearColor(0f, 0f, 0f, 0.5f) // Dark background so text shows up well.

        textureVertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.texture_vertex)
        textureFragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.texture_fragment)

        imageFull = OpenGLGeometryHelper(WorldData.SQUARE_COORDS, viewMatrix, textureVertexShader, textureFragmentShader, WorldData.SQUARE_TEXTURE_COORDS)

        Utility.checkGLError(javaClass.simpleName, "onSurfaceCreated")

        Log.d(javaClass.simpleName, "onSurfaceCreated: end")
    }

    override fun onRendererShutdown() {
        Log.d(javaClass.simpleName, "onRendererShutdown")
    }

    fun loadGLShader(type: Int, resId: Int): Int {
        val code = Utility.readRawTextFile(resources.openRawResource(resId))
        var shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)

        // Get the compilation status.
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(
                javaClass.simpleName,
                "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader)
            )
            GLES20.glDeleteShader(shader)
            shader = 0
        }

        if (shader == 0) {
            throw java.lang.RuntimeException("Error creating shader.")
        }

        return shader
    }

    override fun onFrameReceived(bitmap: Bitmap) {
        this.bitmap = bitmap
    }
}

private fun MotionEvent.getAxis(axis: Int): Float {
    val flat = device.getMotionRange(axis)?.flat ?: 0f
    return getAxisValue(axis).takeIf { it.absoluteValue > flat } ?: 0f
}
