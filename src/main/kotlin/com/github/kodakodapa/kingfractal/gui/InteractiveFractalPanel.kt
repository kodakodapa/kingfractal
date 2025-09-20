package com.github.kodakodapa.kingfractal.gui

import java.awt.*
import java.awt.event.*
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import javax.swing.*

/**
 * Interactive panel for displaying fractal images with mouse navigation
 * Supports panning (click and drag) and zooming (mouse wheel)
 */
class InteractiveFractalPanel(
    private val onViewChanged: (centerX: Double, centerY: Double, zoom: Double) -> Unit
) : JPanel() {

    private var currentImage: BufferedImage? = null
    private var scaledImage: BufferedImage? = null

    // Fractal coordinate system
    private var fractalCenterX = 0.0
    private var fractalCenterY = 0.0
    private var fractalZoom = 1.0

    // Display scaling
    private var displayScale = 1.0
    private var imageOffsetX = 0
    private var imageOffsetY = 0

    // Mouse interaction state
    private var isDragging = false
    private var lastMouseX = 0
    private var lastMouseY = 0
    private var dragStartX = 0
    private var dragStartY = 0

    // Constants
    private val ZOOM_FACTOR = 1.2
    private val MIN_ZOOM = 0.001
    private val MAX_ZOOM = 1000000.0

    init {
        setupUI()
        setupMouseListeners()
    }

    private fun setupUI() {
        background = Color.DARK_GRAY
        cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
        isOpaque = true
        preferredSize = Dimension(1800, 1600)
    }

    private fun setupMouseListeners() {
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (currentImage != null && SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = true
                    lastMouseX = e.x
                    lastMouseY = e.y
                    dragStartX = e.x
                    dragStartY = e.y
                    cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (isDragging && SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = false
                    cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)

                    // If there was significant drag, update the fractal view
                    val dragDistance = Point(dragStartX, dragStartY).distance(Point(e.x, e.y))
                    if (dragDistance > 5) { // Minimum drag distance to trigger update
                        updateFractalView()
                    }
                }
            }

            override fun mouseClicked(e: MouseEvent) {
                if (currentImage != null && SwingUtilities.isLeftMouseButton(e) && e.clickCount == 2) {
                    // Double-click to zoom in on a point
                    zoomAt(e.x, e.y, ZOOM_FACTOR)
                }
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (isDragging && currentImage != null) {
                    val deltaX = e.x - lastMouseX
                    val deltaY = e.y - lastMouseY

                    imageOffsetX += deltaX
                    imageOffsetY += deltaY

                    lastMouseX = e.x
                    lastMouseY = e.y

                    repaint()
                }
            }

            override fun mouseMoved(e: MouseEvent) {
                if (currentImage != null) {
                    // Update tooltip with fractal coordinates
                    val fractalCoords = screenToFractal(e.x, e.y)
                    toolTipText = String.format("Fractal: (%.6f, %.6f)", fractalCoords.x, fractalCoords.y)
                }
            }
        })

        addMouseWheelListener { e ->
            if (currentImage != null) {
                val zoomFactor = if (e.wheelRotation < 0) ZOOM_FACTOR else 1.0 / ZOOM_FACTOR
                zoomAt(width / 2, height / 2, zoomFactor)
            }
        }

        // Add keyboard shortcuts
        isFocusable = true
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_R -> resetView()
                    KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS -> zoomAt(width / 2, height / 2, ZOOM_FACTOR)
                    KeyEvent.VK_MINUS -> zoomAt(width / 2, height / 2, 1.0 / ZOOM_FACTOR)
                    KeyEvent.VK_HOME -> fitToWindow()
                }
            }
        })
    }

    fun setImage(image: BufferedImage) {
        currentImage = image

        // Reset display parameters when new image is set
        displayScale = 1.0
        centerImageInPanel()
        updateScaledImage()
        repaint()
    }

    fun setFractalParameters(centerX: Double, centerY: Double, zoom: Double) {
        fractalCenterX = centerX
        fractalCenterY = centerY
        fractalZoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    private fun centerImageInPanel() {
        val image = currentImage ?: return
        imageOffsetX = (width - (image.width * displayScale).toInt()) / 2
        imageOffsetY = (height - (image.height * displayScale).toInt()) / 2
    }

    private fun updateScaledImage() {
        val image = currentImage ?: return

        val scaledWidth = (image.width * displayScale).toInt().coerceAtLeast(1)
        val scaledHeight = (image.height * displayScale).toInt().coerceAtLeast(1)

        if (scaledWidth != image.width || scaledHeight != image.height) {
            scaledImage = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB)
            val g2d = scaledImage!!.createGraphics()
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2d.drawImage(image, 0, 0, scaledWidth, scaledHeight, null)
            g2d.dispose()
        } else {
            scaledImage = image
        }
    }

    private fun zoomAt(screenX: Int, screenY: Int, zoomFactor: Double) {
        val image = currentImage ?: return

        // Convert screen coordinates to fractal coordinates before zoom
        val fractalPoint = screenToFractal(screenX, screenY)

        // Update fractal zoom directly for GPU re-rendering
        val newFractalZoom = (fractalZoom * zoomFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)

        if (newFractalZoom != fractalZoom) {
            fractalZoom = newFractalZoom
            fractalCenterX = fractalPoint.x
            fractalCenterY = fractalPoint.y

            // Trigger immediate re-render for smooth GPU-based zooming
            onViewChanged(fractalCenterX, fractalCenterY, fractalZoom)
        }
    }

    private fun updateFractalView() {
        val image = currentImage ?: return

        // Calculate how much the image has moved in fractal coordinates
        val centerScreenX = width / 2.0
        val centerScreenY = height / 2.0

        val imageCenterX = imageOffsetX + (image.width * displayScale) / 2.0
        val imageCenterY = imageOffsetY + (image.height * displayScale) / 2.0

        // Calculate offset in screen pixels
        val screenOffsetX = centerScreenX - imageCenterX
        val screenOffsetY = centerScreenY - imageCenterY

        // Convert to fractal coordinates
        val fractalOffsetX = (screenOffsetX / displayScale) / (image.width / 4.0) / fractalZoom
        val fractalOffsetY = (screenOffsetY / displayScale) / (image.height / 4.0) / fractalZoom

        // Update fractal center
        fractalCenterX += fractalOffsetX
        fractalCenterY += fractalOffsetY

        // Reset image position
        centerImageInPanel()

        // Notify listener of view change
        onViewChanged(fractalCenterX, fractalCenterY, fractalZoom)
    }

    private fun screenToFractal(screenX: Int, screenY: Int): Point2D.Double {
        val image = currentImage ?: return Point2D.Double(0.0, 0.0)

        // Convert screen coordinates to image coordinates
        val imageX = (screenX - imageOffsetX) / displayScale
        val imageY = (screenY - imageOffsetY) / displayScale

        // Convert to fractal coordinates
        val fractalX = (imageX - image.width / 2.0) / (fractalZoom * image.width / 4.0) + fractalCenterX
        val fractalY = (imageY - image.height / 2.0) / (fractalZoom * image.height / 4.0) + fractalCenterY

        return Point2D.Double(fractalX, fractalY)
    }

    fun resetView() {
        // Reset to default fractal view
        fractalCenterX = 0.0
        fractalCenterY = 0.0
        fractalZoom = 1.0
        displayScale = 1.0
        centerImageInPanel()
        updateScaledImage()
        onViewChanged(fractalCenterX, fractalCenterY, fractalZoom)
        repaint()
    }

    fun fitToWindow() {
        val image = currentImage ?: return

        val panelAspect = width.toDouble() / height
        val imageAspect = image.width.toDouble() / image.height

        displayScale = if (panelAspect > imageAspect) {
            height.toDouble() / image.height * 0.9 // 90% of panel height
        } else {
            width.toDouble() / image.width * 0.9 // 90% of panel width
        }

        centerImageInPanel()
        updateScaledImage()
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val image = scaledImage ?: currentImage
        if (image != null) {
            g2d.drawImage(image, imageOffsetX, imageOffsetY, null)

            // Draw crosshair at center
            g2d.color = Color.WHITE
            g2d.stroke = BasicStroke(1f)
            val centerX = width / 2
            val centerY = height / 2
            g2d.drawLine(centerX - 10, centerY, centerX + 10, centerY)
            g2d.drawLine(centerX, centerY - 10, centerX, centerY + 10)

            // Draw coordinates
            g2d.color = Color.WHITE
            g2d.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            val coords = String.format("Center: (%.6f, %.6f) Zoom: %.2e", fractalCenterX, fractalCenterY, fractalZoom)
            g2d.drawString(coords, 10, height - 10)

        } else {
            // Draw placeholder
            g2d.color = Color.LIGHT_GRAY
            g2d.font = Font(Font.SANS_SERIF, Font.PLAIN, 16)
            val message = "No fractal image loaded"
            val fm = g2d.fontMetrics
            val x = (width - fm.stringWidth(message)) / 2
            val y = height / 2
            g2d.drawString(message, x, y)
        }

        // Draw instructions in corner
        if (currentImage != null) {
            g2d.color = Color.WHITE
            g2d.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
            val instructions = arrayOf(
                "Mouse: Drag to pan, Wheel to zoom",
                "Double-click to zoom in",
                "Keys: R=Reset, +/- =Zoom, Home=Fit"
            )

            for (i in instructions.indices) {
                g2d.drawString(instructions[i], 10, 20 + i * 15)
            }
        }
    }

    override fun getPreferredSize(): Dimension {
        return currentImage?.let {
            Dimension((it.width * displayScale).toInt(), (it.height * displayScale).toInt())
        } ?: Dimension(1800, 1600)
    }

    // Public methods for external control
    fun zoomIn() = zoomAt(width / 2, height / 2, ZOOM_FACTOR)
    fun zoomOut() = zoomAt(width / 2, height / 2, 1.0 / ZOOM_FACTOR)

    fun getCurrentView(): Triple<Double, Double, Double> {
        return Triple(fractalCenterX, fractalCenterY, fractalZoom)
    }
}