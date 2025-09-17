# kingfractal
GPU powered fractal engine with support for varying color palettes.

## Features

### Fractal Rendering
- **Mandelbrot Set**: Classic fractal with customizable parameters
- **Julia Set**: Julia fractals with configurable constants
- **Efficient CPU Implementation**: Fast rendering with potential for GPU acceleration

### Color Palette System
KingFractal supports multiple color palettes for fractal rendering:

#### Built-in Palettes
- **Rainbow**: Full spectrum rainbow colors
- **Fire**: Red, orange, yellow flame-like colors
- **Cool Blue**: Blue and cyan tones
- **Red-Yellow**: Gradient from red to yellow
- **Purple-Pink**: Gradient from purple to pink  
- **Green-Cyan**: Gradient from green to cyan

#### Custom Palettes
Create custom gradient palettes between any two colors:
```kotlin
val customPalette = GradientPalette("My Palette", 0xFF0000, 0x00FF00) // Red to Green
```

#### Palette Registry
Palettes are managed through a central registry:
```kotlin
// Register a new palette
PaletteRegistry.register(myCustomPalette)

// Get available palettes
val allPalettes = PaletteRegistry.getAllPalettes()
val paletteNames = PaletteRegistry.getPaletteNames()

// Retrieve a specific palette
val palette = PaletteRegistry.getPalette("Rainbow")
```

## Usage

### Basic Fractal Rendering
```kotlin
// Create fractal parameters
val params = FractalParams(
    width = 800,
    height = 600,
    centerX = -0.5,
    centerY = 0.0,
    zoom = 1.0,
    maxIterations = 100
)

// Create renderer and palette
val renderer = MandelbrotRenderer()
val palette = PaletteRegistry.getPalette("Rainbow")!!

// Render fractal
val fractalData = renderer.render(params, palette)
```

### Saving Output
```kotlin
// Save as PPM image
FractalOutput.savePPM(fractalData, "fractal.ppm")

// Save as ASCII art
FractalOutput.saveAscii(fractalData, "fractal.txt")

// Display in terminal
FractalOutput.displayInTerminal(fractalData)
```

## Running the Demo

```bash
./gradlew run
```

This will generate sample fractals with different palettes and save them as both PPM images and ASCII art files.

## Building and Testing

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the application
./gradlew run
```

## Architecture

### Core Components

1. **ColorPalette Interface**: Defines how iteration counts map to colors
2. **PaletteRegistry**: Manages available palettes
3. **FractalRenderer**: Renders fractals using specified palettes
4. **FractalOutput**: Handles various output formats

### Extending the System

To add a new palette type:

```kotlin
class MyCustomPalette : ColorPalette {
    override val name = "My Custom"
    
    override fun getColor(iterations: Int, maxIterations: Int): Int {
        // Your color calculation logic here
        return if (iterations >= maxIterations) 0x000000 else yourColor
    }
}

// Register it
PaletteRegistry.register(MyCustomPalette())
```

## Future Enhancements

- OpenCL GPU acceleration for faster rendering
- More fractal types (Burning Ship, Nova, etc.)
- Interactive palette editor
- Real-time palette switching
- Advanced color interpolation modes