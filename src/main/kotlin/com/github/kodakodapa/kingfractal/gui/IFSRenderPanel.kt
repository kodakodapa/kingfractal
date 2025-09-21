package com.github.kodakodapa.kingfractal.gui

import com.github.kodakodapa.kingfractal.colors.ARGBPalette
import com.github.kodakodapa.kingfractal.colors.ARGBPaletteRegistry
import com.github.kodakodapa.kingfractal.ifs.IFSRenderer
import com.github.kodakodapa.kingfractal.utils.SierpinskiTriangleParams
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.TitledBorder

/**
 * Panel for IFS fractal rendering controls and parameters
 */
class IFSRenderPanel(private val onImageGenerated: (BufferedImage) -> Unit) : JPanel() {

    // IFS type selection
    private val ifsTypeCombo = JComboBox<IFSType>()

    // Palette selection
    private val paletteCombo = JComboBox<String>()

    // Common parameters
    private val widthSpinner = JSpinner(SpinnerNumberModel(800, 100, 4096, 100))
    private val heightSpinner = JSpinner(SpinnerNumberModel(800, 100, 4096, 100))
    private val zoomSpinner = JSpinner(SpinnerNumberModel(1.0, 0.1, 10.0, 0.1))
    private val centerXSpinner = JSpinner(SpinnerNumberModel(0.0, -5.0, 5.0, 0.01))
    private val centerYSpinner = JSpinner(SpinnerNumberModel(0.0, -5.0, 5.0, 0.01))
    private val iterationsSpinner = JSpinner(SpinnerNumberModel(100000, 1000, 10000000, 10000))

    // Sierpinski-specific parameters
    private val pointSizeSpinner = JSpinner(SpinnerNumberModel(1, 1, 10, 1))
    private val sierpinskiParamsPanel = JPanel()

    // Rendering controls
    private val renderButton = JButton("Render IFS")
    private val resetViewButton = JButton("Reset View")
    private val histogramEqualizationCheckBox = JCheckBox("Histogram Equalization", true)
    private val statusLabel = JLabel("Ready")

    // IFS renderer
    private val ifsRenderer = IFSRenderer()

    init {
        setupUI()
        loadPalettes()
        setupEventHandlers()
    }

    private fun setupUI() {
        layout = BorderLayout()

        // Create main panel with form layout
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)

        // IFS type selection
        mainPanel.add(createIFSTypePanel())

        // Palette selection
        mainPanel.add(createPalettePanel())

        // Image dimensions
        mainPanel.add(createDimensionsPanel())

        // Common parameters
        mainPanel.add(createCommonParametersPanel())

        // Sierpinski-specific parameters
        sierpinskiParamsPanel.add(createSierpinskiParametersPanel())
        sierpinskiParamsPanel.isVisible = true
        mainPanel.add(sierpinskiParamsPanel)

        // Render controls
        mainPanel.add(createRenderControlsPanel())

        // Status
        mainPanel.add(createStatusPanel())

        add(mainPanel, BorderLayout.NORTH)
        add(Box.createVerticalGlue(), BorderLayout.CENTER)
    }

    private fun createIFSTypePanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = TitledBorder("IFS Type")

        IFSType.values().forEach { ifsTypeCombo.addItem(it) }
        ifsTypeCombo.selectedItem = IFSType.SIERPINSKI_TRIANGLE

        panel.add(JLabel("Type:"))
        panel.add(ifsTypeCombo)

        return panel
    }

    private fun createPalettePanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = TitledBorder("Color Palette")

        panel.add(JLabel("Palette:"))
        panel.add(paletteCombo)

        return panel
    }

    private fun createDimensionsPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = TitledBorder("Image Dimensions")

        panel.add(JLabel("Width:"))
        panel.add(widthSpinner)
        panel.add(JLabel("Height:"))
        panel.add(heightSpinner)

        return panel
    }

    private fun createCommonParametersPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = TitledBorder("IFS Parameters")

        val gbc = GridBagConstraints()
        gbc.insets = Insets(2, 2, 2, 2)
        gbc.anchor = GridBagConstraints.WEST

        // Zoom
        gbc.gridx = 0; gbc.gridy = 0
        panel.add(JLabel("Zoom:"), gbc)
        gbc.gridx = 1
        panel.add(zoomSpinner, gbc)

        // Center X
        gbc.gridx = 2; gbc.gridy = 0
        panel.add(JLabel("Center X:"), gbc)
        gbc.gridx = 3
        panel.add(centerXSpinner, gbc)

        // Center Y
        gbc.gridx = 0; gbc.gridy = 1
        panel.add(JLabel("Center Y:"), gbc)
        gbc.gridx = 1
        panel.add(centerYSpinner, gbc)

        // Iterations
        gbc.gridx = 2; gbc.gridy = 1
        panel.add(JLabel("Iterations:"), gbc)
        gbc.gridx = 3
        panel.add(iterationsSpinner, gbc)

        return panel
    }

    private fun createSierpinskiParametersPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = TitledBorder("Sierpinski Triangle Parameters")

        panel.add(JLabel("Point Size:"))
        panel.add(pointSizeSpinner)

        return panel
    }

    private fun createRenderControlsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = TitledBorder("Rendering")

        // Button row
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        renderButton.preferredSize = Dimension(120, 30)
        buttonPanel.add(renderButton)

        resetViewButton.preferredSize = Dimension(100, 30)
        resetViewButton.toolTipText = "Reset IFS view to default parameters"
        buttonPanel.add(resetViewButton)

        panel.add(buttonPanel)

        // Color enhancement options
        val colorPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        histogramEqualizationCheckBox.toolTipText = "Use adaptive histogram equalization for better contrast"
        colorPanel.add(histogramEqualizationCheckBox)

        panel.add(colorPanel)

        return panel
    }

    private fun createStatusPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createLoweredBevelBorder()
        panel.add(statusLabel, BorderLayout.WEST)
        return panel
    }

    private fun loadPalettes() {
        paletteCombo.removeAllItems()
        val paletteNames = ARGBPaletteRegistry.getPaletteNames().sorted()
        paletteNames.forEach { paletteCombo.addItem(it) }

        if (paletteNames.isNotEmpty()) {
            paletteCombo.selectedIndex = 0
        }
    }

    private fun setupEventHandlers() {
        ifsTypeCombo.addActionListener { onIFSTypeChanged() }
        renderButton.addActionListener { onRenderClicked() }
        resetViewButton.addActionListener { onResetViewClicked() }
    }

    private fun onIFSTypeChanged() {
        val selectedType = ifsTypeCombo.selectedItem as IFSType
        sierpinskiParamsPanel.isVisible = (selectedType == IFSType.SIERPINSKI_TRIANGLE)

        // Update default parameters based on IFS type
        when (selectedType) {
            IFSType.SIERPINSKI_TRIANGLE -> {
                centerXSpinner.value = 0.0
                centerYSpinner.value = 0.0
                zoomSpinner.value = 1.0
                iterationsSpinner.value = 100000
            }
        }

        revalidate()
        repaint()
    }

    private fun onRenderClicked() {
        val ifsType = ifsTypeCombo.selectedItem as IFSType
        val paletteName = paletteCombo.selectedItem as? String
        val palette = paletteName?.let { ARGBPaletteRegistry.getPalette(it) }

        if (palette == null) {
            updateStatus("No palette selected")
            return
        }

        updateStatus("Rendering ${ifsType.displayName}...")
        renderButton.isEnabled = false

        object : SwingWorker<BufferedImage, Void>() {
            override fun doInBackground(): BufferedImage {
                return renderIFS(ifsType, palette)
            }

            override fun done() {
                try {
                    val image = get()
                    onImageGenerated(image)
                    updateStatus("Rendering complete")
                } catch (e: Exception) {
                    updateStatus("Rendering failed: ${e.message}")
                    JOptionPane.showMessageDialog(
                        this@IFSRenderPanel,
                        "Failed to render IFS: ${e.message}",
                        "Rendering Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                } finally {
                    renderButton.isEnabled = true
                }
            }
        }.execute()
    }

    private fun renderIFS(ifsType: IFSType, palette: ARGBPalette): BufferedImage {
        val width = widthSpinner.value as Int
        val height = heightSpinner.value as Int
        val zoom = (zoomSpinner.value as Double).toFloat()
        val centerX = (centerXSpinner.value as Double).toFloat()
        val centerY = (centerYSpinner.value as Double).toFloat()
        val iterations = iterationsSpinner.value as Int
        val useHistogramEqualization = histogramEqualizationCheckBox.isSelected

        return when (ifsType) {
            IFSType.SIERPINSKI_TRIANGLE -> {
                val pointSize = pointSizeSpinner.value as Int
                val params = SierpinskiTriangleParams(zoom, centerX, centerY, iterations, pointSize)
                val result = ifsRenderer.renderIFS(width, height, params)
                result.toBufferedImage(palette, useHistogramEqualization)
            }
        }
    }

    private fun updateStatus(message: String) {
        statusLabel.text = message
        println(message) // Also log to console
    }

    private fun onResetViewClicked() {
        val selectedType = ifsTypeCombo.selectedItem as IFSType
        when (selectedType) {
            IFSType.SIERPINSKI_TRIANGLE -> {
                centerXSpinner.value = 0.0
                centerYSpinner.value = 0.0
                zoomSpinner.value = 1.0
                iterationsSpinner.value = 100000
            }
        }

        updateStatus("View reset to default parameters")
    }

    fun updateFractalParameters(centerX: Double, centerY: Double, zoom: Double) {
        centerXSpinner.value = centerX
        centerYSpinner.value = centerY
        zoomSpinner.value = zoom

        // Automatically re-render with new parameters
        onRenderClicked()
    }

    fun getFractalParameters(): Triple<Double, Double, Double> {
        val centerX = centerXSpinner.value as Double
        val centerY = centerYSpinner.value as Double
        val zoom = zoomSpinner.value as Double
        return Triple(centerX, centerY, zoom)
    }

    enum class IFSType(val displayName: String) {
        SIERPINSKI_TRIANGLE("Sierpinski Triangle")
    }
}