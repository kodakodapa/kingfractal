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

    // Rendering parameters
    private val widthSpinner = JSpinner(SpinnerNumberModel(800, 100, 2048, 100))
    private val heightSpinner = JSpinner(SpinnerNumberModel(600, 100, 2048, 100))
    private val samplesSpinner = JSpinner(SpinnerNumberModel(10, 1, 100, 5))
    private val maxDepthSpinner = JSpinner(SpinnerNumberModel(10, 1, 50, 1))

    // Rendering controls
    private val renderButton = JButton("Render Scene")
    private val statusLabel = JLabel("Ready")

    private val rayTracer = RayTracer()

    // Camera instance for WASD controls
    private var camera: Camera? = null

    enum class SceneType(val displayName: String) {
        SIMPLE_SPHERES("Simple Spheres"),
        MATERIAL_TEST("Material Test"),
        RANDOM_SPHERES("Random Spheres")
    }

    init {
        setupUI()
        loadSceneTypes()
        setupKeyHandling()
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
        helpPanel.add(JLabel("<html><small>Use WASD to move camera (W/S=forward/back, A/D=turn left/right). Ctrl+WASD to auto-render. Render scene first!</small></html>"))
        controlsPanel.add(helpPanel)

        add(controlsPanel, BorderLayout.CENTER)
    }

    private fun setupKeyHandling() {
        isFocusable = true

        // Use key bindings instead of key listener for better focus handling
        val inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val actionMap = actionMap

        // Define actions for camera movement
        val moveForwardAction = object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                camera?.let { cam ->
                    cam.moveForward()
                    updateSpinnersFromCamera(cam)
                    statusLabel.text = "Camera moved forward"
                }
            }
        }

        val moveBackwardAction = object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                camera?.let { cam ->
                    cam.moveBackward()
                    updateSpinnersFromCamera(cam)
                    statusLabel.text = "Camera moved backward"
                }
            }
        }

        val turnLeftAction = object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                camera?.let { cam ->
                    cam.turnLeft()
                    updateSpinnersFromCamera(cam)
                    statusLabel.text = "Camera turned left"
                }
            }
        }

        val turnRightAction = object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                camera?.let { cam ->
                    cam.turnRight()
                    updateSpinnersFromCamera(cam)
                    statusLabel.text = "Camera turned right"
                }
            }
        }

        val autoMoveForwardAction = object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                camera?.let { cam ->
                    cam.moveForward()
                    updateSpinnersFromCamera(cam)
                    autoRender()
                }
            }
        }

        val autoMoveBackwardAction = object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                camera?.let { cam ->
                    cam.moveBackward()
                    updateSpinnersFromCamera(cam)
                    autoRender()
                }
            }
        }

        val autoTurnLeftAction = object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                camera?.let { cam ->
                    cam.turnLeft()
                    updateSpinnersFromCamera(cam)
                    autoRender()
                }
            }
        }

        val autoTurnRightAction = object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                camera?.let { cam ->
                    cam.turnRight()
                    updateSpinnersFromCamera(cam)
                    autoRender()
                }
            }
        }

        // Bind keys to actions
        inputMap.put(KeyStroke.getKeyStroke('W'), "moveForward")
        inputMap.put(KeyStroke.getKeyStroke('w'), "moveForward")
        actionMap.put("moveForward", moveForwardAction)

        inputMap.put(KeyStroke.getKeyStroke('S'), "moveBackward")
        inputMap.put(KeyStroke.getKeyStroke('s'), "moveBackward")
        actionMap.put("moveBackward", moveBackwardAction)

        inputMap.put(KeyStroke.getKeyStroke('A'), "turnLeft")
        inputMap.put(KeyStroke.getKeyStroke('a'), "turnLeft")
        actionMap.put("turnLeft", turnLeftAction)

        inputMap.put(KeyStroke.getKeyStroke('D'), "turnRight")
        inputMap.put(KeyStroke.getKeyStroke('d'), "turnRight")
        actionMap.put("turnRight", turnRightAction)

        // Ctrl+WASD for auto-rendering
        inputMap.put(KeyStroke.getKeyStroke("ctrl W"), "autoMoveForward")
        inputMap.put(KeyStroke.getKeyStroke("ctrl w"), "autoMoveForward")
        actionMap.put("autoMoveForward", autoMoveForwardAction)

        inputMap.put(KeyStroke.getKeyStroke("ctrl S"), "autoMoveBackward")
        inputMap.put(KeyStroke.getKeyStroke("ctrl s"), "autoMoveBackward")
        actionMap.put("autoMoveBackward", autoMoveBackwardAction)

        inputMap.put(KeyStroke.getKeyStroke("ctrl A"), "autoTurnLeft")
        inputMap.put(KeyStroke.getKeyStroke("ctrl a"), "autoTurnLeft")
        actionMap.put("autoTurnLeft", autoTurnLeftAction)

        inputMap.put(KeyStroke.getKeyStroke("ctrl D"), "autoTurnRight")
        inputMap.put(KeyStroke.getKeyStroke("ctrl d"), "autoTurnRight")
        actionMap.put("autoTurnRight", autoTurnRightAction)

        // Add mouse listener to show focus status
        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                requestFocusInWindow()
                statusLabel.text = "Panel focused - WASD controls active"
            }
        })
    }

    private fun updateSpinnersFromCamera(cam: Camera) {
        cameraXSpinner.value = cam.position.x
        cameraYSpinner.value = cam.position.y
        cameraZSpinner.value = cam.position.z
        targetXSpinner.value = cam.target.x
        targetYSpinner.value = cam.target.y
        targetZSpinner.value = cam.target.z
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
                    aspectRatio = width.toDouble() / height.toDouble()
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
}