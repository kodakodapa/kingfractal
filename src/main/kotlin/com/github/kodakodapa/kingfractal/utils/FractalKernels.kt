package org.example.com.github.kodakodapa.kingfractal.utils


// Kernel sources
object FractalKernels {

    val mandelbrotKernel = """
        __kernel void mandelbrot(
            __global unsigned char* output,
            const int width,
            const int height,
            const float zoom,
            const float centerX,
            const float centerY,
            const int maxIterations
        ) {
            int x = get_global_id(0);
            int y = get_global_id(1);
            
            if (x >= width || y >= height) return;
            
            // Convert pixel coordinates to complex plane
            float real = (x - width/2.0f) / (zoom * width/4.0f) + centerX;
            float imag = (y - height/2.0f) / (zoom * height/4.0f) + centerY;
            
            // Mandelbrot iteration: z = z^2 + c
            float zReal = 0.0f;
            float zImag = 0.0f;
            int iterations = 0;
            
            while (zReal*zReal + zImag*zImag < 4.0f && iterations < maxIterations) {
                float temp = zReal*zReal - zImag*zImag + real;
                zImag = 2.0f * zReal * zImag + imag;
                zReal = temp;
                iterations++;
            }
            
            // Color based on iterations
            int pixelIndex = (y * width + x) * 3;
            if (iterations == maxIterations) {
                output[pixelIndex] = 0;     // R
                output[pixelIndex + 1] = 0; // G  
                output[pixelIndex + 2] = 0; // B
            } else {
                float t = (float)iterations / maxIterations;
                output[pixelIndex] = (unsigned char)(255 * t);           // R
                output[pixelIndex + 1] = (unsigned char)(255 * t * 0.5f); // G
                output[pixelIndex + 2] = (unsigned char)(255 * (1-t));    // B
            }
        }
    """.trimIndent()

    val juliaKernel = """
        __kernel void julia(
            __global unsigned char* output,
            const int width,
            const int height,
            const float zoom,
            const float centerX,
            const float centerY,
            const float juliaReal,
            const float juliaImag,
            const int maxIterations
        ) {
            int x = get_global_id(0);
            int y = get_global_id(1);
            
            if (x >= width || y >= height) return;
            
            // Convert pixel coordinates to complex plane
            float zReal = (x - width/2.0f) / (zoom * width/4.0f) + centerX;
            float zImag = (y - height/2.0f) / (zoom * height/4.0f) + centerY;
            
            // Julia set iteration: z = z^2 + c (where c is constant)
            int iterations = 0;
            
            while (zReal*zReal + zImag*zImag < 4.0f && iterations < maxIterations) {
                float temp = zReal*zReal - zImag*zImag + juliaReal;
                zImag = 2.0f * zReal * zImag + juliaImag;
                zReal = temp;
                iterations++;
            }
            
            // Color based on iterations (different color scheme than Mandelbrot)
            int pixelIndex = (y * width + x) * 3;
            if (iterations == maxIterations) {
                output[pixelIndex] = 0;     // R
                output[pixelIndex + 1] = 0; // G
                output[pixelIndex + 2] = 0; // B
            } else {
                float t = (float)iterations / maxIterations;
                output[pixelIndex] = (unsigned char)(255 * (1-t));       // R
                output[pixelIndex + 1] = (unsigned char)(255 * t * t);   // G
                output[pixelIndex + 2] = (unsigned char)(255 * t);       // B
            }
        }
    """.trimIndent()
}