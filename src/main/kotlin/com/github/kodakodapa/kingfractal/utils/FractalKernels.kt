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

    val buddhabrotKernel = """
        __kernel void buddhabrot(
            __global uint* output,
            const int width,
            const int height,
            const float zoom,
            const float centerX,
            const float centerY,
            const int maxIterations,
            const int sampleCount,
            __global uint* randomStates
        ) {
            int gid = get_global_id(0);

            // Simple linear congruential generator for random numbers
            uint seed = randomStates[gid] + gid * 1103515245 + 12345;

            int samplesPerWorker = sampleCount / get_global_size(0);
            for (int sample = 0; sample < samplesPerWorker; sample++) {
                // Generate random starting point with better distribution
                seed = seed * 1664525 + 1013904223; // Better LCG constants
                float rand1 = (float)(seed & 0xFFFFFF) / (float)0xFFFFFF;
                seed = seed * 1664525 + 1013904223;
                float rand2 = (float)(seed & 0xFFFFFF) / (float)0xFFFFFF;

                float cReal = (rand1 - 0.5f) * 3.0f; // Sample from [-1.5, 1.5]
                float cImag = (rand2 - 0.5f) * 3.0f;

                // Check if this point escapes (anti-Buddhabrot)
                float zReal = 0.0f;
                float zImag = 0.0f;
                int testIterations = 0;
                bool escapes = false;

                // Test if point escapes
                while (testIterations < maxIterations) {
                    float magnitude = zReal*zReal + zImag*zImag;
                    if (magnitude > 4.0f) {
                        escapes = true;
                        break;
                    }
                    float temp = zReal*zReal - zImag*zImag + cReal;
                    zImag = 2.0f * zReal * zImag + cImag;
                    zReal = temp;
                    testIterations++;
                }

                // If point escapes, trace its orbit and accumulate hits
                if (escapes) {
                    zReal = 0.0f;
                    zImag = 0.0f;
                    int iterations = 0;

                    while (iterations < maxIterations) {
                        float magnitude = zReal*zReal + zImag*zImag;
                        if (magnitude > 4.0f) break;

                        // Convert complex coordinate to pixel coordinate
                        int pixelX = (int)((zReal - centerX) * zoom * width/4.0f + width/2.0f);
                        int pixelY = (int)((zImag - centerY) * zoom * height/4.0f + height/2.0f);

                        // Check bounds and accumulate hit
                        if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                            int pixelIndex = pixelY * width + pixelX;
                            // Use atomic add to safely accumulate hits across work groups
                            atomic_add((volatile __global uint*)&output[pixelIndex], 1);
                        }

                        float temp = zReal*zReal - zImag*zImag + cReal;
                        zImag = 2.0f * zReal * zImag + cImag;
                        zReal = temp;
                        iterations++;
                    }
                }
            }

            // Store updated random state
            randomStates[gid] = seed;
        }
    """.trimIndent()

    val fractalFlameKernel = """
        // Variation function implementations
        void variation_linear(float tx, float ty, float weight, float* outX, float* outY) {
            *outX = weight * tx;
            *outY = weight * ty;
        }

        void variation_sinusoidal(float tx, float ty, float weight, float* outX, float* outY) {
            *outX = weight * sin(tx);
            *outY = weight * sin(ty);
        }

        void variation_spherical(float tx, float ty, float weight, float* outX, float* outY) {
            float r2 = tx * tx + ty * ty;
            float r = weight / (r2 + 1e-6f);
            *outX = r * tx;
            *outY = r * ty;
        }

        void variation_swirl(float tx, float ty, float weight, float* outX, float* outY) {
            float r2 = tx * tx + ty * ty;
            float c1 = sin(r2);
            float c2 = cos(r2);
            *outX = weight * (c1 * tx - c2 * ty);
            *outY = weight * (c2 * tx + c1 * ty);
        }

        void variation_horseshoe(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty) + 1e-6f;
            *outX = weight * (tx - ty) * (tx + ty) / r;
            *outY = weight * 2.0f * tx * ty / r;
        }

        void variation_polar(float tx, float ty, float weight, float* outX, float* outY) {
            *outX = weight * atan2(ty, tx) / M_PI;
            *outY = weight * (sqrt(tx * tx + ty * ty) - 1.0f);
        }

        void variation_handkerchief(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);
            *outX = weight * r * sin(theta + r);
            *outY = weight * r * cos(theta - r);
        }

        void variation_heart(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);
            *outX = weight * r * sin(theta * r);
            *outY = weight * (-r) * cos(theta * r);
        }

        void variation_disc(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);
            *outX = weight * theta / M_PI * sin(M_PI * r);
            *outY = weight * theta / M_PI * cos(M_PI * r);
        }

        void variation_spiral(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty) + 1e-6f;
            float theta = atan2(ty, tx);
            *outX = weight * (cos(theta) + sin(r)) / r;
            *outY = weight * (sin(theta) - cos(r)) / r;
        }

        void variation_hyperbolic(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty) + 1e-6f;
            float theta = atan2(ty, tx);
            *outX = weight * sin(theta) / r;
            *outY = weight * cos(theta) * r;
        }

        void variation_diamond(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);
            *outX = weight * sin(theta) * cos(r);
            *outY = weight * cos(theta) * sin(r);
        }

        // Apply variation based on type
        void apply_variation(int variationType, float tx, float ty, float weight, float* outX, float* outY) {
            switch(variationType) {
                case 0: variation_linear(tx, ty, weight, outX, outY); break;
                case 1: variation_sinusoidal(tx, ty, weight, outX, outY); break;
                case 2: variation_spherical(tx, ty, weight, outX, outY); break;
                case 3: variation_swirl(tx, ty, weight, outX, outY); break;
                case 4: variation_horseshoe(tx, ty, weight, outX, outY); break;
                case 5: variation_polar(tx, ty, weight, outX, outY); break;
                case 6: variation_handkerchief(tx, ty, weight, outX, outY); break;
                case 7: variation_heart(tx, ty, weight, outX, outY); break;
                case 8: variation_disc(tx, ty, weight, outX, outY); break;
                case 9: variation_spiral(tx, ty, weight, outX, outY); break;
                case 10: variation_hyperbolic(tx, ty, weight, outX, outY); break;
                case 11: variation_diamond(tx, ty, weight, outX, outY); break;
                default: variation_linear(tx, ty, weight, outX, outY); break;
            }
        }

        __kernel void fractal_flame(
            __global uint* histogram,
            const int width,
            const int height,
            const float zoom,
            const float centerX,
            const float centerY,
            const int iterations,
            const int samples,
            const float gamma,
            const float brightness,
            const float contrast,
            // First transform coefficients
            const float a1, const float b1, const float c1,
            const float d1, const float e1, const float f1,
            // Second transform coefficients
            const float a2, const float b2, const float c2,
            const float d2, const float e2, const float f2,
            // Transform weights
            const float weight1, const float weight2,
            // Variation types
            const int variation1, const int variation2,
            // Variation weights
            const float varWeight1, const float varWeight2,
            __global uint* randomStates
        ) {
            int gid = get_global_id(0);

            // Initialize random seed
            uint seed = randomStates[gid] + gid * 1103515245 + 12345;

            // Number of iterations per work item
            int iterationsPerWorker = iterations / get_global_size(0);

            for (int iter = 0; iter < iterationsPerWorker; iter++) {
                // Start with random point in [-1, 1] x [-1, 1]
                seed = seed * 1664525 + 1013904223;
                float x = ((float)(seed & 0xFFFFFF) / (float)0xFFFFFF) * 2.0f - 1.0f;
                seed = seed * 1664525 + 1013904223;
                float y = ((float)(seed & 0xFFFFFF) / (float)0xFFFFFF) * 2.0f - 1.0f;

                // Skip first few iterations to let system settle
                for (int skip = 0; skip < 20; skip++) {
                    // Choose random transform
                    seed = seed * 1664525 + 1013904223;
                    float randVal = (float)(seed & 0xFFFFFF) / (float)0xFFFFFF;

                    float newX, newY, varX, varY;
                    if (randVal < weight1) {
                        // Apply first affine transform
                        newX = a1 * x + b1 * y + c1;
                        newY = d1 * x + e1 * y + f1;
                        // Apply first variation
                        apply_variation(variation1, newX, newY, varWeight1, &varX, &varY);
                    } else {
                        // Apply second affine transform
                        newX = a2 * x + b2 * y + c2;
                        newY = d2 * x + e2 * y + f2;
                        // Apply second variation
                        apply_variation(variation2, newX, newY, varWeight2, &varX, &varY);
                    }

                    x = varX;
                    y = varY;
                }

                // Now iterate and plot points
                for (int i = 0; i < samples; i++) {
                    // Choose random transform
                    seed = seed * 1664525 + 1013904223;
                    float randVal = (float)(seed & 0xFFFFFF) / (float)0xFFFFFF;

                    float newX, newY, varX, varY;
                    if (randVal < weight1) {
                        // Apply first affine transform
                        newX = a1 * x + b1 * y + c1;
                        newY = d1 * x + e1 * y + f1;
                        // Apply first variation
                        apply_variation(variation1, newX, newY, varWeight1, &varX, &varY);
                    } else {
                        // Apply second affine transform
                        newX = a2 * x + b2 * y + c2;
                        newY = d2 * x + e2 * y + f2;
                        // Apply second variation
                        apply_variation(variation2, newX, newY, varWeight2, &varX, &varY);
                    }

                    x = varX;
                    y = varY;

                    // Transform to screen coordinates
                    float screenX = ((x - centerX) * zoom + 1.0f) * width * 0.5f;
                    float screenY = ((y - centerY) * zoom + 1.0f) * height * 0.5f;

                    // Check bounds and plot
                    if (screenX >= 0 && screenX < width && screenY >= 0 && screenY < height) {
                        int pixelIndex = (int)screenY * width + (int)screenX;
                        if (pixelIndex >= 0 && pixelIndex < width * height) {
                            atomic_add((volatile __global uint*)&histogram[pixelIndex], 1);
                        }
                    }
                }
            }

            // Update random state
            randomStates[gid] = seed;
        }
    """.trimIndent()
}