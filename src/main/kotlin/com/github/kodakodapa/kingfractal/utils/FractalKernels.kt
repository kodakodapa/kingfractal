package com.github.kodakodapa.kingfractal.utils


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
            float magnitude = 0.0f;

            while (iterations < maxIterations) {
                magnitude = zReal*zReal + zImag*zImag;
                if (magnitude > 4.0f) break;

                float temp = zReal*zReal - zImag*zImag + real;
                zImag = 2.0f * zReal * zImag + imag;
                zReal = temp;
                iterations++;
            }

            // Calculate smooth iteration count for better color mapping
            float smoothIterations = (float)iterations;
            if (iterations < maxIterations) {
                // Add fractional part for smooth coloring
                smoothIterations += 1.0f - log2(log2(magnitude));
            }

            // Normalize to 0-255 range for palette indexing
            float normalizedValue = (smoothIterations / (float)maxIterations) * 255.0f;
            normalizedValue = clamp(normalizedValue, 0.0f, 255.0f);

            int pixelIndex = (y * width + x) * 4;
            output[pixelIndex]     = (unsigned char)normalizedValue; // A
            output[pixelIndex + 1] = (unsigned char)normalizedValue; // R
            output[pixelIndex + 2] = (unsigned char)normalizedValue; // G
            output[pixelIndex + 3] = (unsigned char)normalizedValue; // B
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
            float magnitude = 0.0f;

            while (iterations < maxIterations) {
                magnitude = zReal*zReal + zImag*zImag;
                if (magnitude > 4.0f) break;

                float temp = zReal*zReal - zImag*zImag + juliaReal;
                zImag = 2.0f * zReal * zImag + juliaImag;
                zReal = temp;
                iterations++;
            }

            // Calculate smooth iteration count for better color mapping
            float smoothIterations = (float)iterations;
            if (iterations < maxIterations) {
                // Add fractional part for smooth coloring
                smoothIterations += 1.0f - log2(log2(magnitude));
            }

            // Normalize to 0-255 range for palette indexing
            float normalizedValue = (smoothIterations / (float)maxIterations) * 255.0f;
            normalizedValue = clamp(normalizedValue, 0.0f, 255.0f);

            int pixelIndex = (y * width + x) * 4;
            output[pixelIndex]     = (unsigned char)normalizedValue; // A
            output[pixelIndex + 1] = (unsigned char)normalizedValue; // R
            output[pixelIndex + 2] = (unsigned char)normalizedValue; // G
            output[pixelIndex + 3] = (unsigned char)normalizedValue; // B
        }
    """.trimIndent()
}