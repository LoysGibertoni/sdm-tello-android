package br.edu.ifsp.sdm.tello.vr

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class OpenGLGeometryHelper(
    vertices: FloatArray,
    private val modelMatrix: FloatArray,
    vertexShader: Int,
    fragmentShader: Int,
    textureCoordinates: FloatArray
) {
    // OpenGL Program
    private var openGLProgram: Int

    // Buffers to hold vertices & normals.
    private var vertexBuffer: FloatBuffer

    // OpenGL Parameter Identifiers.
    private var positionOpenGLParam: Int
    private var modelViewProjectionOpenGLParam: Int

    // Texture
    private var textureCoordinatesBuffer: FloatBuffer? = null
    private var textureResourceID = 0
    private var textureOpenGLParam = 0
    private var textureCoordinateOpenGLParam = 0

    init {

        // Create a buffer to store the position of the square.
        val vertexByteBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        vertexByteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = vertexByteBuffer.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        openGLProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(openGLProgram, vertexShader)
        GLES20.glAttachShader(openGLProgram, fragmentShader)
        GLES20.glLinkProgram(openGLProgram)
        GLES20.glUseProgram(openGLProgram)
        Utility.checkGLError(javaClass.simpleName, "Program")

        // Get the OpenGL variable positions.
        positionOpenGLParam = GLES20.glGetAttribLocation(openGLProgram, "a_Position")
        modelViewProjectionOpenGLParam = GLES20.glGetUniformLocation(openGLProgram, "u_MVP")
        Utility.checkGLError(javaClass.simpleName, "Program Parameters")

        setTexture(textureCoordinates)
    }

    /**
     * Optional - applies a texture to the geometry.
     * @param texture - the resource ID
     * @param textureCoordinates - coordinates to map the texture to the geometry.
     */
    private fun setTexture(textureCoordinates: FloatArray) {
        // Create a buffer to store the texture coordinates of the square.
        val textureCoordinatesByteBuffer = ByteBuffer.allocateDirect(textureCoordinates.size * 4)
        textureCoordinatesByteBuffer.order(ByteOrder.nativeOrder())
        textureCoordinatesBuffer = textureCoordinatesByteBuffer.asFloatBuffer().apply {
            put(textureCoordinates)
            position(0)
        }

        // OpenGL Parameters
        textureOpenGLParam = GLES20.glGetUniformLocation(openGLProgram, "u_Texture")
        textureCoordinateOpenGLParam = GLES20.glGetAttribLocation(openGLProgram, "a_TexCoordinate")
        Utility.checkGLError(javaClass.simpleName, "Texture Parameters")
    }

    fun setBitmap(bitmap: Bitmap) {
        if (textureResourceID == 0) {
            textureResourceID = IntArray(1).let { textureHandle ->
                GLES20.glGenTextures(1, textureHandle, 0)
                textureHandle[0]
            }
        }

        if (textureResourceID == 0) {
            throw RuntimeException("Error loading texture.")
        }

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureResourceID)

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    }

    /**
     * Draw the object.
     *
     * Pass through parameters to OpenGL.
     */
    fun draw(
        viewMatrix: FloatArray?,
        projectionMatrix: FloatArray?
    ) {
        if (textureResourceID == 0) {
            // Nothing to draw
            return
        }

        // Use this OpenGL Program
        GLES20.glUseProgram(openGLProgram)

        // Compute Model-View and Model-View-Projection Matrices.
        val modelViewMatrix = FloatArray(16)
        val modelViewProjectionMatrix = FloatArray(16)
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        // Model-View-Projection
        GLES20.glUniformMatrix4fv(
            modelViewProjectionOpenGLParam,
            1,
            false,
            modelViewProjectionMatrix,
            0
        )

        // Vertices
        GLES20.glVertexAttribPointer(
            positionOpenGLParam,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(positionOpenGLParam)

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureResourceID)
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(textureOpenGLParam, 0)
        GLES20.glVertexAttribPointer(
            textureCoordinateOpenGLParam,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            textureCoordinatesBuffer
        )
        GLES20.glEnableVertexAttribArray(textureCoordinateOpenGLParam)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexBuffer.capacity() / 3)
        Utility.checkGLError(javaClass.simpleName, "Draw")
    }

    companion object {
        const val COORDS_PER_VERTEX = 3
    }
}
