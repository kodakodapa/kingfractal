package com.github.kodakodapa.kingfractal.utils

sealed class FractalParams

// Fractal-specific configuration classes
data class MandelbrotParams(
    val zoom: Float = 1.0f,
    val centerX: Float = -0.5f,
    val centerY: Float = 0.0f,
    val maxIterations: Int = 100
) : FractalParams()

data class JuliaParams(
    val zoom: Float = 1.0f,
    val centerX: Float = 0.0f,
    val centerY: Float = 0.0f,
    val juliaReal: Float = -0.7f,
    val juliaImag: Float = 0.27015f,
    val maxIterations: Int = 100
) : FractalParams()

data class BuddhabrotParams(
    val zoom: Float = 1.0f,
    val centerX: Float = -0.5f,
    val centerY: Float = 0.0f,
    val maxIterations: Int = 1000,
    val sampleCount: Int = 1000000
) : FractalParams()

// IFS (Iterated Function System) parameters
sealed class IFSParams : FractalParams()

data class SierpinskiTriangleParams(
    val zoom: Float = 1.0f,
    val centerX: Float = 0.0f,
    val centerY: Float = 0.0f,
    val iterations: Int = 100000,
    val pointSize: Int = 1
) : IFSParams()

// Fractal Flame parameters
data class FractalFlameParams(
    val zoom: Float = 1.0f,
    val centerX: Float = 0.0f,
    val centerY: Float = 0.0f,
    val iterations: Int = 1000000,
    val samples: Int = 50,
    val gamma: Float = 2.2f,
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    // Transform coefficients for first affine transform
    val a1: Float = 0.5f,
    val b1: Float = 0.0f,
    val c1: Float = 0.0f,
    val d1: Float = 0.5f,
    val e1: Float = 0.0f,
    val f1: Float = 0.0f,
    // Transform coefficients for second affine transform
    val a2: Float = -0.5f,
    val b2: Float = 0.0f,
    val c2: Float = 0.5f,
    val d2: Float = 0.5f,
    val e2: Float = 0.5f,
    val f2: Float = 0.0f,
    // Transform weights
    val weight1: Float = 0.5f,
    val weight2: Float = 0.5f,
    // Variation types (0=linear, 1=sinusoidal, 2=spherical, 3=swirl, 4=horseshoe, 5=polar, 6=handkerchief, 7=heart, 8=disc, 9=spiral, 10=hyperbolic, 11=diamond)
    val variation1: Int = 0,
    val variation2: Int = 1,
    // Variation weights
    val varWeight1: Float = 1.0f,
    val varWeight2: Float = 1.0f
) : FractalParams()

// Variation types enum for reference
enum class FlameVariationType(val id: Int, val displayName: String) {
    LINEAR(0, "Linear"),
    SINUSOIDAL(1, "Sinusoidal"),
    SPHERICAL(2, "Spherical"),
    SWIRL(3, "Swirl"),
    HORSESHOE(4, "Horseshoe"),
    POLAR(5, "Polar"),
    HANDKERCHIEF(6, "Handkerchief"),
    HEART(7, "Heart"),
    DISC(8, "Disc"),
    SPIRAL(9, "Spiral"),
    HYPERBOLIC(10, "Hyperbolic"),
    DIAMOND(11, "Diamond"),
    EX(12, "Ex"),
    JULIA(13, "Julia"),
    BENT(14, "Bent"),
    WAVES(15, "Waves"),
    FISHEYE(16, "Fisheye"),
    POPCORN(17, "Popcorn"),
    EXPONENTIAL(18, "Exponential"),
    POWER(19, "Power"),
    COSINE(20, "Cosine"),
    RINGS(21, "Rings"),
    FAN(22, "Fan"),
    BLOB(23, "Blob"),
    PDJ(24, "PDJ"),
    FAN2(25, "Fan2"),
    RINGS2(26, "Rings2"),
    EYEFISH(27, "Eyefish"),
    BUBBLE(28, "Bubble"),
    CYLINDER(29, "Cylinder"),
    PERSPECTIVE(30, "Perspective"),
    NOISE(31, "Noise"),
    JULIAN(32, "Julian"),
    JULIASCOPE(33, "Juliascope"),
    BLUR(34, "Blur"),
    GAUSSIAN_BLUR(35, "Gaussian Blur"),
    RADIAL_BLUR(36, "Radial Blur"),
    PIE(37, "Pie"),
    NGON(38, "Ngon"),
    CURL(39, "Curl"),
    RECTANGLES(40, "Rectangles"),
    ARCH(41, "Arch"),
    TANGENT(42, "Tangent"),
    SQUARE(43, "Square"),
    RAYS(44, "Rays"),
    BLADE(45, "Blade"),
    SECANT2(46, "Secant2"),
    TWINTRIAN(47, "Twintrian"),
    CROSS(48, "Cross"),
    DISC2(49, "Disc2"),
    SUPER_SHAPE(50, "Super Shape"),
    FLOWER(51, "Flower"),
    CONIC(52, "Conic"),
    PARABOLA(53, "Parabola"),
    BENT2(54, "Bent2"),
    BIPOLAR(55, "Bipolar"),
    BOARDERS(56, "Boarders"),
    BUTTERFLY(57, "Butterfly"),
    CELL(58, "Cell"),
    CPOW(59, "Cpow"),
    CURVE(60, "Curve"),
    EDISC(61, "Edisc"),
    ELLIPTIC(62, "Elliptic"),
    ESCHER(63, "Escher"),
    FOCI(64, "Foci"),
    LAZYSUSAN(65, "Lazy Susan"),
    LOONIE(66, "Loonie"),
    PRE_BLUR(67, "Pre Blur"),
    MODULUS(68, "Modulus"),
    OSCILLOSCOPE(69, "Oscilloscope"),
    POLAR2(70, "Polar2"),
    POPCORN2(71, "Popcorn2"),
    SCRY(72, "Scry"),
    SEPARATION(73, "Separation"),
    SPLIT(74, "Split"),
    SPLITS(75, "Splits"),
    STRIPES(76, "Stripes"),
    WEDGE(77, "Wedge"),
    WEDGE_JULIA(78, "Wedge Julia"),
    WEDGE_SPH(79, "Wedge Sph"),
    WHORL(80, "Whorl"),
    WAVES2(81, "Waves2"),
    EXP(82, "Exp"),
    LOG(83, "Log"),
    SIN(84, "Sin"),
    COS(85, "Cos"),
    TAN(86, "Tan"),
    SEC(87, "Sec"),
    CSC(88, "Csc"),
    COT(89, "Cot"),
    SINH(90, "Sinh"),
    COSH(91, "Cosh"),
    TANH(92, "Tanh"),
    SECH(93, "Sech"),
    CSCH(94, "Csch"),
    COTH(95, "Coth"),
    AUGER(96, "Auger"),
    FLUX(97, "Flux"),
    MOBIUS(98, "Mobius")
}
