package github.kodakodapa.kingfractal.colors


/**
 * Represents an ARGB color with full 255-level precision for each channel
 * Supports the [256][4] vector format requested for full ARGB support
 */
data class ARGBColor(
    val alpha: Int = 255,
    val red: Int,
    val green: Int,
    val blue: Int
) {
    init {
        require(alpha in 0..255) { "Alpha must be in range 0-255, got $alpha" }
        require(red in 0..255) { "Red must be in range 0-255, got $red" }
        require(green in 0..255) { "Green must be in range 0-255, got $green" }
        require(blue in 0..255) { "Blue must be in range 0-255, got $blue" }
    }

    /**
     * Convert to packed ARGB integer format
     */
    fun toPackedARGB(): Int = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    /**
     * Convert to 4-component vector array [A, R, G, B]
     */
    fun toVector(): IntArray = intArrayOf(alpha, red, green, blue)

    /**
     * Convert to normalized float vector [A, R, G, B] in range 0.0-1.0
     */
    fun toNormalizedVector(): FloatArray = floatArrayOf(
        alpha / 255f,
        red / 255f,
        green / 255f,
        blue / 255f
    )

    companion object {
        /**
         * Create from packed ARGB integer
         */
        fun fromPackedARGB(argb: Int): ARGBColor = ARGBColor(
            alpha = (argb shr 24) and 0xFF,
            red = (argb shr 16) and 0xFF,
            green = (argb shr 8) and 0xFF,
            blue = argb and 0xFF
        )

        /**
         * Create from 4-component vector [A, R, G, B]
         */
        fun fromVector(vector: IntArray): ARGBColor {
            require(vector.size == 4) { "Vector must have 4 components, got ${vector.size}" }
            return ARGBColor(vector[0], vector[1], vector[2], vector[3])
        }

        /**
         * Create from normalized float vector [A, R, G, B]
         */
        fun fromNormalizedVector(vector: FloatArray): ARGBColor {
            require(vector.size == 4) { "Vector must have 4 components, got ${vector.size}" }
            return ARGBColor(
                (vector[0] * 255f).toInt().coerceIn(0, 255),
                (vector[1] * 255f).toInt().coerceIn(0, 255),
                (vector[2] * 255f).toInt().coerceIn(0, 255),
                (vector[3] * 255f).toInt().coerceIn(0, 255)
            )
        }

        // Common colors with full alpha
        val BLACK = ARGBColor(255, 0, 0, 0)
        val WHITE = ARGBColor(255, 255, 255, 255)
        val RED = ARGBColor(255, 255, 0, 0)
        val GREEN = ARGBColor(255, 0, 255, 0)
        val BLUE = ARGBColor(255, 0, 0, 255)
        val TRANSPARENT = ARGBColor(0, 0, 0, 0)

        // Common colors with partial transparency
        val SEMI_TRANSPARENT_RED = ARGBColor(128, 255, 0, 0)
        val SEMI_TRANSPARENT_GREEN = ARGBColor(128, 0, 255, 0)
        val SEMI_TRANSPARENT_BLUE = ARGBColor(128, 0, 0, 255)
    }
}

/**
 * Color interpolation utilities for ARGB colors
 */
object ARGBInterpolation {

    /**
     * Linear interpolation between two ARGB colors
     * @param color1 Start color
     * @param color2 End color
     * @param t Interpolation factor (0.0 to 1.0)
     */
    fun lerp(color1: ARGBColor, color2: ARGBColor, t: Float): ARGBColor {
        val clampedT = t.coerceIn(0f, 1f)
        return ARGBColor(
            alpha = (color1.alpha + clampedT * (color2.alpha - color1.alpha)).toInt().coerceIn(0, 255),
            red = (color1.red + clampedT * (color2.red - color1.red)).toInt().coerceIn(0, 255),
            green = (color1.green + clampedT * (color2.green - color1.green)).toInt().coerceIn(0, 255),
            blue = (color1.blue + clampedT * (color2.blue - color1.blue)).toInt().coerceIn(0, 255)
        )
    }

    /**
     * HSV-based interpolation for more natural color transitions
     */
    fun lerpHSV(color1: ARGBColor, color2: ARGBColor, t: Float): ARGBColor {
        val hsv1 = rgbToHSV(color1.red, color1.green, color1.blue)
        val hsv2 = rgbToHSV(color2.red, color2.green, color2.blue)

        val clampedT = t.coerceIn(0f, 1f)

        // Interpolate alpha linearly
        val alpha = (color1.alpha + clampedT * (color2.alpha - color1.alpha)).toInt().coerceIn(0, 255)

        // Interpolate HSV components
        val h = lerpAngle(hsv1[0], hsv2[0], clampedT)
        val s = hsv1[1] + clampedT * (hsv2[1] - hsv1[1])
        val v = hsv1[2] + clampedT * (hsv2[2] - hsv1[2])

        val rgb = hsvToRGB(h, s, v)
        return ARGBColor(alpha, rgb[0], rgb[1], rgb[2])
    }

    private fun rgbToHSV(r: Int, g: Int, b: Int): FloatArray {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min

        val h = when {
            delta == 0f -> 0f
            max == rf -> 60f * ((gf - bf) / delta % 6f)
            max == gf -> 60f * ((bf - rf) / delta + 2f)
            else -> 60f * ((rf - gf) / delta + 4f)
        }

        val s = if (max == 0f) 0f else delta / max
        val v = max

        return floatArrayOf(h, s, v)
    }

    private fun hsvToRGB(h: Float, s: Float, v: Float): IntArray {
        val c = v * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = v - c

        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return intArrayOf(
            ((r1 + m) * 255f).toInt().coerceIn(0, 255),
            ((g1 + m) * 255f).toInt().coerceIn(0, 255),
            ((b1 + m) * 255f).toInt().coerceIn(0, 255)
        )
    }

    private fun lerpAngle(a1: Float, a2: Float, t: Float): Float {
        var angle1 = a1
        var angle2 = a2

        // Normalize angles to 0-360
        angle1 = angle1 % 360f
        angle2 = angle2 % 360f

        if (angle1 < 0) angle1 += 360f
        if (angle2 < 0) angle2 += 360f

        // Choose shortest path
        val diff = angle2 - angle1
        val adjustedDiff = when {
            diff > 180f -> diff - 360f
            diff < -180f -> diff + 360f
            else -> diff
        }

        return (angle1 + t * adjustedDiff) % 360f
    }
}

/**
 * Represents a full [256][4] color matrix for advanced palette operations
 * This provides the requested [256][4] size vectors functionality
 */
class ARGBColorMatrix(val size: Int = 255) {
    private val matrix = Array(size + 1) { ARGBColor.BLACK.toVector() }

    /**
     * Set color at index using ARGB color
     */
    operator fun set(index: Int, color: ARGBColor) {
        require(index in 0 .. size) { "Index $index out of bounds [0, $size)" }
        matrix[index] = color.toVector()
    }

    /**
     * Get color at index as ARGB color
     */
    operator fun get(index: Int): ARGBColor {
        require(index in 0 .. size) { "Index $index out of bounds [0, $size)" }
        return ARGBColor.fromVector(matrix[index])
    }

    /**
     * Get raw vector at index [A, R, G, B]
     */
    fun getVector(index: Int): IntArray {
        require(index in 0 .. size) { "Index $index out of bounds [0, $size)" }
        return matrix[index].copyOf()
    }

    /**
     * Set vector at index [A, R, G, B]
     */
    fun setVector(index: Int, vector: IntArray) {
        require(index in 0 .. size) { "Index $index out of bounds [0, $size)" }
        require(vector.size == 4) { "Vector must have 4 components" }
        matrix[index] = vector.copyOf()
    }

    /**
     * Get the entire matrix as [256][4] array
     */
    fun toMatrix(): Array<IntArray> = matrix.map { it.copyOf() }.toTypedArray()

    /**
     * Fill the matrix with a gradient between two colors
     */
    fun fillGradient(startColor: ARGBColor, endColor: ARGBColor) {
        for (i in 0 .. size) {
            val t = i.toFloat() / (size - 1).toFloat()
            this[i] = ARGBInterpolation.lerp(startColor, endColor, t)
        }
    }

    /**
     * Fill the matrix with HSV-based gradient for smoother color transitions
     */
    fun fillHSVGradient(startColor: ARGBColor, endColor: ARGBColor) {
        for (i in 0 .. size) {
            val t = i.toFloat() / (size - 1).toFloat()
            this[i] = ARGBInterpolation.lerpHSV(startColor, endColor, t)
        }
    }
}