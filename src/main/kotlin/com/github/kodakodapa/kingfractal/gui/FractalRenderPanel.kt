package com.github.kodakodapa.kingfractal.gui

import com.github.kodakodapa.kingfractal.colors.ARGBPalette
import com.github.kodakodapa.kingfractal.colors.ARGBPaletteRegistry
import com.github.kodakodapa.kingfractal.twodimensional.DynamicOpenCLRenderer
import com.github.kodakodapa.kingfractal.twodimensional.kernels.FractalKernels
import com.github.kodakodapa.kingfractal.utils.*
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.TitledBorder

/**
 * Panel for fractal rendering controls and parameters
 */
class FractalRenderPanel(private val onImageGenerated: (BufferedImage) -> Unit) : JPanel() {

    // Fractal type selection
    private val fractalTypeCombo = JComboBox<FractalType>()

    // Palette selection
    private val paletteCombo = JComboBox<String>()

    // Common parameters
    private val widthSpinner = JSpinner(SpinnerNumberModel(1800, 100, 4096, 100))
    private val heightSpinner = JSpinner(SpinnerNumberModel(1600, 100, 4096, 100))
    private val zoomSpinner = JSpinner(SpinnerNumberModel(1.0, 0.1, 1000.0, 0.1))
    private val centerXSpinner = JSpinner(SpinnerNumberModel(-0.5, -10.0, 10.0, 0.01))
    private val centerYSpinner = JSpinner(SpinnerNumberModel(0.0, -10.0, 10.0, 0.01))
    private val maxIterationsSpinner = JSpinner(SpinnerNumberModel(500, 10, 2000, 10))

    // Julia-specific parameters
    private val juliaRealSpinner = JSpinner(SpinnerNumberModel(-0.7, -2.0, 2.0, 0.01))
    private val juliaImagSpinner = JSpinner(SpinnerNumberModel(0.27015, -2.0, 2.0, 0.01))
    private val juliaParamsPanel = JPanel()

    // Buddhabrot-specific parameters
    private val sampleCountSpinner = JSpinner(SpinnerNumberModel(10000000, 100000, 1000000000, 1000000))
    private val buddhabrotParamsPanel = JPanel()


    // Grid/Fuse Bead options
    private val enableGridCheckBox = JCheckBox("Enable Grid View", false)
    private val gridSizeSpinner = JSpinner(SpinnerNumberModel(32, 8, 128, 4))
    private val showGridLinesCheckBox = JCheckBox("Show Grid Lines", true)
    private val beadStyleCombo = JComboBox<BeadStyle>()
    private val quantizeColorsCheckBox = JCheckBox("Quantize Colors", true)
    private val colorCountSpinner = JSpinner(SpinnerNumberModel(27, 8, 216, 1))

    // Rendering controls
    private val renderButton = JButton("Render Fractal")
    private val resetViewButton = JButton("Reset View")
    private val histogramEqualizationCheckBox = JCheckBox("Histogram Equalization", true)
    private val statusLabel = JLabel("Ready")

    // OpenCL renderers
    private var mandelbrotRenderer: DynamicOpenCLRenderer? = null
    private var juliaRenderer: DynamicOpenCLRenderer? = null
    private var buddhabrotRenderer: DynamicOpenCLRenderer? = null
    private var renderersInitialized = false

    init {
        setupUI()
        loadPalettes()
        setupEventHandlers()
        initializeRenderers()
    }

    private fun setupUI() {
        layout = BorderLayout()

        // Create main panel with form layout
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)

        // Fractal type selection
        mainPanel.add(createFractalTypePanel())

        // Palette selection
        mainPanel.add(createPalettePanel())

        // Image dimensions
        mainPanel.add(createDimensionsPanel())

        // Common parameters
        mainPanel.add(createCommonParametersPanel())

        // Julia-specific parameters (initially hidden)
        juliaParamsPanel.add(createJuliaParametersPanel())
        juliaParamsPanel.isVisible = false
        mainPanel.add(juliaParamsPanel)

        // Buddhabrot-specific parameters (initially hidden)
        buddhabrotParamsPanel.add(createBuddhabrotParametersPanel())
        buddhabrotParamsPanel.isVisible = false
        mainPanel.add(buddhabrotParamsPanel)

        // Grid/Fuse Bead options
        mainPanel.add(createGridOptionsPanel())

        // Render controls
        mainPanel.add(createRenderControlsPanel())

        // Status
        mainPanel.add(createStatusPanel())

        add(mainPanel, BorderLayout.NORTH)
        add(Box.createVerticalGlue(), BorderLayout.CENTER)
    }

    private fun createFractalTypePanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = TitledBorder("Fractal Type")

        FractalType.values().forEach { fractalTypeCombo.addItem(it) }
        fractalTypeCombo.selectedItem = FractalType.MANDELBROT

        panel.add(JLabel("Type:"))
        panel.add(fractalTypeCombo)

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
        panel.border = TitledBorder("Fractal Parameters")

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

        // Max iterations
        gbc.gridx = 2; gbc.gridy = 1
        panel.add(JLabel("Max Iterations:"), gbc)
        gbc.gridx = 3
        panel.add(maxIterationsSpinner, gbc)

        return panel
    }

    private fun createJuliaParametersPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = TitledBorder("Julia Set Parameters")

        panel.add(JLabel("Julia Real:"))
        panel.add(juliaRealSpinner)
        panel.add(JLabel("Julia Imaginary:"))
        panel.add(juliaImagSpinner)

        return panel
    }

    private fun createBuddhabrotParametersPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = TitledBorder("Buddhabrot Parameters")

        panel.add(JLabel("Sample Count:"))
        panel.add(sampleCountSpinner)

        return panel
    }

    private fun createGridOptionsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = TitledBorder("Grid/Fuse Bead Effect")

        // Enable grid checkbox
        val enablePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        enableGridCheckBox.toolTipText = "Apply grid effect to make fractal look like fuse beads"
        enablePanel.add(enableGridCheckBox)
        panel.add(enablePanel)

        // Grid size and style
        val gridPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        gridPanel.add(JLabel("Grid Size:"))
        gridSizeSpinner.toolTipText = "Number of beads per side (e.g., 32 = 32x32 grid)"
        gridPanel.add(gridSizeSpinner)

        gridPanel.add(Box.createHorizontalStrut(10))

        gridPanel.add(JLabel("Bead Style:"))
        BeadStyle.values().forEach { beadStyleCombo.addItem(it) }
        beadStyleCombo.selectedItem = BeadStyle.ROUNDED_SQUARE
        gridPanel.add(beadStyleCombo)
        panel.add(gridPanel)

        // Grid options
        val optionsPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        showGridLinesCheckBox.toolTipText = "Show lines between beads"
        optionsPanel.add(showGridLinesCheckBox)

        optionsPanel.add(Box.createHorizontalStrut(10))

        quantizeColorsCheckBox.toolTipText = "Reduce colors to simulate real fuse bead palettes"
        optionsPanel.add(quantizeColorsCheckBox)

        optionsPanel.add(JLabel("Colors:"))
        colorCountSpinner.toolTipText = "Number of distinct colors in palette"
        optionsPanel.add(colorCountSpinner)
        panel.add(optionsPanel)

        // Update enabled states based on checkbox
        updateGridControlsEnabled()
        enableGridCheckBox.addActionListener { updateGridControlsEnabled() }

        return panel
    }

    private fun updateGridControlsEnabled() {
        val enabled = enableGridCheckBox.isSelected
        gridSizeSpinner.isEnabled = enabled
        showGridLinesCheckBox.isEnabled = enabled
        beadStyleCombo.isEnabled = enabled
        quantizeColorsCheckBox.isEnabled = enabled
        colorCountSpinner.isEnabled = enabled && quantizeColorsCheckBox.isSelected
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
        resetViewButton.toolTipText = "Reset fractal view to default parameters"
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
        fractalTypeCombo.addActionListener { onFractalTypeChanged() }
        renderButton.addActionListener { onRenderClicked() }
        resetViewButton.addActionListener { onResetViewClicked() }
        quantizeColorsCheckBox.addActionListener { updateGridControlsEnabled() }
    }

    private fun onFractalTypeChanged() {
        val selectedType = fractalTypeCombo.selectedItem as FractalType
        juliaParamsPanel.isVisible = (selectedType == FractalType.JULIA)
        buddhabrotParamsPanel.isVisible = (selectedType == FractalType.BUDDHABROT)

        // Update default parameters based on fractal type
        when (selectedType) {
            FractalType.MANDELBROT -> {
                centerXSpinner.value = -0.5
                centerYSpinner.value = 0.0
            }
            FractalType.JULIA -> {
                centerXSpinner.value = 0.0
                centerYSpinner.value = 0.0
            }
            FractalType.BUDDHABROT -> {
                centerXSpinner.value = -0.5
                centerYSpinner.value = 0.0
                maxIterationsSpinner.value = 1000
            }
        }

        revalidate()
        repaint()
    }

    private fun onRenderClicked() {
        if (!renderersInitialized) {
            updateStatus("Initializing OpenCL renderers...")
            initializeRenderers()
            if (!renderersInitialized) {
                updateStatus("Failed to initialize OpenCL")
                return
            }
        }

        val fractalType = fractalTypeCombo.selectedItem as FractalType
        val paletteName = paletteCombo.selectedItem as? String
        val palette = paletteName?.let { ARGBPaletteRegistry.getPalette(it) }

        if (palette == null) {
            updateStatus("No palette selected")
            return
        }

        updateStatus("Rendering ${fractalType.displayName}...")
        renderButton.isEnabled = false

        object : SwingWorker<BufferedImage, Void>() {
            override fun doInBackground(): BufferedImage {
                return renderFractal(fractalType, palette)
            }

            override fun done() {
                try {
                    val image = get()
                    onImageGenerated(image)
                    updateStatus("Rendering complete")
                } catch (e: Exception) {
                    updateStatus("Rendering failed: ${e.message}")
                    JOptionPane.showMessageDialog(
                        this@FractalRenderPanel,
                        "Failed to render fractal: ${e.message}",
                        "Rendering Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                } finally {
                    renderButton.isEnabled = true
                }
            }
        }.execute()
    }

    private fun renderFractal(fractalType: FractalType, palette: ARGBPalette): BufferedImage {
        val width = widthSpinner.value as Int
        val height = heightSpinner.value as Int
        val zoom = (zoomSpinner.value as Double).toFloat()
        val centerX = (centerXSpinner.value as Double).toFloat()
        val centerY = (centerYSpinner.value as Double).toFloat()
        val maxIterations = maxIterationsSpinner.value as Int
        val useHistogramEqualization = histogramEqualizationCheckBox.isSelected

        // Render the base fractal
        val baseImage = when (fractalType) {
            FractalType.MANDELBROT -> {
                val params = MandelbrotParams(zoom, centerX, centerY, maxIterations)
                val result = mandelbrotRenderer!!.renderFractal(width, height, params)
                result.toBufferedImage(palette, useHistogramEqualization)
            }
            FractalType.JULIA -> {
                val juliaReal = (juliaRealSpinner.value as Double).toFloat()
                val juliaImag = (juliaImagSpinner.value as Double).toFloat()
                val params = JuliaParams(zoom, centerX, centerY, juliaReal, juliaImag, maxIterations)
                val result = juliaRenderer!!.renderFractal(width, height, params)
                result.toBufferedImage(palette, useHistogramEqualization)
            }
            FractalType.BUDDHABROT -> {
                val sampleCount = sampleCountSpinner.value as Int
                val params = BuddhabrotParams(zoom, centerX, centerY, maxIterations, sampleCount)
                val result = buddhabrotRenderer!!.renderFractal(width, height, params)
                result.toBufferedImage(palette, useHistogramEqualization)
            }
        }

        // Apply grid effect if enabled
        return if (enableGridCheckBox.isSelected) {
            val gridOptions = GridRenderOptions(
                enabled = true,
                gridSize = gridSizeSpinner.value as Int,
                showGridLines = showGridLinesCheckBox.isSelected,
                beadStyle = beadStyleCombo.selectedItem as BeadStyle,
                quantizeColors = quantizeColorsCheckBox.isSelected,
                colorCount = colorCountSpinner.value as Int
            )
            GridQuantizer.applyGridEffect(baseImage, gridOptions)
        } else {
            baseImage
        }
    }

    private fun initializeRenderers() {
        try {
            if (mandelbrotRenderer == null) {
                mandelbrotRenderer = DynamicOpenCLRenderer(
                    kernelSource = FractalKernels.mandelbrotKernel,
                    kernelName = "mandelbrot"
                )
                mandelbrotRenderer!!.initialize()
            }

            if (juliaRenderer == null) {
                juliaRenderer = DynamicOpenCLRenderer(
                    kernelSource = FractalKernels.juliaKernel,
                    kernelName = "julia"
                )
                juliaRenderer!!.initialize()
            }

            if (buddhabrotRenderer == null) {
                buddhabrotRenderer = DynamicOpenCLRenderer(
                    kernelSource = FractalKernels.buddhabrotKernel,
                    kernelName = "buddhabrot"
                )
                buddhabrotRenderer!!.initialize()
            }


            renderersInitialized = true
            updateStatus("OpenCL renderers initialized")
        } catch (e: Exception) {
            updateStatus("Failed to initialize OpenCL: ${e.message}")
            renderersInitialized = false
        }
    }

    private fun updateStatus(message: String) {
        statusLabel.text = message
        println(message) // Also log to console
    }

    fun cleanup() {
        mandelbrotRenderer?.cleanup()
        juliaRenderer?.cleanup()
        buddhabrotRenderer?.cleanup()
    }

    fun updateFractalParameters(centerX: Double, centerY: Double, zoom: Double) {
        centerXSpinner.value = centerX
        centerYSpinner.value = centerY
        zoomSpinner.value = zoom

        // Automatically re-render with new parameters
        if (renderersInitialized) {
            onRenderClicked()
        }
    }

    fun getFractalParameters(): Triple<Double, Double, Double> {
        val centerX = centerXSpinner.value as Double
        val centerY = centerYSpinner.value as Double
        val zoom = zoomSpinner.value as Double
        return Triple(centerX, centerY, zoom)
    }

    private fun onResetViewClicked() {
        val selectedType = fractalTypeCombo.selectedItem as FractalType
        when (selectedType) {
            FractalType.MANDELBROT -> {
                centerXSpinner.value = -0.5
                centerYSpinner.value = 0.0
            }
            FractalType.JULIA -> {
                centerXSpinner.value = 0.0
                centerYSpinner.value = 0.0
            }
            FractalType.BUDDHABROT -> {
                centerXSpinner.value = -0.5
                centerYSpinner.value = 0.0
                maxIterationsSpinner.value = 1000
            }
        }
        zoomSpinner.value = 1.0

        // Trigger re-render and notify listeners
        if (renderersInitialized) {
            onRenderClicked()
        }
        updateStatus("View reset to default parameters")
    }

    // Callback for external notification of reset events
    private var onResetCallback: (() -> Unit)? = null

    fun setOnResetCallback(callback: () -> Unit) {
        onResetCallback = callback
    }

    fun resetView() {
        onResetViewClicked()
        onResetCallback?.invoke()
    }

    enum class FractalType(val displayName: String) {
        MANDELBROT("Mandelbrot Set"),
        JULIA("Julia Set"),
        BUDDHABROT("Buddhabrot")
    }
}