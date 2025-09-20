package com.github.kodakodapa.kingfractal.gui

import com.github.kodakodapa.kingfractal.colors.ARGBPaletteRegistry
import com.github.kodakodapa.kingfractal.outputs.PaletteRender
import com.github.kodakodapa.kingfractal.utils.FractalParams
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Custom JLabel that allows rectangle selection with mouse
 */
class SelectableImageLabel(private val onRectangleSelected: (Rectangle) -> Unit) : JLabel() {
    private var selectionStart: Point? = null
    private var selectionEnd: Point? = null
    private var isSelecting = false

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e) && icon != null) {
                    selectionStart = e.point
                    isSelecting = true
                    repaint()
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (isSelecting && selectionStart != null) {
                    selectionEnd = e.point
                    isSelecting = false
                    val rect = createRectangle(selectionStart!!, selectionEnd!!)
                    if (rect.width > 5 && rect.height > 5) { // Minimum selection size
                        onRectangleSelected(rect)
                    }
                    selectionStart = null
                    selectionEnd = null
                    repaint()
                }
            }
        })

        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (isSelecting) {
                    selectionEnd = e.point
                    repaint()
                }
            }
        })
    }

    private fun createRectangle(start: Point, end: Point): Rectangle {
        val x = minOf(start.x, end.x)
        val y = minOf(start.y, end.y)
        val width = kotlin.math.abs(end.x - start.x)
        val height = kotlin.math.abs(end.y - start.y)
        return Rectangle(x, y, width, height)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        if (isSelecting && selectionStart != null && selectionEnd != null) {
            val g2d = g as Graphics2D
            g2d.color = Color.RED
            g2d.stroke = BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, floatArrayOf(5f), 0f)
            
            val rect = createRectangle(selectionStart!!, selectionEnd!!)
            g2d.drawRect(rect.x, rect.y, rect.width, rect.height)
        }
    }
}

/**
 * Main GUI application for KingFractal palette visualization and fractal rendering
 */
class KingFractalGUI : JFrame("KingFractal - Palette Viewer & Fractal Renderer") {

    private val paletteRenderer = PaletteRender()
    private val imageLabel = SelectableImageLabel { rect -> handleRectangleSelection(rect) }
    private val paletteComboBox = JComboBox<String>()
    private val statusLabel = JLabel("Ready")
    private lateinit var scrollPane: JScrollPane
    private lateinit var fractalPanel: FractalRenderPanel
    private lateinit var tabbedPane: JTabbedPane

    init {
        setupUI()
        loadPalettes()
        displayDefaultImage()
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

        // Palette viewer tab
        val paletteViewerPanel = createPaletteViewerPanel()
        tabbedPane.addTab("Palette Viewer", paletteViewerPanel)

        // Fractal renderer tab
        fractalPanel = FractalRenderPanel { image -> displayImage(image) }
        tabbedPane.addTab("Fractal Renderer", fractalPanel)

        mainSplitPane.leftComponent = tabbedPane
        mainSplitPane.leftComponent.minimumSize = Dimension(350, 400)

        // Create main image display area
        imageLabel.horizontalAlignment = SwingConstants.CENTER
        imageLabel.verticalAlignment = SwingConstants.CENTER
        imageLabel.background = Color.LIGHT_GRAY
        imageLabel.isOpaque = true
        imageLabel.toolTipText = "Click and drag to select an area to zoom into (Fractal Renderer tab only)"

        scrollPane = JScrollPane(imageLabel).apply {
            preferredSize = Dimension(800, 600)
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        mainSplitPane.rightComponent = scrollPane
        mainSplitPane.rightComponent.minimumSize = Dimension(400, 400)

        mainSplitPane.dividerLocation = 350
        mainSplitPane.resizeWeight = 0.3

        add(mainSplitPane, BorderLayout.CENTER)

        // Create status bar
        add(createStatusBar(), BorderLayout.SOUTH)

        // Set window properties
        setSize(1200, 800)
        setLocationRelativeTo(null)
        minimumSize = Dimension(800, 600)
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
            add(JMenuItem("Fit to Window").apply {
                addActionListener { fitImageToWindow() }
                accelerator = KeyStroke.getKeyStroke("ctrl 0")
            })
            add(JMenuItem("Actual Size").apply {
                addActionListener { showActualSize() }
                accelerator = KeyStroke.getKeyStroke("ctrl 1")
            })
            addSeparator()
            add(JMenuItem("Reset Fractal View").apply {
                addActionListener { resetFractalView() }
                accelerator = KeyStroke.getKeyStroke("ctrl R")
                toolTipText = "Reset fractal to default zoom and position"
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
        paletteComboBox.removeAllItems()
        val paletteNames = ARGBPaletteRegistry.getPaletteNames().sorted()

        paletteNames.forEach { name ->
            paletteComboBox.addItem(name)
        }

        updateStatus("Loaded ${paletteNames.size} palettes")
    }

    private fun displayDefaultImage() {
        SwingUtilities.invokeLater {
            if (paletteComboBox.itemCount > 0) {
                paletteComboBox.selectedIndex = 0
                renderCurrentPalette(RenderMode.COMPREHENSIVE)
            } else {
                displayPlaceholderImage()
            }
        }
    }

    private fun displayPlaceholderImage() {
        val placeholder = BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB)
        val g2d = placeholder.createGraphics()
        g2d.color = Color.LIGHT_GRAY
        g2d.fillRect(0, 0, 400, 300)
        g2d.color = Color.DARK_GRAY
        g2d.drawString("No palettes available", 150, 150)
        g2d.dispose()

        displayImage(placeholder)
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
        imageLabel.icon = ImageIcon(image)
        imageLabel.text = null
        scrollPane.revalidate()
        scrollPane.repaint()
    }

    private fun saveCurrentImage() {
        val icon = imageLabel.icon as? ImageIcon ?: return
        val image = icon.image

        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("PNG Images", "png")
            selectedFile = File("palette_${System.currentTimeMillis()}.png")
        }

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            try {
                // Convert Image to BufferedImage if necessary
                val bufferedImage = if (image is BufferedImage) {
                    image
                } else {
                    val bi = BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB)
                    val g2d = bi.createGraphics()
                    g2d.drawImage(image, 0, 0, null)
                    g2d.dispose()
                    bi
                }

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

    private fun fitImageToWindow() {
        val icon = imageLabel.icon as? ImageIcon ?: return
        val originalImage = icon.image

        val viewportSize = scrollPane.viewport.size
        val imageSize = Dimension(originalImage.getWidth(null), originalImage.getHeight(null))

        // Avoid division by zero
        if (imageSize.width <= 0 || imageSize.height <= 0 || viewportSize.width <= 0 || viewportSize.height <= 0) {
            return
        }

        val scale = minOf(
            viewportSize.width.toDouble() / imageSize.width,
            viewportSize.height.toDouble() / imageSize.height
        )

        if (scale < 1.0) {
            val scaledWidth = (imageSize.width * scale).toInt()
            val scaledHeight = (imageSize.height * scale).toInt()
            val scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH)
            imageLabel.icon = ImageIcon(scaledImage)
        } else {
            imageLabel.icon = ImageIcon(originalImage)
        }

        scrollPane.revalidate()
        scrollPane.repaint()
        updateStatus("Fitted image to window")
    }

    private fun showActualSize() {
        val icon = imageLabel.icon as? ImageIcon ?: return
        val originalImage = icon.image
        imageLabel.icon = ImageIcon(originalImage)
        scrollPane.revalidate()
        scrollPane.repaint()
        updateStatus("Showing actual size")
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
            • Interactive rectangle selection for zooming into fractal regions
            • Real-time palette application to fractal images
            • Save images and palette visualizations

            Controls:
            • Click and drag on fractal images to zoom into selected areas
            • Use Ctrl+R to reset fractal view to defaults

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

    private fun handleRectangleSelection(rect: Rectangle) {
        // Only handle rectangle selection if we're on the fractal renderer tab
        if (tabbedPane.selectedIndex != 1) {
            updateStatus("Rectangle selection only works in Fractal Renderer tab")
            return
        }

        val icon = imageLabel.icon as? ImageIcon ?: return
        val image = icon.image
        
        // Convert rectangle coordinates to fractal coordinates
        val imageWidth = image.getWidth(null)
        val imageHeight = image.getHeight(null)
        
        if (imageWidth <= 0 || imageHeight <= 0) return
        
        // Get current fractal parameters
        val currentParams = fractalPanel.getCurrentParams() ?: return
        
        // Calculate the fractal coordinate range for the selected rectangle
        val fractalCoords = convertPixelToFractalCoordinates(
            rect, imageWidth, imageHeight, currentParams
        )
        
        // Update the fractal parameters
        fractalPanel.updateParams(fractalCoords)
        
        updateStatus("Zoomed to selected region")
    }

    private fun convertPixelToFractalCoordinates(
        rect: Rectangle, 
        imageWidth: Int, 
        imageHeight: Int, 
        params: FractalParams
    ): FractalParams {
        // Calculate the fractal coordinate system bounds
        val aspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val halfWidth = 2.0f / params.zoom
        val halfHeight = halfWidth / aspectRatio
        
        val fractalLeft = params.centerX - halfWidth
        val fractalRight = params.centerX + halfWidth
        val fractalTop = params.centerY - halfHeight
        val fractalBottom = params.centerY + halfHeight
        
        // Convert pixel coordinates to fractal coordinates
        val selectionLeft = fractalLeft + (rect.x.toFloat() / imageWidth) * (fractalRight - fractalLeft)
        val selectionRight = fractalLeft + ((rect.x + rect.width).toFloat() / imageWidth) * (fractalRight - fractalLeft)
        val selectionTop = fractalTop + (rect.y.toFloat() / imageHeight) * (fractalBottom - fractalTop)
        val selectionBottom = fractalTop + ((rect.y + rect.height).toFloat() / imageHeight) * (fractalBottom - fractalTop)
        
        // Calculate new center and zoom
        val newCenterX = (selectionLeft + selectionRight) / 2
        val newCenterY = (selectionTop + selectionBottom) / 2
        val selectionWidth = selectionRight - selectionLeft
        val newZoom = 2.0f / selectionWidth
        
        return params.withNewParams(newZoom, newCenterX, newCenterY)
    }

    private fun resetFractalView() {
        fractalPanel.resetToDefaults()
        updateStatus("Reset to default view")
    }

    private fun exitApplication() {
        // Clean up OpenCL resources
        try {
            fractalPanel.cleanup()
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