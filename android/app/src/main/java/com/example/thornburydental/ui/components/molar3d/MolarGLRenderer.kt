package com.example.thornburydental.ui.components.molar3d

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MolarGLRenderer : GLSurfaceView.Renderer {

    @Volatile
    var model: MolarModel? = null

    @Volatile
    var rotationX: Float = 15.0f

    @Volatile
    var rotationY: Float = -25.0f

    @Volatile
    var zoom: Float = 1.0f

    @Volatile
    var selectedLayer: MolarLayer = MolarLayer.ALL

    private var program = 0
    private var uMVPMatrixHandle = 0
    private var uNormalMatrixHandle = 0
    private var uColorHandle = 0
    private var uLightDirHandle = 0
    private var aPositionHandle = 0
    private var aNormalHandle = 0

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvMatrix = FloatArray(16)
    private val normalMatrix = FloatArray(16)

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uNormalMatrix;
        attribute vec4 aPosition;
        attribute vec3 aNormal;
        varying vec3 vNormal;
        varying vec3 vPosition;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vNormal = normalize((uNormalMatrix * vec4(aNormal, 0.0)).xyz);
            vPosition = vec3(aPosition);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 uColor;
        uniform vec3 uLightDir;
        varying vec3 vNormal;
        varying vec3 vPosition;
        void main() {
            vec3 lightDir = normalize(uLightDir);
            vec3 normal = normalize(vNormal);
            float diff = max(dot(normal, lightDir), 0.0);
            
            // Subtle directional fill light from opposite angle
            vec3 fillLight = normalize(vec3(-0.5, -0.2, 0.8));
            float fill = max(dot(normal, fillLight), 0.0) * 0.25;
            
            vec3 viewDir = vec3(0.0, 0.0, 1.0);
            vec3 reflectDir = reflect(-lightDir, normal);
            float spec = pow(max(dot(viewDir, reflectDir), 0.0), 24.0) * 0.35;
            
            vec3 ambient = 0.45 * uColor.rgb;
            vec3 diffuse = (diff * 0.55 + fill) * uColor.rgb;
            vec3 specular = spec * vec3(1.0, 1.0, 1.0);
            
            gl_FragColor = vec4(ambient + diffuse + specular, uColor.a);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)

            uMVPMatrixHandle = GLES20.glGetUniformLocation(it, "uMVPMatrix")
            uNormalMatrixHandle = GLES20.glGetUniformLocation(it, "uNormalMatrix")
            uColorHandle = GLES20.glGetUniformLocation(it, "uColor")
            uLightDirHandle = GLES20.glGetUniformLocation(it, "uLightDir")
            aPositionHandle = GLES20.glGetAttribLocation(it, "aPosition")
            aNormalHandle = GLES20.glGetAttribLocation(it, "aNormal")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 38.0f, ratio, 0.1f, 100.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val currentModel = model ?: return

        // Camera setup
        val eyeZ = 3.6f / zoom.coerceIn(0.5f, 2.5f)
        Matrix.setLookAtM(viewMatrix, 0, 0.0f, 0.0f, eyeZ, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)

        // Model matrix with user orbit rotations
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationX, 1.0f, 0.0f, 0.0f)
        Matrix.rotateM(modelMatrix, 0, rotationY, 0.0f, 1.0f, 0.0f)

        // MVP Matrix
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        // Normal Matrix (inverse transpose of MV matrix)
        Matrix.invertM(normalMatrix, 0, mvMatrix, 0)
        Matrix.transposeM(normalMatrix, 0, normalMatrix, 0)

        GLES20.glUseProgram(program)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uNormalMatrixHandle, 1, false, normalMatrix, 0)
        // Directional light from front top right
        GLES20.glUniform3f(uLightDirHandle, 0.5f, 0.8f, 1.0f)

        val stride = 6 * 4 // 6 floats (3 pos + 3 norm) * 4 bytes

        currentModel.subMeshes.forEach { subMesh ->
            val color = determineColor(subMesh.material, selectedLayer)

            // Pass color
            GLES20.glUniform4fv(uColorHandle, 1, color, 0)

            val buffer: FloatBuffer = subMesh.vertexBuffer
            buffer.position(0)
            GLES20.glEnableVertexAttribArray(aPositionHandle)
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)

            buffer.position(3)
            GLES20.glEnableVertexAttribArray(aNormalHandle)
            GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, subMesh.vertexCount)
        }

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
    }

    private fun determineColor(material: String, layer: MolarLayer): FloatArray {
        return when (layer) {
            MolarLayer.ALL -> {
                when (material) {
                    "enamel" -> floatArrayOf(0.96f, 0.94f, 0.90f, 1.0f)     // Pearl Enamel
                    "dentin" -> floatArrayOf(0.88f, 0.72f, 0.44f, 1.0f)     // Golden Dentin
                    "cementum" -> floatArrayOf(0.72f, 0.54f, 0.40f, 1.0f)   // Root Cementum
                    else -> floatArrayOf(0.92f, 0.90f, 0.85f, 1.0f)
                }
            }
            MolarLayer.ENAMEL -> {
                if (material == "enamel") {
                    floatArrayOf(1.0f, 0.98f, 0.94f, 1.0f) // Highlighted Enamel
                } else {
                    floatArrayOf(0.7f, 0.6f, 0.5f, 0.15f) // Semi-transparent ghost
                }
            }
            MolarLayer.DENTIN -> {
                if (material == "dentin") {
                    floatArrayOf(0.96f, 0.75f, 0.35f, 1.0f) // Highlighted Dentin Core
                } else {
                    floatArrayOf(0.85f, 0.85f, 0.85f, 0.18f) // Ghost outer shell
                }
            }
            MolarLayer.ROOTS -> {
                if (material == "cementum") {
                    floatArrayOf(0.75f, 0.42f, 0.28f, 1.0f) // Highlighted Root Terracotta
                } else {
                    floatArrayOf(0.9f, 0.9f, 0.9f, 0.18f) // Ghost crown
                }
            }
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
