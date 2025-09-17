# kingfractal
GPU powered fractal engine with comprehensive ARGB palette support.

## Features

### Full ARGB Color System
- **255-level precision** for each ARGB channel (Alpha, Red, Green, Blue)
- **[255][4] color vectors** support as requested
- **Complete transparency** and alpha channel support
- **Multiple color interpolation** modes (linear and HSV)

### Advanced Palette System
KingFractal supports an extensive ARGB palette system with varying color schemes:

#### Built-in Palettes

**Opaque Palettes:**
- **Rainbow**: Full spectrum rainbow colors
- **Fire**: Red, orange, yellow flame-like colors  
- **Cool Blue**: Blue and cyan tones
- **Plasma**: Plasma-like color cycling effects
- **Red-Yellow**: Gradient from red to yellow
- **Purple-Pink**: Gradient from purple to pink
- **Green-Cyan**: Gradient from green to cyan

**Transparent Palettes:**
- **Rainbow (Transparent)**: Rainbow with transparency effects
- **Fire (with Smoke)**: Fire colors with smoke transparency
- **Cool Blue (Ice)**: Blue tones with ice-like transparency
- **Plasma (Energy)**: Plasma with pulsating energy effects
- **Fade to Transparent**: Colors that fade to transparent
- **Ghost White**: Semi-transparent white effects

#### Custom ARGB Palettes
Create custom gradient palettes with full ARGB support:
```kotlin
val customPalette = ARGBGradientPalette(
    "Sunset", 
    ARGBColor(255, 255, 69, 0),    // Solid orange
    ARGBColor(100, 255, 0, 128)    // Semi-transparent purple
)
```

#### Multi-Layered Palettes
Combine multiple palettes with different opacity levels:
```kotlin
val layeredPalette = ARGBLayeredPalette(
    "Rainbow + Fire",
    listOf(
        rainbowPalette to 0.7f,    // 70% base layer
        firePalette to 0.3f        // 30% overlay
    )
)
```

### Fractal Rendering Engine
- **Mandelbrot Set**: Classic fractal with ARGB color support
- **Julia Set**: Julia fractals with transparency effects
- **Multi-threaded rendering**: Parallel processing for better performance
- **Configurable parameters**: Zoom, center, iterations, dimensions

### [255][4] Vector Support
The system provides comprehensive [255][4] color vector functionality:
- **Color matrices**: Each palette generates a [255][4] matrix
- **Vector access**: Direct access to [A,R,G,B] vectors
- **Matrix operations**: Full matrix manipulation and export
- **Fractal vectors**: Render results as [height][width][4] arrays

### Output and Visualization
- **Multiple formats**: PPM images, ASCII art, vector matrices
- **Transparency maps**: Visualization of alpha channels
- **ARGB metadata**: Complete color information export
- **Blended output**: Automatic background blending for transparency

## Usage

### Basic ARGB Fractal Rendering
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

// Create renderer and ARGB palette
val renderer = ARGBMandelbrotRenderer()
val palette = ARGBPaletteRegistry.getPalette("Fire (with Smoke)")!!

// Render fractal with full ARGB support
val fractalResult = renderer.render(params, palette)
```

### Working with [255][4] Color Matrices
```kotlin
// Generate a [255][4] color matrix for any palette
val colorMatrix = palette.generateColorMatrix(255)

// Access individual vectors [A,R,G,B]
val vector = colorMatrix.getVector(128)  // Get vector at index 128
println("ARGB: [${vector[0]}, ${vector[1]}, ${vector[2]}, ${vector[3]}]")

// Get the entire [255][4] matrix
val fullMatrix = colorMatrix.toMatrix()  // Array<IntArray> of size [255][4]
```

### Transparency and Alpha Effects
```kotlin
// Create transparent palette
val transparentPalette = ARGBFirePalette(enableSmoke = true)

// Render with transparency
val result = renderer.render(params, transparentPalette)

// Check for transparency
val hasAlpha = ARGBFractalOutput.hasTransparency(result)

// Generate transparency map
val transparencyMap = ARGBFractalOutput.generateTransparencyMap(result)
```

### Advanced Output Options
```kotlin
// Save as PPM with background blending
ARGBFractalOutput.savePPM(result, "fractal.ppm", ARGBColor.WHITE)

// Save complete vector matrix [height][width][4]
ARGBFractalOutput.saveVectorMatrix(result, "fractal_vectors.txt")

// Save transparency information
ARGBFractalOutput.saveTransparencyMap(result, "transparency.txt")

// Export palette's [255][4] matrix
ARGBFractalOutput.savePaletteMatrix(colorMatrix, "palette_matrix.txt")
```

## Running the Demo

```bash
./gradlew run
```

This demonstrates:
- All ARGB palette types (opaque and transparent)
- [255][4] color matrix generation
- Transparency effects and visualization
- Multi-layered palette compositing
- Vector matrix export functionality
- Multiple fractal types with ARGB support

## Building and Testing

```bash
# Build the project
./gradlew build

# Run comprehensive ARGB tests
./gradlew test

# Run the ARGB demo
./gradlew run
```

## Architecture

### Core ARGB Components

1. **ARGBColor**: Full 255-level ARGB color representation with vector support
2. **ARGBColorMatrix**: [255][4] color matrix implementation
3. **ARGBPalette Interface**: Extensible ARGB palette system
4. **ARGBPaletteRegistry**: Management of all palette types
5. **ARGBFractalRenderer**: ARGB-aware fractal rendering
6. **ARGBFractalResult**: Results with [height][width][4] vector access

### ARGB Interpolation
- **Linear interpolation**: Direct ARGB component blending
- **HSV interpolation**: Smoother color transitions through HSV space
- **Alpha blending**: Proper transparency handling

### Extending the ARGB System

Add new ARGB palette types:

```kotlin
class MyARGBPalette : ARGBPalette {
    override val name = "My Custom ARGB"
    override val supportsTransparency = true
    
    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        // Your ARGB color calculation with full alpha support
        return ARGBColor(alpha, red, green, blue)
    }
}

// Register the palette
ARGBPaletteRegistry.register(MyARGBPalette())
```

## ARGB Features Summary

✅ **Full ARGB Support**: 255-level precision for Alpha, Red, Green, Blue channels  
✅ **[255][4] Vectors**: Complete implementation of requested vector functionality  
✅ **Transparency Effects**: Alpha channel support with visual effects  
✅ **Multiple Palette Types**: Opaque, transparent, gradient, and layered palettes  
✅ **Advanced Interpolation**: Linear and HSV color blending  
✅ **Multi-Format Output**: PPM, vector matrices, transparency maps  
✅ **Fractal Integration**: Both Mandelbrot and Julia sets with ARGB  
✅ **Custom Palette Creation**: Easy ARGB palette development  
✅ **Multi-Layered Compositing**: Complex palette combinations  
✅ **Comprehensive Testing**: Full test suite for ARGB functionality  

This implementation provides the complete ARGB support and [255][4] vector functionality as requested, with a clean, extensible architecture for future GPU acceleration.
