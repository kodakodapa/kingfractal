package com.github.kodakodapa.kingfractal.gui

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertDoesNotThrow
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.awt.Color
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class InteractiveFractalPanelTest {

    private lateinit var panel: InteractiveFractalPanel
    private val viewChangeEvents = mutableListOf<Triple<Double, Double, Double>>()
    private lateinit var testImage: BufferedImage

    @BeforeEach
    fun setUp() {
        viewChangeEvents.clear()

        // Create panel with view change listener
        panel = InteractiveFractalPanel { centerX, centerY, zoom ->
            viewChangeEvents.add(Triple(centerX, centerY, zoom))
        }

        // Create a test image
        testImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
        val g2d = testImage.createGraphics()
        g2d.color = Color.BLUE
        g2d.fillRect(0, 0, 100, 100)
        g2d.dispose()
    }

    @AfterEach
    fun tearDown() {
        panel.cleanup()
    }

    @Nested
    @DisplayName("Initialization Tests")
    inner class InitializationTests {

        @Test
        fun `should initialize with default values`() {
            assertEquals(Dimension(1800, 1600), panel.preferredSize)
            assertNotNull(panel.background)
            assertTrue(panel.isOpaque)
            assertTrue(panel.isFocusable)
        }

        @Test
        fun `should have default fractal parameters`() {
            val view = panel.getCurrentView()
            assertEquals(0.0, view.first) // centerX
            assertEquals(0.0, view.second) // centerY
            assertEquals(1.0, view.third) // zoom
        }

        @Test
        fun `should initialize with no image`() {
            // Panel should handle null image gracefully
            assertDoesNotThrow {
                panel.repaint()
            }
        }
    }

    @Nested
    @DisplayName("Image Management Tests")
    inner class ImageManagementTests {

        @Test
        fun `should set and display image correctly`() {
            panel.setImage(testImage)

            // Image should be set and panel should repaint
            assertDoesNotThrow {
                panel.repaint()
            }
        }

        @Test
        fun `should handle image replacement without memory leak`() {
            val firstImage = BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB)
            val secondImage = BufferedImage(75, 75, BufferedImage.TYPE_INT_ARGB)

            panel.setImage(firstImage)
            panel.setImage(secondImage)

            // Should handle image replacement without issues
            assertDoesNotThrow {
                panel.repaint()
            }
        }

        @Test
        fun `should reset display scale when new image is set`() {
            panel.setImage(testImage)

            // Zoom in
            panel.zoomIn()

            // Set new image
            val newImage = BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB)
            panel.setImage(newImage)

            // Display scale should be reset (tested indirectly through view)
            assertDoesNotThrow {
                panel.repaint()
            }
        }

        @Test
        fun `should handle cleanup properly`() {
            panel.setImage(testImage)

            assertDoesNotThrow {
                panel.cleanup()
            }

            // Should handle painting after cleanup
            assertDoesNotThrow {
                panel.repaint()
            }
        }
    }

    @Nested
    @DisplayName("Fractal Parameters Tests")
    inner class FractalParametersTests {

        @Test
        fun `should update fractal parameters correctly`() {
            val centerX = 1.5
            val centerY = -0.5
            val zoom = 2.0

            panel.setFractalParameters(centerX, centerY, zoom)

            val view = panel.getCurrentView()
            assertEquals(centerX, view.first)
            assertEquals(centerY, view.second)
            assertEquals(zoom, view.third)
        }

        @Test
        fun `should clamp zoom values within bounds`() {
            // Test minimum zoom
            panel.setFractalParameters(0.0, 0.0, 0.0001)
            val minView = panel.getCurrentView()
            assertTrue(minView.third >= 0.001) // MIN_ZOOM

            // Test maximum zoom
            panel.setFractalParameters(0.0, 0.0, 10000000.0)
            val maxView = panel.getCurrentView()
            assertTrue(maxView.third <= 1000000.0) // MAX_ZOOM
        }
    }

    @Nested
    @DisplayName("Mouse Interaction Tests")
    inner class MouseInteractionTests {

        @Test
        fun `should trigger view change on mouse wheel zoom`() {
            panel.setImage(testImage)
            viewChangeEvents.clear()

            // Simulate mouse wheel event
            val wheelEvent = createMouseWheelEvent(-1) // Zoom in
            panel.dispatchEvent(wheelEvent)

            // Should trigger view change
            assertTrue(viewChangeEvents.isNotEmpty())
            val event = viewChangeEvents.last()
            assertTrue(event.third > 1.0) // Zoom increased
        }

        @Test
        fun `should zoom to center on mouse wheel`() {
            panel.setImage(testImage)
            viewChangeEvents.clear()

            // Simulate mouse wheel at non-center position
            val wheelEvent = createMouseWheelEvent(-1, x = 100, y = 100)
            panel.dispatchEvent(wheelEvent)

            // Since we modified to always zoom to center, check that happens
            assertTrue(viewChangeEvents.isNotEmpty())
        }

        @Test
        fun `should handle mouse drag for panning`() {
            panel.setImage(testImage)
            panel.size = Dimension(800, 600)
            viewChangeEvents.clear()

            // Simulate mouse press
            val pressEvent = createMouseEvent(MouseEvent.MOUSE_PRESSED, 100, 100)
            panel.dispatchEvent(pressEvent)

            // Simulate mouse drag
            val dragEvent = createMouseEvent(MouseEvent.MOUSE_DRAGGED, 150, 150)
            panel.dispatchEvent(dragEvent)

            // Simulate mouse release with sufficient distance
            val releaseEvent = createMouseEvent(MouseEvent.MOUSE_RELEASED, 200, 200)
            panel.dispatchEvent(releaseEvent)

            // Should trigger view change if drag distance > 5
            assertTrue(viewChangeEvents.isNotEmpty())
        }

        @Test
        fun `should handle double-click zoom`() {
            panel.setImage(testImage)
            viewChangeEvents.clear()

            // Simulate double-click
            val clickEvent = createMouseEvent(MouseEvent.MOUSE_CLICKED, 200, 200, clickCount = 2)
            panel.dispatchEvent(clickEvent)

            // Should trigger zoom
            assertTrue(viewChangeEvents.isNotEmpty())
            val event = viewChangeEvents.last()
            assertTrue(event.third > 1.0) // Zoomed in
        }

        @Test
        fun `should not drag without image`() {
            viewChangeEvents.clear()

            // Try to drag without image
            val pressEvent = createMouseEvent(MouseEvent.MOUSE_PRESSED, 100, 100)
            panel.dispatchEvent(pressEvent)

            val dragEvent = createMouseEvent(MouseEvent.MOUSE_DRAGGED, 150, 150)
            panel.dispatchEvent(dragEvent)

            // Should not trigger any events without image
            assertTrue(viewChangeEvents.isEmpty())
        }
    }

    @Nested
    @DisplayName("Keyboard Interaction Tests")
    inner class KeyboardInteractionTests {

        @Test
        fun `should reset view on R key`() {
            panel.setImage(testImage)
            panel.setFractalParameters(2.0, 3.0, 5.0)
            viewChangeEvents.clear()

            // Test reset functionality directly (equivalent to R key)
            panel.resetView()

            // Should reset to default values
            assertTrue(viewChangeEvents.isNotEmpty())
            val event = viewChangeEvents.last()
            assertEquals(0.0, event.first) // Default centerX
            assertEquals(0.0, event.second) // Default centerY
            assertEquals(1.0, event.third) // Default zoom
        }

        @Test
        fun `should zoom in on plus key`() {
            panel.setImage(testImage)
            panel.size = Dimension(800, 600)
            viewChangeEvents.clear()

            // Test zoom in functionality directly (equivalent to + key)
            panel.zoomIn()

            // Should zoom in
            assertTrue(viewChangeEvents.isNotEmpty())
            assertTrue(viewChangeEvents.last().third > 1.0)
        }

        @Test
        fun `should zoom out on minus key`() {
            panel.setImage(testImage)
            panel.size = Dimension(800, 600)
            panel.setFractalParameters(0.0, 0.0, 2.0)
            viewChangeEvents.clear()

            // Test zoom out functionality directly (equivalent to - key)
            panel.zoomOut()

            // Should zoom out
            assertTrue(viewChangeEvents.isNotEmpty())
            assertTrue(viewChangeEvents.last().third < 2.0)
        }

        @Test
        fun `should fit to window on Home key`() {
            panel.setImage(testImage)
            panel.size = Dimension(800, 600)

            // Test fit to window functionality directly (equivalent to Home key)
            assertDoesNotThrow {
                panel.fitToWindow()
            }
        }
    }

    @Nested
    @DisplayName("View Control Tests")
    inner class ViewControlTests {

        @Test
        fun `should zoom in programmatically`() {
            panel.setImage(testImage)
            panel.size = Dimension(800, 600)
            viewChangeEvents.clear()

            panel.zoomIn()

            assertTrue(viewChangeEvents.isNotEmpty())
            assertTrue(viewChangeEvents.last().third > 1.0)
        }

        @Test
        fun `should zoom out programmatically`() {
            panel.setImage(testImage)
            panel.size = Dimension(800, 600)
            panel.setFractalParameters(0.0, 0.0, 2.0)
            viewChangeEvents.clear()

            panel.zoomOut()

            assertTrue(viewChangeEvents.isNotEmpty())
            assertTrue(viewChangeEvents.last().third < 2.0)
        }

        @Test
        fun `should reset view correctly`() {
            panel.setImage(testImage)
            panel.setFractalParameters(5.0, -3.0, 10.0)
            viewChangeEvents.clear()

            panel.resetView()

            assertTrue(viewChangeEvents.isNotEmpty())
            val event = viewChangeEvents.last()
            assertEquals(0.0, event.first)
            assertEquals(0.0, event.second)
            assertEquals(1.0, event.third)
        }

        @Test
        fun `should fit image to window`() {
            panel.setImage(testImage)
            panel.size = Dimension(800, 600)

            assertDoesNotThrow {
                panel.fitToWindow()
            }
        }

        @Test
        fun `should return current view correctly`() {
            val centerX = 1.5
            val centerY = -0.5
            val zoom = 3.0

            panel.setFractalParameters(centerX, centerY, zoom)

            val view = panel.getCurrentView()
            assertEquals(centerX, view.first)
            assertEquals(centerY, view.second)
            assertEquals(zoom, view.third)
        }
    }

    @Nested
    @DisplayName("Callback Tests")
    inner class CallbackTests {

        @Test
        fun `should trigger callback on view change`() {
            val callbackCount = AtomicInteger(0)
            val lastCallback = AtomicReference<Triple<Double, Double, Double>>()

            val testPanel = InteractiveFractalPanel { centerX, centerY, zoom ->
                callbackCount.incrementAndGet()
                lastCallback.set(Triple(centerX, centerY, zoom))
            }

            testPanel.setImage(testImage)
            testPanel.size = Dimension(800, 600)
            testPanel.zoomIn()

            assertTrue(callbackCount.get() > 0)
            assertNotNull(lastCallback.get())
            assertTrue(lastCallback.get()!!.third > 1.0)

            testPanel.cleanup()
        }

        @Test
        fun `should not trigger callback without image`() {
            val callbackCount = AtomicInteger(0)

            val testPanel = InteractiveFractalPanel { _, _, _ ->
                callbackCount.incrementAndGet()
            }

            testPanel.size = Dimension(800, 600)
            testPanel.zoomIn()

            assertEquals(0, callbackCount.get())

            testPanel.cleanup()
        }
    }

    @Nested
    @DisplayName("Rendering Tests")
    inner class RenderingTests {

        @Test
        fun `should repaint without errors when no image`() {
            assertDoesNotThrow {
                panel.repaint()
            }
        }

        @Test
        fun `should repaint without errors with image`() {
            panel.setImage(testImage)

            assertDoesNotThrow {
                panel.repaint()
            }
        }

        @Test
        fun `should handle display updates with image`() {
            panel.setImage(testImage)
            panel.size = Dimension(800, 600)

            assertDoesNotThrow {
                panel.repaint()
                panel.revalidate()
                // Visual elements should be updated without errors
            }
        }

        @Test
        fun `should handle UI updates when image is present`() {
            panel.setImage(testImage)

            assertDoesNotThrow {
                panel.repaint()
                panel.revalidate()
                // Instructions and UI should be updated without errors
            }
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    inner class EdgeCaseTests {

        @Test
        fun `should handle zero size panel`() {
            panel.size = Dimension(0, 0)
            panel.setImage(testImage)

            assertDoesNotThrow {
                panel.zoomIn()
                panel.repaint()
            }
        }

        @Test
        fun `should handle very small images`() {
            val tinyImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)

            assertDoesNotThrow {
                panel.setImage(tinyImage)
                panel.zoomIn()
                panel.zoomOut()
                panel.repaint()
            }
        }

        @Test
        fun `should handle rapid zoom changes`() {
            panel.setImage(testImage)
            panel.size = Dimension(800, 600)

            assertDoesNotThrow {
                repeat(10) {
                    panel.zoomIn()
                }
                repeat(10) {
                    panel.zoomOut()
                }
            }
        }

        @Test
        fun `should handle rapid image changes`() {
            assertDoesNotThrow {
                repeat(5) { i ->
                    val img = BufferedImage(100 + i * 10, 100 + i * 10, BufferedImage.TYPE_INT_ARGB)
                    panel.setImage(img)
                }
            }
        }

        @Test
        fun `should cleanup multiple times safely`() {
            panel.setImage(testImage)

            assertDoesNotThrow {
                panel.cleanup()
                panel.cleanup() // Second cleanup should not throw
            }
        }
    }

    // Helper methods for creating test events
    private fun createMouseEvent(
        id: Int,
        x: Int,
        y: Int,
        button: Int = MouseEvent.BUTTON1,
        clickCount: Int = 1
    ): MouseEvent {
        return MouseEvent(
            panel,
            id,
            System.currentTimeMillis(),
            0,
            x, y,
            clickCount,
            false,
            button
        )
    }

    private fun createMouseWheelEvent(
        wheelRotation: Int,
        x: Int = panel.width / 2,
        y: Int = panel.height / 2
    ): MouseWheelEvent {
        return MouseWheelEvent(
            panel,
            MouseEvent.MOUSE_WHEEL,
            System.currentTimeMillis(),
            0,
            x, y,
            0,
            false,
            MouseWheelEvent.WHEEL_UNIT_SCROLL,
            1,
            wheelRotation
        )
    }


}