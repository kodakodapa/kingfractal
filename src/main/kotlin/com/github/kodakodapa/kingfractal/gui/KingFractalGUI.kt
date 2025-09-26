package com.github.kodakodapa.kingfractal.gui

import com.github.kodakodapa.kingfractal.colors.ARGBPaletteRegistry
import com.github.kodakodapa.kingfractal.outputs.PaletteRender
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Main GUI application for KingFractal palette visualization and fractal rendering
 */
class KingFractalGUI : JFrame("KingFractal - Palette Viewer & Fractal Renderer") {

    private val paletteRenderer = PaletteRender()
    private val paletteComboBox = JComboBox<String>()
    private val statusLabel = JLabel("Ready")
    private lateinit var fractalPanel: FractalRenderPanel
    private lateinit var ifsPanel: IFSRenderPanel
    private lateinit var rayTracingPanel: RayTracingPanel
    private lateinit var interactiveFractalPanel: InteractiveFractalPanel
    private lateinit var tabbedPane: JTabbedPane
    private var currentFractalImage: BufferedImage? = null

    init {
        setupUI()
        loadPalettes()
    }

    private fun setupUI() {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()

        // Create menu bar
        jMenuBar = createMenuBar()

        // Create main split pane layout
        val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)

        // Create left panel with tabs for different functions
        tabbedPane = JTabbedPane()

        // Fractal renderer tab
        fractalPanel = FractalRenderPanel { image -> onFractalImageGenerated(image) }
        fractalPanel.setOnResetCallback { interactiveFractalPanel.resetView() }
        tabbedPane.addTab("Fractal Renderer", fractalPanel)

        // IFS fractal tab
        ifsPanel = IFSRenderPanel { image -> onFractalImageGenerated(image) }
        tabbedPane.addTab("IFS Fractals", ifsPanel)

        // Ray tracing tab
        rayTracingPanel = RayTracingPanel { image -> onFractalImageGenerated(image) }
        tabbedPane.addTab("Ray Tracing", rayTracingPanel)

        // Palette viewer tab
        val paletteViewerPanel = createPaletteViewerPanel()
        tabbedPane.addTab("Palette Viewer", paletteViewerPanel)

        // Set Fractal Renderer as default tab
        tabbedPane.selectedIndex = 0

        mainSplitPane.leftComponent = tabbedPane
        mainSplitPane.leftComponent.minimumSize = Dimension(350, 400)

        // Create interactive fractal display as main component
        interactiveFractalPanel = InteractiveFractalPanel { centerX, centerY, zoom ->
            onFractalViewChanged(centerX, centerY, zoom)
        }

        mainSplitPane.rightComponent = interactiveFractalPanel
        mainSplitPane.rightComponent.minimumSize = Dimension(400, 400)

        mainSplitPane.dividerLocation = 350
        mainSplitPane.resizeWeight = 0.3

        add(mainSplitPane, BorderLayout.CENTER)

        // Create status bar
        add(createStatusBar(), BorderLayout.SOUTH)

        // Set window properties
        setSize(1920, 1800)
        setLocationRelativeTo(null)
        minimumSize = Dimension(1800, 1600)
    }

    private fun createPaletteViewerPanel(): JPanel {
        val panel = JPanel(BorderLayout())

        // Create toolbar for palette operations
        val toolbar = createToolbar()
        panel.add(toolbar, BorderLayout.NORTH)

        return panel
    }

    private fun createMenuBar(): JMenuBar {
        val menuBar = JMenuBar()

        // File menu
        val fileMenu = JMenu("File").apply {
            add(JMenuItem("Save Image...").apply {
                addActionListener { saveCurrentImage() }
                accelerator = KeyStroke.getKeyStroke("ctrl S")
                toolTipText = "Save the currently displayed image"
            })
            add(JMenuItem("Save All Palettes...").apply {
                addActionListener { saveAllPalettes() }
                accelerator = KeyStroke.getKeyStroke("ctrl shift S")
                toolTipText = "Save all palettes as a single image"
            })
            addSeparator()
            add(JMenuItem("Exit").apply {
                addActionListener { exitApplication() }
                accelerator = KeyStroke.getKeyStroke("ctrl Q")
            })
        }

        // View menu
        val viewMenu = JMenu("View").apply {
            add(JMenuItem("Refresh Palettes").apply {
                addActionListener { loadPalettes() }
                accelerator = KeyStroke.getKeyStroke("F5")
            })
            addSeparator()
            add(JMenuItem("Fit Fractal to Window").apply {
                addActionListener { interactiveFractalPanel.fitToWindow() }
                accelerator = KeyStroke.getKeyStroke("ctrl 0")
            })
            addSeparator()
            add(JMenuItem("Reset Fractal View").apply {
                addActionListener { fractalPanel.resetView() }
                accelerator = KeyStroke.getKeyStroke("ctrl R")
                toolTipText = "Reset fractal view to default parameters"
            })
        }

        // Help menu
        val helpMenu = JMenu("Help").apply {
            add(JMenuItem("About").apply {
                addActionListener { showAboutDialog() }
            })
        }

        menuBar.add(fileMenu)
        menuBar.add(viewMenu)
        menuBar.add(helpMenu)

        return menuBar
    }

    private fun createToolbar(): JPanel {
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT))

        // Palette selection
        toolbar.add(JLabel("Palette:"))
        paletteComboBox.addActionListener { onPaletteSelected() }
        toolbar.add(paletteComboBox)

        // Render buttons
        toolbar.add(JSeparator(SwingConstants.VERTICAL))
        toolbar.add(JButton("Comprehensive View").apply {
            addActionListener { renderCurrentPalette(RenderMode.COMPREHENSIVE) }
            toolTipText = "Show complete palette visualization"
        })
        toolbar.add(JButton("Gradient Only").apply {
            addActionListener { renderCurrentPalette(RenderMode.GRADIENT) }
            toolTipText = "Show gradient visualization only"
        })
        toolbar.add(JButton("Swatches Only").apply {
            addActionListener { renderCurrentPalette(RenderMode.SWATCHES) }
            toolTipText = "Show color swatches only"
        })

        // All palettes button
        toolbar.add(JSeparator(SwingConstants.VERTICAL))
        toolbar.add(JButton("All Palettes").apply {
            addActionListener { renderAllPalettes() }
            toolTipText = "Render all palettes in one image"
        })

        return toolbar
    }

    private fun createStatusBar(): JPanel {
        val statusPanel = JPanel(BorderLayout())
        statusPanel.border = BorderFactory.createLoweredBevelBorder()
        statusPanel.add(statusLabel, BorderLayout.WEST)
        return statusPanel
    }

    private fun loadPalettes() {
        // Temporarily remove action listener to prevent triggering during loading
        val listeners = paletteComboBox.actionListeners
        listeners.forEach { paletteComboBox.removeActionListener(it) }

        paletteComboBox.removeAllItems()
        val paletteNames = ARGBPaletteRegistry.getPaletteNames().sorted()

        paletteNames.forEach { name ->
            paletteComboBox.addItem(name)
        }

        // Re-add action listeners
        listeners.forEach { paletteComboBox.addActionListener(it) }

        updateStatus("Loaded ${paletteNames.size} palettes")
    }


    private fun onPaletteSelected() {
        val selectedPalette = paletteComboBox.selectedItem as? String
        if (selectedPalette != null) {
            renderCurrentPalette(RenderMode.COMPREHENSIVE)
        }
    }

    private fun renderCurrentPalette(mode: RenderMode) {
        val selectedPalette = paletteComboBox.selectedItem as? String ?: return
        val palette = ARGBPaletteRegistry.getPalette(selectedPalette) ?: return

        updateStatus("Rendering palette: $selectedPalette...")

        object : SwingWorker<BufferedImage, Void>() {
            override fun doInBackground(): BufferedImage {
                return when (mode) {
                    RenderMode.COMPREHENSIVE -> paletteRenderer.toComprehensiveImage(palette)
                    RenderMode.GRADIENT -> paletteRenderer.toGradientImage(palette)
                    RenderMode.SWATCHES -> paletteRenderer.toSwatchGrid(palette)
                }
            }

            override fun done() {
                try {
                    val image = get()
                    displayImage(image)
                    updateStatus("Rendered: $selectedPalette (${mode.displayName})")
                } catch (e: Exception) {
                    updateStatus("Error rendering palette: ${e.message}")
                    showErrorDialog("Rendering Error", "Failed to render palette: ${e.message}")
                }
            }
        }.execute()
    }

    private fun renderAllPalettes() {
        updateStatus("Rendering all palettes...")

        object : SwingWorker<BufferedImage, Void>() {
            override fun doInBackground(): BufferedImage {
                return paletteRenderer.renderAllPalettes()
            }

            override fun done() {
                try {
                    val image = get()
                    displayImage(image)
                    updateStatus("Rendered all palettes")
                } catch (e: Exception) {
                    updateStatus("Error rendering all palettes: ${e.message}")
                    showErrorDialog("Rendering Error", "Failed to render all palettes: ${e.message}")
                }
            }
        }.execute()
    }

    private fun displayImage(image: BufferedImage) {
        // Show palette images in a popup dialog since we removed static display
        val dialog = JDialog(this, "Palette Visualization", true)
        val imageLabel = JLabel(ImageIcon(image))
        val scrollPane = JScrollPane(imageLabel)
        scrollPane.preferredSize = Dimension(
            minOf(image.width + 50, 1200),
            minOf(image.height + 50, 800)
        )
        dialog.add(scrollPane)
        dialog.pack()
        dialog.setLocationRelativeTo(this)
        dialog.isVisible = true
    }

    private fun onFractalImageGenerated(image: BufferedImage) {
        currentFractalImage = image

        // Update the interactive panel with the new image
        interactiveFractalPanel.setImage(image)

        // Update the fractal parameters in the interactive panel
        updateInteractiveFractalParameters()
    }

    private fun onFractalViewChanged(centerX: Double, centerY: Double, zoom: Double) {
        // Check which tab is active and update the appropriate panel
        when (tabbedPane.selectedIndex) {
            0 -> {
                // Fractal Renderer tab
                fractalPanel.updateFractalParameters(centerX, centerY, zoom)
            }
            1 -> {
                // IFS Fractals tab
                ifsPanel.updateFractalParameters(centerX, centerY, zoom)
            }
            // Palette Viewer tab doesn't need view updates
        }
        updateStatus("View changed - Center: (${"%.6f".format(centerX)}, ${"%.6f".format(centerY)}) Zoom: ${"%.2e".format(zoom)}")
    }

    private fun updateInteractiveFractalParameters() {
        // Get current parameters from the active panel and update interactive panel
        val (centerX, centerY, zoom) = when (tabbedPane.selectedIndex) {
            0 -> fractalPanel.getFractalParameters()
            1 -> ifsPanel.getFractalParameters()
            else -> Triple(0.0, 0.0, 1.0) // Default values for palette viewer
        }
        interactiveFractalPanel.setFractalParameters(centerX, centerY, zoom)
    }

    private fun saveCurrentImage() {
        val image = currentFractalImage ?: return

        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("PNG Images", "png")
            selectedFile = File("palette_${System.currentTimeMillis()}.png")
        }

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            try {
                // Convert Image to BufferedImage if necessary
                val bufferedImage = image

                ImageIO.write(bufferedImage, "PNG", file)
                updateStatus("Saved image: ${file.name}")
            } catch (e: Exception) {
                showErrorDialog("Save Error", "Failed to save image: ${e.message}")
            }
        }
    }

    private fun saveAllPalettes() {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("PNG Images", "png")
            selectedFile = File("all_palettes_${System.currentTimeMillis()}.png")
        }

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            updateStatus("Saving all palettes...")

            object : SwingWorker<Void, Void>() {
                override fun doInBackground(): Void? {
                    paletteRenderer.saveAllPalettesAsPng(file.absolutePath)
                    return null
                }

                override fun done() {
                    try {
                        get() // This will throw any exception that occurred in doInBackground
                        updateStatus("Saved all palettes: ${file.name}")
                    } catch (e: Exception) {
                        updateStatus("Error saving all palettes: ${e.message}")
                        showErrorDialog("Save Error", "Failed to save all palettes: ${e.message}")
                    }
                }
            }.execute()
        }
    }


    private fun showAboutDialog() {
        val message = """
            KingFractal - Palette Viewer & Fractal Renderer

            A comprehensive tool for visualizing ARGB color palettes and rendering fractals.

            Features:
            • ARGB palette visualization with transparency support
            • Multiple rendering modes (comprehensive, gradient, swatches)
            • OpenCL-accelerated fractal rendering (Mandelbrot & Julia sets)
            • Customizable fractal parameters (zoom, center, iterations)
            • Real-time palette application to fractal images
            • Save images and palette visualizations

            Built with Kotlin, Swing, and OpenCL
        """.trimIndent()

        JOptionPane.showMessageDialog(
            this,
            message,
            "About KingFractal",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun showErrorDialog(title: String, message: String) {
        JOptionPane.showMessageDialog(
            this,
            message,
            title,
            JOptionPane.ERROR_MESSAGE
        )
    }

    private fun updateStatus(message: String) {
        statusLabel.text = message
        println(message) // Also log to console
    }

    private fun exitApplication() {
        // Clean up OpenCL resources
        try {
            fractalPanel.cleanup()
            interactiveFractalPanel.cleanup()
        } catch (e: Exception) {
            println("Error during cleanup: ${e.message}")
        }
        System.exit(0)
    }

    enum class RenderMode(val displayName: String) {
        COMPREHENSIVE("Comprehensive"),
        GRADIENT("Gradient"),
        SWATCHES("Swatches")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Set system look and feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            SwingUtilities.invokeLater {
                KingFractalGUI().isVisible = true
            }
        }
    }
}