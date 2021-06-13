package br.edu.ifsp.sdm.tello.vr

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import br.edu.ifsp.sdm.tello.CommandSender
import br.edu.ifsp.sdm.tello.R
import br.edu.ifsp.sdm.tello.VideoStreamReceiver
import com.google.vr.sdk.base.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_vr_main.*
import javax.microedition.khronos.egl.EGLConfig

class VrMainActivity : GvrActivity(), GvrView.StereoRenderer, VideoStreamReceiver.OnReceiveListener {

    private val commandSender by lazy { CommandSender() }
    private val videoStreamReceiver by lazy { VideoStreamReceiver(commandSender, cacheDir, this) } 

    private val camera = FloatArray(16)
    private val viewMatrix by lazy {
        FloatArray(16).apply {
            //Set the size and placement of the virtual screen.
            Matrix.setIdentityM(this, 0)
            val screenSize = resources.displayMetrics.density //Virtual screen height in meters.
            val aspectRatio =
                resources.displayMetrics.widthPixels / resources.displayMetrics.heightPixels //Image will be stretched to this ratio.
            Matrix.scaleM(this, 0, screenSize, screenSize / aspectRatio, 1f)
            Matrix.translateM(this, 0, 0.0f, 0.0f, -4f)
        }
    }
    private var textureVertexShader: Int = 0
    private var textureFragmentShader: Int = 0
    private lateinit var imageFull: OpenGLGeometryHelper
    private lateinit var imageLeft: OpenGLGeometryHelper
    private lateinit var imageRight: OpenGLGeometryHelper
    private var bitmap: Bitmap? = null
    /*private val controllerManager: ControllerManager by lazy { ControllerManager(this, null) }
    private val controller: Controller by lazy {
        controllerManager.controller.apply {
            setEventListener(object : Controller.EventListener() {
                override fun onUpdate() {
                    controller.update()
                    onControllerUpdate()
                }
            })
        }
    }*/

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

    /*private fun onControllerUpdate() {
        val (yaw, pitch, roll) = controller.orientation.toYawPitchRollDegrees(FloatArray(3))
        calculateRotation(yaw)?.let {
            if (it > 0) {
                commandSender.rotateRight(it)
            } else {
                commandSender.rotateLeft(-it)
            }
        }
        pitch.takeIf { it > 0 }?.let { this.commandSender.forward(it) }
        pitch.takeIf { it < 0 }?.let { this.commandSender.back(Math.abs(it)) }
        roll.takeIf { it > 0 }?.let { this.commandSender.right(it) }
        roll.takeIf { it < 0 }?.let { this.commandSender.left(Math.abs(it)) }
        yaw.takeIf { it > 0 }?.let { this.commandSender.rotateRight(it) }
        yaw.takeIf { it < 0 }?.let { this.commandSender.rotateLeft(Math.abs(it)) }
    }*/

    override fun onNewFrame(headTransform: HeadTransform) {
        Log.d(javaClass.simpleName, "onNewFrame: start")

        bitmap?.let {
            imageFull.setBitmap(it)
            imageLeft.setBitmap(it)
            imageRight.setBitmap(it)
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

        when (eye.type) {
            1 -> imageRight
            2 -> imageLeft
            else -> imageFull
        }.draw(camera, perspective)

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
        imageLeft = OpenGLGeometryHelper(WorldData.SQUARE_COORDS, viewMatrix, textureVertexShader, textureFragmentShader, WorldData.SQUARE_TEXTURE_COORDS_LEFT)
        imageRight = OpenGLGeometryHelper(WorldData.SQUARE_COORDS, viewMatrix, textureVertexShader, textureFragmentShader, WorldData.SQUARE_TEXTURE_COORDS_RIGHT)

        Utility.checkGLError(javaClass.simpleName, "onSurfaceCreated")

        Log.d(javaClass.simpleName, "onSurfaceCreated: end")
    }

    override fun onRendererShutdown() {
        Log.d(javaClass.simpleName, "onRendererShutdown")
    }

    /*companion object {
        private const val MIN_MOVEMENT = 20
        private const val MAX_MOVEMENT = 50
        private const val MIN_ROTATION = 1
        private const val MAX_ROTATION = 36
        private const val DEAD_ZONE = 0.2F
        const val MAX_DEGREES = 360F

        private fun calculateValue(axis: Float, min: Int, max: Int): Int? {
            val axisAbs = abs(axis)
            if (axisAbs < DEAD_ZONE) {
                return null
            }

            val maxBias = (axisAbs - DEAD_ZONE) / (1 - DEAD_ZONE)
            val minBias = 1 - maxBias
            return ((minBias * min + maxBias * max) * sign(axis)).toInt()
        }

        fun calculateMovement(axis: Float) = calculateValue(axis, MIN_MOVEMENT, MAX_MOVEMENT)

        fun calculateRotation(axis: Float) = calculateValue(axis, MIN_ROTATION, MAX_ROTATION)
    }*/

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