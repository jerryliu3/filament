/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.filament.transparentrendering

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.opengl.Matrix
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.filament.*
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer.AttributeType
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.android.filament.View
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels

class MainActivity : Activity() {
    // Make sure to initialize Filament first
    // This loads the JNI library needed by most API calls
    companion object {
        init {
            Filament.init()
        }
    }

    // The View we want to render into
    private lateinit var surfaceView: SurfaceView
    // UiHelper is provided by Filament to manage SurfaceView and SurfaceTexture
    private lateinit var uiHelper: UiHelper
    // Choreographer is used to schedule new frames
    private lateinit var choreographer: Choreographer

    // Engine creates and destroys Filament resources
    // Each engine must be accessed from a single thread of your choosing
    // Resources cannot be shared across engines
    private lateinit var engine: Engine
    // A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
    private lateinit var renderer: Renderer
    // A scene holds all the renderable, lights, etc. to be drawn
    private lateinit var scene: Scene
    // A view defines a viewport, a scene and a camera for rendering
    private lateinit var view: View
    // Should be pretty obvious :)
    private lateinit var camera: Camera

    private lateinit var material: Material
    private lateinit var materialInstance: MaterialInstance

    private lateinit var vertexBuffer: VertexBuffer
    private lateinit var indexBuffer: IndexBuffer

    // Filament entity representing a renderable object
    @Entity private var renderable = 0
    @Entity private var light = 0

    // A swap chain is Filament's representation of a surface
    private var swapChain: SwapChain? = null

    // Performs the rendering and schedules new frames
    private val frameScheduler = FrameCallback()

    private val animator = ValueAnimator.ofFloat(0.0f, 360.0f)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        choreographer = Choreographer.getInstance()

        surfaceView = SurfaceView(this)

        val textView = TextView(this).apply {
            val d = resources.displayMetrics.density
            text = "This TextView is under the Filament SurfaceView."
            textSize = 32.0f
            setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
        }
        var seekbar = SeekBar(this).apply{
            val d = resources.displayMetrics.density
            //setPadding((32 * d).toInt(), 0, (32 * d).toInt(), 0)


        }
        seekbar?.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                // write custom code for progress is changed
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
                Toast.makeText(this@MainActivity,
                        "Progress is: " + seek.progress + "%",
                        Toast.LENGTH_SHORT).show()

            }
        })

        var seekbar2 = SeekBar(this).apply{
            val d = resources.displayMetrics.density
            //setPadding((32 * d).toInt(), (32 * d).toInt(), (32 * d).toInt(), 0)
            setProgress(25)
        }

        var seekbar3 = SeekBar(this).apply{
            val d = resources.displayMetrics.density
            setProgress(50)

        }

        var xPos = 1.0f
        var yPos = 1.0f
        seekbar2?.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                // write custom code for progress is changed

                animator.interpolator = LinearInterpolator()
                animator.duration = 1000
                animator.repeatMode = ValueAnimator.RESTART
                animator.repeatCount = 0
                //animator.setFloatValues(0.5f)
                animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                    val transformMatrix = FloatArray(16)
                    override fun onAnimationUpdate(a: ValueAnimator) {
                        xPos = 0.01f + progress/100.0f*359.99f
                        Matrix.setRotateM(transformMatrix, 0, xPos, 1.0f, yPos/xPos, 0.0f)
                        //Matrix.setLookAtM(transformMatrix, 0, a.animatedValue as Float, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
                        val tcm = engine.transformManager
                        tcm.setTransform(tcm.getInstance(renderable), transformMatrix)
                    }
                })
                animator.start()

            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                Toast.makeText(this@MainActivity,
                        "Seek is: " + seek.progress, Toast.LENGTH_SHORT).show()
                animator.start()

            }
        })

        seekbar3?.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                // write custom code for progress is changed

                animator.interpolator = LinearInterpolator()
                animator.duration = 1000
                animator.repeatMode = ValueAnimator.RESTART
                animator.repeatCount = 0
                //animator.setFloatValues(0.5f)
                animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                    val transformMatrix = FloatArray(16)
                    override fun onAnimationUpdate(a: ValueAnimator) {
                        yPos = 0.01f + progress/100.0f*359.99f
                        Matrix.setRotateM(transformMatrix, 0, yPos, xPos/yPos, 1.0f, 0.0f)
                        //Matrix.setLookAtM(transformMatrix, 0, a.animatedValue as Float, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
                        val tcm = engine.transformManager
                        tcm.setTransform(tcm.getInstance(renderable), transformMatrix)
                    }
                })
                animator.start()

            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                Toast.makeText(this@MainActivity,
                        "Seek is: " + seek.progress, Toast.LENGTH_SHORT).show()
                animator.start()

            }
        })

        val layoutParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(30, 1500, 800, 0)
        seekbar.layoutParams = layoutParams
        val layoutParams2 = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams2.setMargins(400, 1500, 400, 0)
        seekbar2.layoutParams = layoutParams2
        val layoutParams3 = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams3.setMargins(800, 1500, 30, 0)
        seekbar3.layoutParams = layoutParams3

        setContentView(FrameLayout(this).apply {
            addView(textView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL
            ))
            addView(seekbar)
            addView(seekbar2)
            addView(seekbar3)
            addView(surfaceView)

        })

        setupSurfaceView()
        setupFilament()
        setupView()
        setupScene()
    }

    private fun setupSurfaceView() {
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = SurfaceCallback()

        // Make the render target transparent
        uiHelper.isOpaque = false

        uiHelper.attachTo(surfaceView)
    }

    private fun setupFilament() {
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera()
    }

    private fun setupView() {
        // Make sure to clear to a fully transparent color
        view.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        // Tell the view which camera we want to use
        view.camera = camera

        // Tell the view which scene we want to render
        view.scene = scene
    }

    private fun setupScene() {
        loadMaterial()
        setupMaterial()
        createMesh()

        // To create a renderable we first create a generic entity
        renderable = EntityManager.get().create()

        // We then create a renderable component on that entity
        // A renderable is made of several primitives; in this case we declare only 1
        // If we wanted each face of the cube to have a different material, we could
        // declare 6 primitives (1 per face) and give each of them a different material
        // instance, setup with different parameters
        RenderableManager.Builder(1)
                // Overall bounding box of the renderable
                .boundingBox(Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
                // Sets the mesh data of the first primitive, 6 faces of 6 indices each
                .geometry(0, PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer, 0, 6 * 6)
                // Sets the material of the first primitive
                .material(0, materialInstance)
                .build(engine, renderable)

        // Add the entity to the scene to render it
        scene.addEntity(renderable)


        // We now need a light, let's create a directional light
        light = EntityManager.get().create()

        // Create a color from a temperature (5,500K)
        val (r, g, b) = Colors.cct(5_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(r, g, b)
                // Intensity of the sun in lux on a clear day
                .intensity(110_000.0f)
                // The direction is normalized on our behalf
                .direction(-1.0f, -1.0f, -1.0f)
                .castShadows(true)
                .build(engine, light)

        // Add the entity to the scene to light it
        scene.addEntity(light)

        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // Since we've defined a light that has the same intensity as the sun, it
        // guarantees a proper exposure
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)

        // Move the camera back to see the object
        camera.lookAt(0.0, 3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)

    }

    private fun loadMaterial() {
        readUncompressedAsset("materials/lit_sphere.filamat").let {
            material = Material.Builder().payload(it, it.remaining()).build(engine)
        }
    }

    private fun setupMaterial() {
        // Create an instance of the material to set different parameters on it
        materialInstance = material.createInstance()
        // Specify that our color is in sRGB so the conversion to linear
        // is done automatically for us. If you already have a linear color
        // you can pass it directly, or use Colors.RgbType.LINEAR
        materialInstance.setParameter("baseColor", Colors.RgbType.SRGB, 1.0f, 0.85f, 0.57f)
        // The default value is always 0, but it doesn't hurt to be clear about our intentions
        // Here we are defining a dielectric material
        materialInstance.setParameter("metallic", 0.0f)
        // We increase the roughness to spread the specular highlights
        materialInstance.setParameter("roughness", 0.3f)

        materialInstance.setParameter("metallicReflectance", 0.5f)


    }

    private fun createMesh() {
        val floatSize = 4
        val shortSize = 2
        // A vertex is a position + a tangent frame:
        // 3 floats for XYZ position, 4 floats for normal+tangents (quaternion)
        val vertexSize = 3 * floatSize + 4 * floatSize

        // Define a vertex and a function to put a vertex in a ByteBuffer
        @Suppress("ArrayInDataClass")
        data class Vertex(val x: Float, val y: Float, val z: Float, val tangents: FloatArray)
        fun ByteBuffer.put(v: Vertex): ByteBuffer {
            putFloat(v.x)
            putFloat(v.y)
            putFloat(v.z)
            v.tangents.forEach { putFloat(it) }
            return this
        }

        // 6 faces, 4 vertices per face
        val vertexCount = 6 * 4

        // Create tangent frames, one per face
        val tfPX = FloatArray(4)
        val tfNX = FloatArray(4)
        val tfPY = FloatArray(4)
        val tfNY = FloatArray(4)
        val tfPZ = FloatArray(4)
        val tfNZ = FloatArray(4)

        MathUtils.packTangentFrame( 0.0f,  1.0f, 0.0f, 0.0f, 0.0f, -1.0f,  1.0f,  0.0f,  0.0f, tfPX)
        MathUtils.packTangentFrame( 0.0f,  1.0f, 0.0f, 0.0f, 0.0f, -1.0f, -1.0f,  0.0f,  0.0f, tfNX)
        MathUtils.packTangentFrame(-1.0f,  0.0f, 0.0f, 0.0f, 0.0f, -1.0f,  0.0f,  1.0f,  0.0f, tfPY)
        MathUtils.packTangentFrame(-1.0f,  0.0f, 0.0f, 0.0f, 0.0f,  1.0f,  0.0f, -1.0f,  0.0f, tfNY)
        MathUtils.packTangentFrame( 0.0f,  1.0f, 0.0f, 1.0f, 0.0f,  0.0f,  0.0f,  0.0f,  1.0f, tfPZ)
        MathUtils.packTangentFrame( 0.0f, -1.0f, 0.0f, 1.0f, 0.0f,  0.0f,  0.0f,  0.0f, -1.0f, tfNZ)

        val vertexData = ByteBuffer.allocate(vertexCount * vertexSize)
                // It is important to respect the native byte order
                .order(ByteOrder.nativeOrder())
                // Face -Z
                .put(Vertex(-0.5f, -0.5f, -0.5f, tfNZ))
                .put(Vertex(-0.5f,  0.5f, -0.5f, tfNZ))
                .put(Vertex( 0.5f,  0.5f, -0.5f, tfNZ))
                .put(Vertex( 0.5f, -0.5f, -0.5f, tfNZ))
                // Face +X
                .put(Vertex( 0.5f, -0.5f, -0.5f, tfPX))
                .put(Vertex( 0.5f,  0.5f, -0.5f, tfPX))
                .put(Vertex( 0.5f,  0.5f,  0.5f, tfPX))
                .put(Vertex( 0.5f, -0.5f,  0.5f, tfPX))
                // Face +Z
                .put(Vertex(-0.5f, -0.5f,  0.5f, tfPZ))
                .put(Vertex( 0.5f, -0.5f,  0.5f, tfPZ))
                .put(Vertex( 0.5f,  0.5f,  0.5f, tfPZ))
                .put(Vertex(-0.5f,  0.5f,  0.5f, tfPZ))
                // Face -X
                .put(Vertex(-0.5f, -0.5f,  0.5f, tfNX))
                .put(Vertex(-0.5f,  0.5f,  0.5f, tfNX))
                .put(Vertex(-0.5f,  0.5f, -0.5f, tfNX))
                .put(Vertex(-0.5f, -0.5f, -0.5f, tfNX))
                // Face -Y
                .put(Vertex(-0.5f, -0.5f,  0.5f, tfNY))
                .put(Vertex(-0.5f, -0.5f, -0.5f, tfNY))
                .put(Vertex( 0.5f, -0.5f, -0.5f, tfNY))
                .put(Vertex( 0.5f, -0.5f,  0.5f, tfNY))
                // Face +Y
                .put(Vertex(-0.5f,  0.5f, -0.5f, tfPY))
                .put(Vertex(-0.5f,  0.5f,  0.5f, tfPY))
                .put(Vertex( 0.5f,  0.5f,  0.5f, tfPY))
                .put(Vertex( 0.5f,  0.5f, -0.5f, tfPY))
                // Make sure the cursor is pointing in the right place in the byte buffer
                .flip()

        // Declare the layout of our mesh
        vertexBuffer = VertexBuffer.Builder()
                .bufferCount(1)
                .vertexCount(vertexCount)
                // Because we interleave position and color data we must specify offset and stride
                // We could use de-interleaved data by declaring two buffers and giving each
                // attribute a different buffer index
                .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0,             vertexSize)
                .attribute(VertexAttribute.TANGENTS, 0, AttributeType.FLOAT4, 3 * floatSize, vertexSize)
                .build(engine)

        // Feed the vertex data to the mesh
        // We only set 1 buffer because the data is interleaved
        vertexBuffer.setBufferAt(engine, 0, vertexData)

        // Create the indices
        val indexData = ByteBuffer.allocate(6 * 2 * 3 * shortSize)
                .order(ByteOrder.nativeOrder())
        repeat(6) {
            val i = (it * 4).toShort()
            indexData
                    .putShort(i).putShort((i + 1).toShort()).putShort((i + 2).toShort())
                    .putShort(i).putShort((i + 2).toShort()).putShort((i + 3).toShort())
        }
        indexData.flip()

        // 6 faces, 2 triangles per face,
        indexBuffer = IndexBuffer.Builder()
                .indexCount(vertexCount * 2)
                .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                .build(engine)
        indexBuffer.setBuffer(engine, indexData)
    }

    private fun startAnimation() {
        // Animate the triangle
        animator.interpolator = LinearInterpolator()
        animator.duration = 1000
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = 0
        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            val transformMatrix = FloatArray(16)
            override fun onAnimationUpdate(a: ValueAnimator) {
                Matrix.setRotateM(transformMatrix, 0, a.animatedValue as Float, 0.0f, 1.0f, 0.0f)
                val tcm = engine.transformManager
                tcm.setTransform(tcm.getInstance(renderable), transformMatrix)
            }
        })
        animator.start()
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameScheduler)
        animator.start()
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop the animation and any pending frame
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel();

        // Always detach the surface before destroying the engine
        uiHelper.detach()

        // Cleanup all resources
        engine.destroyEntity(renderable)
        engine.destroyRenderer(renderer)
        engine.destroyVertexBuffer(vertexBuffer)
        engine.destroyIndexBuffer(indexBuffer)
        engine.destroyMaterial(material)
        engine.destroyMaterialInstance(materialInstance)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCamera(camera)

        // Engine.destroyEntity() destroys Filament related resources only
        // (components), not the entity itself
        val entityManager = EntityManager.get()
        entityManager.destroy(renderable)

        // Destroying the engine will free up any resource you may have forgotten
        // to destroy, but it's recommended to do the cleanup properly
        engine.destroy()
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // Schedule the next frame
            choreographer.postFrameCallback(this)

            // This check guarantees that we have a swap chain
            if (uiHelper.isReadyToRender) {
                // If beginFrame() returns false you should skip the frame
                // This means you are sending frames too quickly to the GPU
                if (renderer.beginFrame(swapChain!!)) {
                    renderer.render(view)
                    renderer.endFrame()
                }
            }
        }
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface, uiHelper.swapChainFlags)
        }

        override fun onDetachedFromSurface() {
            swapChain?.let {
                engine.destroySwapChain(it)
                // Required to ensure we don't return before Filament is done executing the
                // destroySwapChain command, otherwise Android might destroy the Surface
                // too early
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            val zoom = 1.5
            val aspect = width.toDouble() / height.toDouble()
            camera.setProjection(Camera.Projection.ORTHO,
                    -aspect * zoom, aspect * zoom, -zoom, zoom, 0.0, 10.0)

            view.viewport = Viewport(0, 0, width, height)
        }
    }

    private fun readUncompressedAsset(assetName: String): ByteBuffer {
        assets.openFd(assetName).use { fd ->
            val input = fd.createInputStream()
            val dst = ByteBuffer.allocate(fd.length.toInt())

            val src = Channels.newChannel(input)
            src.read(dst)
            src.close()

            return dst.apply { rewind() }
        }
    }
}
