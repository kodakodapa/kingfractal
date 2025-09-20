# KingFractal
GPU powered fractal engine with interactive exploration capabilities.

## Features

- **OpenCL-accelerated fractal rendering** for high performance
- **Interactive GUI** with palette visualization and fractal rendering
- **Mouse rectangle selection** for zooming into fractal regions
- **Multiple fractal types**: Mandelbrot and Julia sets
- **Customizable parameters**: zoom, center position, iterations, and Julia constants
- **Palette system** with comprehensive visualization modes
- **Export capabilities** for saving images

## Interactive Navigation

### Mouse Rectangle Selection
- **Click and drag** on fractal images to select areas of interest
- **Automatic zoom** into selected regions with precise coordinate conversion
- **Visual feedback** with red dashed selection rectangle
- **Reset functionality** (Ctrl+R) to return to default view
- Works with both Mandelbrot and Julia sets

### Controls
- `Ctrl+R`: Reset fractal view to defaults
- `Ctrl+S`: Save current image
- `Ctrl+0`: Fit image to window
- `Ctrl+1`: Show actual size

## Getting Started

1. Build the project: `gradle build`
2. Run the application: `gradle run`
3. Navigate to the "Fractal Renderer" tab
4. Select fractal type and palette
5. Click "Render Fractal"
6. Click and drag on the image to zoom into interesting areas!

## Requirements

- Java 17 or higher
- OpenCL-compatible GPU (for accelerated rendering)
- Gradle build system
