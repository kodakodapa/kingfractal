package com.github.kodakodapa.kingfractal.gui

import com.github.kodakodapa.kingfractal.colors.ARGBColor
import com.github.kodakodapa.kingfractal.threedimensional.*
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.TitledBorder
import kotlin.concurrent.thread

class RayTracingPanel(private val onImageGenerated: (BufferedImage) -> Unit) : JPanel() {

    // Scene selection
    private val sceneCombo = JComboBox<SceneType>()

    // Camera parameters
    private val cameraXSpinner = JSpinner(SpinnerNumberModel(0.0, -10.0, 10.0, 0.1))
    private val cameraYSpinner = JSpinner(SpinnerNumberModel(2.0, -10.0, 10.0, 0.1))
    private val cameraZSpinner = JSpinner(SpinnerNumberModel(5.0, -10.0, 10.0, 0.1))
    private val targetXSpinner = JSpinner(SpinnerNumberModel(0.0, -10.0, 10.0, 0.1))
    private val targetYSpinner = JSpinner(SpinnerNumberModel(0.0, -10.0, 10.0, 0.1))
    private val targetZSpinner = JSpinner(SpinnerNumberModel(0.0, -10.0, 10.0, 0.1))
    private val fovSpinner = JSpinner(SpinnerNumberModel(45.0, 10.0, 120.0, 5.0))

    // Camera speed controls
    private val moveSpeedSpinner = JSpinner(SpinnerNumberModel(0.02, 0.001, 1.0, 0.01))
    private val turnSpeedSpinner = JSpinner(SpinnerNumberModel(0.01, 0.001, 0.5, 0.01))

    // Rendering parameters
    private val widthSpinner = JSpinner(SpinnerNumberModel(800, 100, 2048, 100))
    private val heightSpinner = JSpinner(SpinnerNumberModel(600, 100, 2048, 100))
    private val samplesSpinner = JSpinner(SpinnerNumberModel(10, 1, 100, 5))
    private val maxDepthSpinner = JSpinner(SpinnerNumberModel(10, 1, 50, 1))

    // Rendering controls
    private val renderButton = JButton("Render Scene")
    private val statusLabel = JLabel("Ready")

    private val rayTracer = GPURayTracer()

    // Camera instance for WASD controls
    private var camera: Camera? = null

    // Key state tracking for continuous movement
    private val pressedKeys = mutableSetOf<Int>()
    private var movementTimer: Timer? = null
    private var autoRenderEnabled = false
    private var autoRenderToggle = false

    enum class SceneType(val displayName: String) {
        SIMPLE_SPHERES("Simple Spheres"),
        MATERIAL_TEST("Material Test"),
        RANDOM_SPHERES("Random Spheres")
    }

    init {
        setupUI()
        loadSceneTypes()
        setupKeyHandling()
        setupSpeedListeners()

        // Initialize GPU ray tracer
        try {
            rayTracer.initialize()
            statusLabel.text = "GPU Ray Tracer ready"
        } catch (e: Exception) {
            statusLabel.text = "GPU initialization failed: ${e.message}"
            e.printStackTrace()
        }
    }

    private fun setupUI() {
        layout = BorderLayout()

        val controlsPanel = JPanel()
        controlsPanel.layout = BoxLayout(controlsPanel, BoxLayout.Y_AXIS)

        // Scene selection panel
        val scenePanel = JPanel(GridBagLayout())
        scenePanel.border = TitledBorder("Scene")
        val gbc1 = GridBagConstraints()
        gbc1.insets = Insets(2, 2, 2, 2)
        gbc1.anchor = GridBagConstraints.WEST

        gbc1.gridx = 0; gbc1.gridy = 0
        scenePanel.add(JLabel("Scene Type:"), gbc1)
        gbc1.gridx = 1
        scenePanel.add(sceneCombo, gbc1)

        controlsPanel.add(scenePanel)

        // Camera parameters panel
        val cameraPanel = JPanel(GridBagLayout())
        cameraPanel.border = TitledBorder("Camera")
        val gbc2 = GridBagConstraints()
        gbc2.insets = Insets(2, 2, 2, 2)
        gbc2.anchor = GridBagConstraints.WEST

        gbc2.gridx = 0; gbc2.gridy = 0
        cameraPanel.add(JLabel("Position X:"), gbc2)
        gbc2.gridx = 1
        cameraPanel.add(cameraXSpinner, gbc2)
        gbc2.gridx = 2
        cameraPanel.add(JLabel("Y:"), gbc2)
        gbc2.gridx = 3
        cameraPanel.add(cameraYSpinner, gbc2)
        gbc2.gridx = 4
        cameraPanel.add(JLabel("Z:"), gbc2)
        gbc2.gridx = 5
        cameraPanel.add(cameraZSpinner, gbc2)

        gbc2.gridx = 0; gbc2.gridy = 1
        cameraPanel.add(JLabel("Target X:"), gbc2)
        gbc2.gridx = 1
        cameraPanel.add(targetXSpinner, gbc2)
        gbc2.gridx = 2
        cameraPanel.add(JLabel("Y:"), gbc2)
        gbc2.gridx = 3
        cameraPanel.add(targetYSpinner, gbc2)
        gbc2.gridx = 4
        cameraPanel.add(JLabel("Z:"), gbc2)
        gbc2.gridx = 5
        cameraPanel.add(targetZSpinner, gbc2)

        gbc2.gridx = 0; gbc2.gridy = 2
        cameraPanel.add(JLabel("Field of View:"), gbc2)
        gbc2.gridx = 1
        cameraPanel.add(fovSpinner, gbc2)

        gbc2.gridx = 0; gbc2.gridy = 3
        cameraPanel.add(JLabel("Move Speed:"), gbc2)
        gbc2.gridx = 1
        cameraPanel.add(moveSpeedSpinner, gbc2)
        gbc2.gridx = 2
        cameraPanel.add(JLabel("Turn Speed:"), gbc2)
        gbc2.gridx = 3
        cameraPanel.add(turnSpeedSpinner, gbc2)

        controlsPanel.add(cameraPanel)

        // Rendering parameters panel
        val renderingPanel = JPanel(GridBagLayout())
        renderingPanel.border = TitledBorder("Rendering")
        val gbc3 = GridBagConstraints()
        gbc3.insets = Insets(2, 2, 2, 2)
        gbc3.anchor = GridBagConstraints.WEST

        gbc3.gridx = 0; gbc3.gridy = 0
        renderingPanel.add(JLabel("Width:"), gbc3)
        gbc3.gridx = 1
        renderingPanel.add(widthSpinner, gbc3)
        gbc3.gridx = 2
        renderingPanel.add(JLabel("Height:"), gbc3)
        gbc3.gridx = 3
        renderingPanel.add(heightSpinner, gbc3)

        gbc3.gridx = 0; gbc3.gridy = 1
        renderingPanel.add(JLabel("Samples:"), gbc3)
        gbc3.gridx = 1
        renderingPanel.add(samplesSpinner, gbc3)
        gbc3.gridx = 2
        renderingPanel.add(JLabel("Max Depth:"), gbc3)
        gbc3.gridx = 3
        renderingPanel.add(maxDepthSpinner, gbc3)

        controlsPanel.add(renderingPanel)

        // Render button panel
        val buttonPanel = JPanel(FlowLayout())
        buttonPanel.add(renderButton)
        renderButton.addActionListener { renderScene() }

        controlsPanel.add(buttonPanel)

        // Status panel
        val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        statusPanel.add(statusLabel)
        controlsPanel.add(statusPanel)

        // Controls help panel
        val helpPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        helpPanel.add(JLabel("<html><small><b>CLICK HERE FIRST</b>, then: WASD=move/turn, QE=look up/down, R=auto-render, +/-=speed, 0=reset speed</small></html>"))
        controlsPanel.add(helpPanel)

        add(controlsPanel, BorderLayout.CENTER)
    }

    private fun setupKeyHandling() {
        // Make the panel focusable and request focus
        isFocusable = true
        requestFocusInWindow()

        // Add a simple key listener that should definitely work
        addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                println("Key pressed: ${e.keyChar} (${e.keyCode})") // Debug output

                val keyCode = e.keyCode

                // Handle special keys immediately
                when (keyCode) {
                    java.awt.event.KeyEvent.VK_R -> {
                        autoRenderToggle = !autoRenderToggle
                        statusLabel.text = if (autoRenderToggle) "Auto-render: ON" else "Auto-render: OFF"
                        return
                    }
                    java.awt.event.KeyEvent.VK_PLUS, java.awt.event.KeyEvent.VK_EQUALS -> {
                        camera?.let { cam ->
                            cam.increaseSpeed()
                            updateSpeedSpinners(cam)
                            statusLabel.text = "Speed increased: move=${String.format("%.3f", cam.moveSpeed)}, turn=${String.format("%.3f", cam.turnSpeed)}"
                        }
                        return
                    }
                    java.awt.event.KeyEvent.VK_MINUS -> {
                        camera?.let { cam ->
                            cam.decreaseSpeed()
                            updateSpeedSpinners(cam)
                            statusLabel.text = "Speed decreased: move=${String.format("%.3f", cam.moveSpeed)}, turn=${String.format("%.3f", cam.turnSpeed)}"
                        }
                        return
                    }
                    java.awt.event.KeyEvent.VK_0 -> {
                        camera?.let { cam ->
                            cam.resetSpeed()
                            updateSpeedSpinners(cam)
                            statusLabel.text = "Speed reset to default"
                        }
                        return
                    }
                }

                // Add movement keys to pressed set
                if (isMovementKey(keyCode) && !pressedKeys.contains(keyCode)) {
                    pressedKeys.add(keyCode)
                    statusLabel.text = "Key pressed: ${getKeyName(keyCode)}"

                    // Start movement timer if not already running
                    if (movementTimer == null) {
                        startMovementTimer()
                    }
                    updateStatusForKeys()
                }
            }

            override fun keyReleased(e: java.awt.event.KeyEvent) {
                println("Key released: ${e.keyChar} (${e.keyCode})") // Debug output

                val keyCode = e.keyCode
                if (isMovementKey(keyCode)) {
                    pressedKeys.remove(keyCode)

                    // Stop timer if no movement keys are pressed
                    if (pressedKeys.isEmpty()) {
                        stopMovementTimer()
                    }
                    updateStatusForKeys()
                }
            }
        })

        // Add focus listeners to help with debugging
        addFocusListener(object : java.awt.event.FocusListener {
            override fun focusGained(e: java.awt.event.FocusEvent?) {
                statusLabel.text = "Panel has focus - WASDQE controls active"
                println("Panel gained focus") // Debug output
            }

            override fun focusLost(e: java.awt.event.FocusEvent?) {
                statusLabel.text = "Panel lost focus - click to reactivate"
                println("Panel lost focus") // Debug output
            }
        })

        // Add mouse listener for focus
        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                requestFocusInWindow()
                println("Mouse clicked - requesting focus") // Debug output
            }
        })
    }

    private fun isMovementKey(keyCode: Int): Boolean {
        return keyCode in setOf(
            java.awt.event.KeyEvent.VK_W,
            java.awt.event.KeyEvent.VK_S,
            java.awt.event.KeyEvent.VK_A,
            java.awt.event.KeyEvent.VK_D,
            java.awt.event.KeyEvent.VK_Q,
            java.awt.event.KeyEvent.VK_E
        )
    }

    private fun getKeyName(keyCode: Int): String {
        return when (keyCode) {
            java.awt.event.KeyEvent.VK_W -> "W"
            java.awt.event.KeyEvent.VK_S -> "S"
            java.awt.event.KeyEvent.VK_A -> "A"
            java.awt.event.KeyEvent.VK_D -> "D"
            java.awt.event.KeyEvent.VK_Q -> "Q"
            java.awt.event.KeyEvent.VK_E -> "E"
            else -> "Unknown"
        }
    }

    private fun startMovementTimer() {
        movementTimer = Timer(16) { // ~60 FPS movement updates
            processContinuousMovement()
        }
        movementTimer?.start()
    }

    private fun stopMovementTimer() {
        movementTimer?.stop()
        movementTimer = null
    }

    private fun processContinuousMovement() {
        camera?.let { cam ->
            var moved = false

            // Process each pressed key
            for (keyCode in pressedKeys) {
                when (keyCode) {
                    java.awt.event.KeyEvent.VK_W -> {
                        cam.moveForward()
                        moved = true
                    }
                    java.awt.event.KeyEvent.VK_S -> {
                        cam.moveBackward()
                        moved = true
                    }
                    java.awt.event.KeyEvent.VK_A -> {
                        cam.turnLeft()
                        moved = true
                    }
                    java.awt.event.KeyEvent.VK_D -> {
                        cam.turnRight()
                        moved = true
                    }
                    java.awt.event.KeyEvent.VK_Q -> {
                        cam.pitchUp()
                        moved = true
                    }
                    java.awt.event.KeyEvent.VK_E -> {
                        cam.pitchDown()
                        moved = true
                    }
                }
            }

            if (moved) {
                SwingUtilities.invokeLater {
                    updateSpinnersFromCamera(cam)
                    if (autoRenderToggle) {
                        autoRender()
                    }
                }
            }
        }
    }

    private fun updateStatusForKeys() {
        if (pressedKeys.isEmpty()) {
            val autoStatus = if (autoRenderToggle) " - Auto-render: ON" else ""
            statusLabel.text = "Ready$autoStatus"
        } else {
            val keyNames = pressedKeys.mapNotNull { keyCode ->
                when (keyCode) {
                    java.awt.event.KeyEvent.VK_W -> "W"
                    java.awt.event.KeyEvent.VK_S -> "S"
                    java.awt.event.KeyEvent.VK_A -> "A"
                    java.awt.event.KeyEvent.VK_D -> "D"
                    java.awt.event.KeyEvent.VK_Q -> "Q"
                    java.awt.event.KeyEvent.VK_E -> "E"
                    else -> null
                }
            }.joinToString("+")

            val autoStatus = if (autoRenderToggle) " (auto-render)" else ""
            statusLabel.text = "Moving: $keyNames$autoStatus"
        }
    }

    private fun updateSpinnersFromCamera(cam: Camera) {
        cameraXSpinner.value = cam.position.x
        cameraYSpinner.value = cam.position.y
        cameraZSpinner.value = cam.position.z
        targetXSpinner.value = cam.target.x
        targetYSpinner.value = cam.target.y
        targetZSpinner.value = cam.target.z
    }

    private fun updateSpeedSpinners(cam: Camera) {
        moveSpeedSpinner.value = cam.moveSpeed
        turnSpeedSpinner.value = cam.turnSpeed
    }

    private fun autoRender() {
        if (!renderButton.isEnabled) return
        statusLabel.text = "Auto-rendering (Ctrl+WASD)..."
        renderScene()
    }

    private fun loadSceneTypes() {
        sceneCombo.removeAllItems()
        SceneType.values().forEach { sceneCombo.addItem(it) }
        sceneCombo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int,
                isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is SceneType) {
                    text = value.displayName
                }
                return this
            }
        }
    }

    private fun renderScene() {
        renderButton.isEnabled = false
        statusLabel.text = "Rendering..."

        thread {
            try {
                val width = widthSpinner.value as Int
                val height = heightSpinner.value as Int
                val samples = samplesSpinner.value as Int
                val maxDepth = maxDepthSpinner.value as Int

                camera = Camera(
                    position = Vector3(
                        cameraXSpinner.value as Double,
                        cameraYSpinner.value as Double,
                        cameraZSpinner.value as Double
                    ),
                    target = Vector3(
                        targetXSpinner.value as Double,
                        targetYSpinner.value as Double,
                        targetZSpinner.value as Double
                    ),
                    up = Vector3.UNIT_Y,
                    fov = fovSpinner.value as Double,
                    aspectRatio = width.toDouble() / height.toDouble(),
                    moveSpeed = moveSpeedSpinner.value as Double,
                    turnSpeed = turnSpeedSpinner.value as Double
                )

                val world = createScene(sceneCombo.selectedItem as SceneType)
                val imageData = rayTracer.render(camera!!, world, width, height, maxDepth, samples)

                // Convert to BufferedImage
                val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val color = imageData[y][x]
                        val rgb = (color.red shl 16) or (color.green shl 8) or color.blue
                        bufferedImage.setRGB(x, y, rgb)
                    }
                }

                SwingUtilities.invokeLater {
                    onImageGenerated(bufferedImage)
                    statusLabel.text = "Rendering complete"
                    renderButton.isEnabled = true
                }

            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    statusLabel.text = "Rendering failed: ${e.message}"
                    renderButton.isEnabled = true
                }
                e.printStackTrace()
            }
        }
    }

    private fun createScene(sceneType: SceneType): HittableList {
        val world = HittableList()

        when (sceneType) {
            SceneType.SIMPLE_SPHERES -> {
                // Ground
                world.add(Sphere(
                    Vector3(0.0, -100.5, -1.0),
                    100.0,
                    Material(ARGBColor(255, 128, 128, 128))
                ))

                // Center sphere
                world.add(Sphere(
                    Vector3(0.0, 0.0, -1.0),
                    0.5,
                    Material(ARGBColor(255, 255, 100, 100))
                ))

                // Left sphere
                world.add(Sphere(
                    Vector3(-1.0, 0.0, -1.0),
                    0.5,
                    Material(ARGBColor(255, 100, 255, 100))
                ))

                // Right sphere
                world.add(Sphere(
                    Vector3(1.0, 0.0, -1.0),
                    0.5,
                    Material(ARGBColor(255, 100, 100, 255))
                ))
            }

            SceneType.MATERIAL_TEST -> {
                // Ground
                world.add(Sphere(
                    Vector3(0.0, -1000.0, 0.0),
                    1000.0,
                    Material(ARGBColor(255, 128, 128, 128))
                ))

                // Large sphere
                world.add(Sphere(
                    Vector3(0.0, 1.0, 0.0),
                    1.0,
                    Material(ARGBColor(255, 255, 200, 100))
                ))

                // Three smaller spheres
                world.add(Sphere(
                    Vector3(-4.0, 1.0, 0.0),
                    1.0,
                    Material(ARGBColor(255, 255, 100, 100))
                ))

                world.add(Sphere(
                    Vector3(4.0, 1.0, 0.0),
                    1.0,
                    Material(ARGBColor(255, 100, 100, 255))
                ))
            }

            SceneType.RANDOM_SPHERES -> {
                // Ground
                world.add(Sphere(
                    Vector3(0.0, -1000.0, 0.0),
                    1000.0,
                    Material(ARGBColor(255, 128, 128, 128))
                ))

                // Random small spheres
                for (a in -5..5) {
                    for (b in -5..5) {
                        val chooseMat = kotlin.random.Random.nextDouble()
                        val center = Vector3(
                            a + 0.9 * kotlin.random.Random.nextDouble(),
                            0.2,
                            b + 0.9 * kotlin.random.Random.nextDouble()
                        )

                        if ((center - Vector3(4.0, 0.2, 0.0)).length() > 0.9) {
                            val material = when {
                                chooseMat < 0.8 -> {
                                    Material(ARGBColor(
                                        255,
                                        (kotlin.random.Random.nextDouble() * 255).toInt(),
                                        (kotlin.random.Random.nextDouble() * 255).toInt(),
                                        (kotlin.random.Random.nextDouble() * 255).toInt()
                                    ))
                                }
                                else -> {
                                    Material(ARGBColor(
                                        255,
                                        (128 + kotlin.random.Random.nextDouble() * 127).toInt(),
                                        (128 + kotlin.random.Random.nextDouble() * 127).toInt(),
                                        (128 + kotlin.random.Random.nextDouble() * 127).toInt()
                                    ))
                                }
                            }

                            world.add(Sphere(center, 0.2, material))
                        }
                    }
                }

                // Three main spheres
                world.add(Sphere(Vector3(0.0, 1.0, 0.0), 1.0, Material(ARGBColor(255, 255, 200, 100))))
                world.add(Sphere(Vector3(-4.0, 1.0, 0.0), 1.0, Material(ARGBColor(255, 255, 100, 100))))
                world.add(Sphere(Vector3(4.0, 1.0, 0.0), 1.0, Material(ARGBColor(255, 100, 100, 255))))
            }
        }

        return world
    }

    private fun setupSpeedListeners() {
        moveSpeedSpinner.addChangeListener {
            camera?.let { cam ->
                cam.moveSpeed = moveSpeedSpinner.value as Double
            }
        }

        turnSpeedSpinner.addChangeListener {
            camera?.let { cam ->
                cam.turnSpeed = turnSpeedSpinner.value as Double
            }
        }
    }

    fun cleanup() {
        stopMovementTimer()
        rayTracer.cleanup()
    }
}