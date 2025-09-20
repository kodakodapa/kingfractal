# Mouse Rectangle Selection Feature Documentation

## Overview
This feature allows users to interactively zoom into specific regions of fractal images by selecting rectangular areas with the mouse.

## How It Works

### User Interaction
1. **Click and Drag**: Users click and drag on the fractal image to create a selection rectangle
2. **Visual Feedback**: A red dashed rectangle appears during selection
3. **Automatic Zoom**: Upon mouse release, the selected area becomes the new view
4. **Reset Option**: Users can reset to default view using Ctrl+R or View menu

### Technical Implementation

#### 1. SelectableImageLabel Class
- Custom JLabel that captures mouse events
- Draws selection rectangle overlay during drag operation
- Validates minimum selection size (5x5 pixels)
- Calls callback function when selection is completed

#### 2. Coordinate Conversion
- Converts pixel coordinates to fractal coordinate system
- Accounts for current zoom level and center position
- Handles aspect ratio calculations
- Works for both Mandelbrot and Julia sets

#### 3. Parameter Updates
- Enhanced FractalParams base class with abstract methods
- `withNewParams()` method preserves fractal-specific parameters
- Updates zoom, centerX, and centerY while maintaining other settings
- Julia set parameters (juliaReal, juliaImag) are preserved during zoom

#### 4. UI Integration
- Works only in the "Fractal Renderer" tab
- Reset functionality via View menu (Ctrl+R)
- Tooltip instructions for user guidance
- Updated About dialog documents new features

## Code Structure

### Key Files Modified:
1. **KingFractalGUI.kt**: Added SelectableImageLabel and rectangle handling
2. **FractalRenderPanel.kt**: Added parameter management methods
3. **ConfigClasses.kt**: Enhanced FractalParams with base class and abstract methods

### New Methods:
- `handleRectangleSelection(Rectangle)`: Main selection handler
- `convertPixelToFractalCoordinates()`: Coordinate system conversion
- `getCurrentParams()`: Get current fractal parameters
- `updateParams()`: Update fractal parameters programmatically
- `resetToDefaults()`: Reset to default view

## Usage Instructions

1. **Navigate to Fractal Renderer tab**
2. **Render a fractal** (Mandelbrot or Julia set)
3. **Click and drag on the image** to select an area of interest
4. **Release mouse** to zoom into the selected region
5. **Use Ctrl+R or View > Reset Fractal View** to return to default zoom

## Features

✅ Interactive rectangle selection with visual feedback
✅ Automatic coordinate system conversion
✅ Parameter preservation for Julia sets
✅ Reset functionality
✅ Tab-specific behavior (only works in Fractal Renderer)
✅ Keyboard shortcuts (Ctrl+R for reset)
✅ User guidance via tooltips and menu items

## Benefits

- **Intuitive Navigation**: Natural point-and-click interface for exploring fractals
- **Precise Zooming**: Select exact regions of interest
- **Non-destructive**: Easy reset to default view
- **Preserves Settings**: Maintains fractal-specific parameters during zoom operations
- **Responsive UI**: Visual feedback during selection process

This implementation provides a seamless and intuitive way for users to explore fractal images by zooming into areas of interest using familiar mouse interaction patterns.