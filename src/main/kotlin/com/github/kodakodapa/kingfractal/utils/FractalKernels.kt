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

        void variation_ex(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);

            float n0 = sin(theta + r);
            float n1 = cos(theta - r);

            float m0 = n0 * n0 * n0 * r;
            float m1 = n1 * n1 * n1 * r;

            *outX = weight * (m0 + m1);
            *outY = weight * (m0 - m1);
        }

        void variation_julia(float tx, float ty, float weight, float* outX, float* outY, uint* seed) {
            float theta = 0.5f * atan2(ty, tx);
            float r = sqrt(tx * tx + ty * ty);

            // Random bit using seed
            *seed = *seed * 1664525 + 1013904223;
            if (*seed & 1) {
                theta += M_PI;
            }

            r = weight * sqrt(r);

            *outX = r * cos(theta);
            *outY = r * sin(theta);
        }

        void variation_bent(float tx, float ty, float weight, float* outX, float* outY) {
            float nx = tx;
            float ny = ty;

            if (nx < 0.0f) nx = nx * 2.0f;
            if (ny < 0.0f) ny = ny / 2.0f;

            *outX = weight * nx;
            *outY = weight * ny;
        }

        void variation_waves(float tx, float ty, float weight, float c10, float c11, float dx2, float dy2, float* outX, float* outY) {
            float nx = tx + c10 * sin(ty * dx2);
            float ny = ty + c11 * sin(tx * dy2);

            *outX = weight * nx;
            *outY = weight * ny;
        }

        void variation_fisheye(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            r = 2.0f * weight / (r + 1.0f);

            *outX = r * ty;
            *outY = r * tx;
        }

        void variation_popcorn(float tx, float ty, float weight, float c20, float c21, float* outX, float* outY) {
            float dx = tan(3.0f * ty);
            float dy = tan(3.0f * tx);

            float nx = tx + c20 * sin(dx);
            float ny = ty + c21 * sin(dy);

            *outX = weight * nx;
            *outY = weight * ny;
        }

        void variation_exponential(float tx, float ty, float weight, float* outX, float* outY) {
            float dx = weight * exp(tx - 1.0f);
            float dy = M_PI * ty;

            *outX = dx * cos(dy);
            *outY = dx * sin(dy);
        }

        void variation_power(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);
            float sina = sin(theta);
            float cosa = cos(theta);

            r = weight * pow(r, sina);

            *outX = r * cosa;
            *outY = r * sina;
        }

        void variation_cosine(float tx, float ty, float weight, float* outX, float* outY) {
            float a = tx * M_PI;

            float nx = cos(a) * cosh(ty);
            float ny = -sin(a) * sinh(ty);

            *outX = weight * nx;
            *outY = weight * ny;
        }

        void variation_rings(float tx, float ty, float weight, float c20, float* outX, float* outY) {
            float dx = c20 * c20 + 1e-6f;
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);

            r = weight * (fmod(r + dx, 2.0f * dx) - dx + r * (1.0f - dx));

            *outX = r * cos(theta);
            *outY = r * sin(theta);
        }

        void variation_fan(float tx, float ty, float weight, float c20, float c21, float* outX, float* outY) {
            float dx = M_PI * (c20 * c20 + 1e-6f);
            float dy = c21;
            float dx2 = 0.5f * dx;

            float theta = atan2(ty, tx);
            float r = weight * sqrt(tx * tx + ty * ty);

            theta += (fmod(theta + dy, dx) > dx2) ? -dx2 : dx2;

            *outX = r * cos(theta);
            *outY = r * sin(theta);
        }

        void variation_blob(float tx, float ty, float weight, float blob_low, float blob_high, float blob_waves, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);
            float sina = sin(theta);
            float cosa = cos(theta);

            float bdiff = blob_high - blob_low;
            r = r * (blob_low + bdiff * (0.5f + 0.5f * sin(blob_waves * theta)));

            *outX = weight * sina * r;
            *outY = weight * cosa * r;
        }

        void variation_pdj(float tx, float ty, float weight, float pdj_a, float pdj_b, float pdj_c, float pdj_d, float* outX, float* outY) {
            float nx1 = cos(pdj_b * tx);
            float nx2 = sin(pdj_c * tx);
            float ny1 = sin(pdj_a * ty);
            float ny2 = cos(pdj_d * ty);

            *outX = weight * (ny1 - nx1);
            *outY = weight * (nx2 - ny2);
        }

        void variation_fan2(float tx, float ty, float weight, float fan2_x, float fan2_y, float* outX, float* outY) {
            float dy = fan2_y;
            float dx = M_PI * (fan2_x * fan2_x + 1e-6f);
            float dx2 = 0.5f * dx;
            float theta = atan2(ty, tx);
            float r = weight * sqrt(tx * tx + ty * ty);

            float t = theta + dy - dx * (int)((theta + dy) / dx);

            if (t > dx2) {
                theta = theta - dx2;
            } else {
                theta = theta + dx2;
            }

            *outX = r * sin(theta);
            *outY = r * cos(theta);
        }

        void variation_rings2(float tx, float ty, float weight, float rings2_val, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);
            float sina = sin(theta);
            float cosa = cos(theta);

            float dx = rings2_val * rings2_val + 1e-6f;
            r += -2.0f * dx * (int)((r + dx) / (2.0f * dx)) + r * (1.0f - dx);

            *outX = weight * sina * r;
            *outY = weight * cosa * r;
        }

        void variation_eyefish(float tx, float ty, float weight, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            r = (weight * 2.0f) / (r + 1.0f);

            *outX = r * tx;
            *outY = r * ty;
        }

        void variation_bubble(float tx, float ty, float weight, float* outX, float* outY) {
            float sumsq = tx * tx + ty * ty;
            float r = weight / (0.25f * sumsq + 1.0f);

            *outX = r * tx;
            *outY = r * ty;
        }

        void variation_cylinder(float tx, float ty, float weight, float* outX, float* outY) {
            *outX = weight * sin(tx);
            *outY = weight * ty;
        }

        void variation_perspective(float tx, float ty, float weight, float perspective_dist, float perspective_angle, float* outX, float* outY) {
            float vfcos = cos(perspective_angle);
            float vsin = sin(perspective_angle);
            float t = 1.0f / (perspective_dist - ty * vsin);

            *outX = weight * perspective_dist * tx * t;
            *outY = weight * vfcos * ty * t;
        }

        void variation_noise(float tx, float ty, float weight, uint* seed, float* outX, float* outY) {
            // Generate two random numbers
            *seed = *seed * 1664525 + 1013904223;
            float tmpr = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * 2.0f * M_PI;

            *seed = *seed * 1664525 + 1013904223;
            float r = weight * ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            *outX = tx * r * cos(tmpr);
            *outY = ty * r * sin(tmpr);
        }

        void variation_julian(float tx, float ty, float weight, float julian_power, float julian_dist, uint* seed, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);

            // Random integer for branch selection
            *seed = *seed * 1664525 + 1013904223;
            int t_rnd = (int)(((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * julian_power);

            float tmpr = (theta + 2.0f * M_PI * t_rnd) / julian_power;
            float rr = weight * pow(r * r, julian_dist);

            *outX = rr * cos(tmpr);
            *outY = rr * sin(tmpr);
        }

        void variation_juliascope(float tx, float ty, float weight, float juliascope_power, float juliascope_dist, uint* seed, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);

            // Random integer for branch selection
            *seed = *seed * 1664525 + 1013904223;
            int t_rnd = (int)(((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * juliascope_power);

            float tmpr;
            if ((t_rnd & 1) == 0) {
                tmpr = (2.0f * M_PI * t_rnd + theta) / juliascope_power;
            } else {
                tmpr = (2.0f * M_PI * t_rnd - theta) / juliascope_power;
            }

            float rr = weight * pow(r * r, juliascope_dist);

            *outX = rr * cos(tmpr);
            *outY = rr * sin(tmpr);
        }

        void variation_blur(float tx, float ty, float weight, uint* seed, float* outX, float* outY) {
            // Generate random angle
            *seed = *seed * 1664525 + 1013904223;
            float tmpr = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * 2.0f * M_PI;

            // Generate random radius
            *seed = *seed * 1664525 + 1013904223;
            float r = weight * ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            *outX = r * cos(tmpr);
            *outY = r * sin(tmpr);
        }

        void variation_gaussian_blur(float tx, float ty, float weight, uint* seed, float* outX, float* outY) {
            // Generate random angle
            *seed = *seed * 1664525 + 1013904223;
            float ang = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * 2.0f * M_PI;

            // Generate Gaussian-distributed radius using sum of 4 uniform random numbers
            float r = 0.0f;
            for (int i = 0; i < 4; i++) {
                *seed = *seed * 1664525 + 1013904223;
                r += ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);
            }
            r = weight * (r - 2.0f);

            *outX = r * cos(ang);
            *outY = r * sin(ang);
        }

        void variation_radial_blur(float tx, float ty, float weight, float radial_blur_angle, uint* seed, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);

            // Generate Gaussian-distributed random number
            float rndG = 0.0f;
            for (int i = 0; i < 4; i++) {
                *seed = *seed * 1664525 + 1013904223;
                rndG += ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);
            }
            rndG = weight * (rndG - 2.0f);

            float tmpa = theta + radial_blur_angle * rndG;
            float rz = 0.3f * rndG - 1.0f;  // Default zoom factor

            *outX = r * cos(tmpa) + rz * tx;
            *outY = r * sin(tmpa) + rz * ty;
        }

        void variation_pie(float tx, float ty, float weight, float pie_slices, float pie_rotation, float pie_thickness, uint* seed, float* outX, float* outY) {
            // Random slice selection
            *seed = *seed * 1664525 + 1013904223;
            int sl = (int)(((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * pie_slices + 0.5f);

            // Random thickness
            *seed = *seed * 1664525 + 1013904223;
            float rand_thick = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            float a = pie_rotation + 2.0f * M_PI * (sl + rand_thick * pie_thickness) / pie_slices;

            // Random radius
            *seed = *seed * 1664525 + 1013904223;
            float r = weight * ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            *outX = r * cos(a);
            *outY = r * sin(a);
        }

        void variation_ngon(float tx, float ty, float weight, float ngon_power, float ngon_sides, float ngon_corners, float ngon_circle, float* outX, float* outY) {
            float sumsq = tx * tx + ty * ty;
            float r_factor = pow(sumsq, ngon_power / 2.0f);

            float theta = atan2(ty, tx);
            float b = 2.0f * M_PI / ngon_sides;

            float phi = theta - (b * floor(theta / b));
            if (phi > b / 2.0f) {
                phi -= b;
            }

            float amp = ngon_corners * (1.0f / (cos(phi) + 1e-6f) - 1.0f) + ngon_circle;
            amp /= (r_factor + 1e-6f);

            *outX = weight * tx * amp;
            *outY = weight * ty * amp;
        }

        void variation_curl(float tx, float ty, float weight, float curl_c1, float curl_c2, float* outX, float* outY) {
            float re = 1.0f + curl_c1 * tx + curl_c2 * (tx * tx - ty * ty);
            float im = curl_c1 * ty + 2.0f * curl_c2 * tx * ty;

            float r = weight / (re * re + im * im);

            *outX = (tx * re + ty * im) * r;
            *outY = (ty * re - tx * im) * r;
        }

        void variation_rectangles(float tx, float ty, float weight, float rect_x, float rect_y, float* outX, float* outY) {
            if (rect_x == 0.0f) {
                *outX = weight * tx;
            } else {
                *outX = weight * ((2.0f * floor(tx / rect_x) + 1.0f) * rect_x - tx);
            }

            if (rect_y == 0.0f) {
                *outY = weight * ty;
            } else {
                *outY = weight * ((2.0f * floor(ty / rect_y) + 1.0f) * rect_y - ty);
            }
        }

        void variation_arch(float tx, float ty, float weight, uint* seed, float* outX, float* outY) {
            *seed = *seed * 1664525 + 1013904223;
            float ang = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * weight * M_PI;

            *outX = weight * sin(ang);
            *outY = weight * (sin(ang) * sin(ang)) / cos(ang);
        }

        void variation_tangent(float tx, float ty, float weight, float* outX, float* outY) {
            *outX = weight * sin(tx) / cos(ty);
            *outY = weight * tan(ty);
        }

        void variation_square(float tx, float ty, float weight, uint* seed, float* outX, float* outY) {
            *seed = *seed * 1664525 + 1013904223;
            float rand1 = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) - 0.5f;

            *seed = *seed * 1664525 + 1013904223;
            float rand2 = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) - 0.5f;

            *outX = weight * rand1;
            *outY = weight * rand2;
        }

        void variation_rays(float tx, float ty, float weight, uint* seed, float* outX, float* outY) {
            float sumsq = tx * tx + ty * ty;

            *seed = *seed * 1664525 + 1013904223;
            float ang = weight * ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * M_PI;

            float r = weight / (sumsq + 1e-6f);
            float tanr = weight * tan(ang) * r;

            *outX = tanr * cos(tx);
            *outY = tanr * sin(ty);
        }

        void variation_blade(float tx, float ty, float weight, uint* seed, float* outX, float* outY) {
            float sqrt_r = sqrt(tx * tx + ty * ty);

            *seed = *seed * 1664525 + 1013904223;
            float r = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * weight * sqrt_r;

            *outX = weight * tx * (cos(r) + sin(r));
            *outY = weight * tx * (cos(r) - sin(r));
        }

        void variation_secant2(float tx, float ty, float weight, float* outX, float* outY) {
            float r = weight * sqrt(tx * tx + ty * ty);
            float cr = cos(r);
            float icr = 1.0f / cr;

            *outX = weight * tx;

            if (cr < 0.0f) {
                *outY = weight * (icr + 1.0f);
            } else {
                *outY = weight * (icr - 1.0f);
            }
        }

        void variation_twintrian(float tx, float ty, float weight, uint* seed, float* outX, float* outY) {
            float sqrt_r = sqrt(tx * tx + ty * ty);

            *seed = *seed * 1664525 + 1013904223;
            float r = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * weight * sqrt_r;

            float sinr = sin(r);
            float cosr = cos(r);
            float diff = log10(sinr * sinr) + cosr;

            // Handle bad values (log of negative/zero)
            if (isnan(diff) || isinf(diff)) {
                diff = -30.0f;
            }

            *outX = weight * tx * diff;
            *outY = weight * tx * (diff - sinr * M_PI);
        }

        void variation_cross(float tx, float ty, float weight, float* outX, float* outY) {
            float s = tx * tx - ty * ty;
            float r = weight * sqrt(1.0f / (s * s + 1e-6f));

            *outX = tx * r;
            *outY = ty * r;
        }

        void variation_disc2(float tx, float ty, float weight, float disc2_rot, float disc2_twist, float* outX, float* outY) {
            float theta = atan2(ty, tx);
            float t = disc2_twist * M_PI * (tx + ty);
            float r = weight * theta / M_PI;

            *outX = (sin(t) + disc2_rot) * r;
            *outY = (cos(t) + disc2_twist) * r;
        }

        void variation_super_shape(float tx, float ty, float weight, float ss_m, float ss_n1, float ss_n2, float ss_n3, float ss_rnd, float ss_holes, uint* seed, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = ss_m * atan2(ty, tx) / 4.0f + M_PI / 4.0f;

            float t1 = pow(fabs(cos(theta)), ss_n2);
            float t2 = pow(fabs(sin(theta)), ss_n3);

            *seed = *seed * 1664525 + 1013904223;
            float myrnd = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            float rr = weight * ((ss_rnd * myrnd + (1.0f - ss_rnd) * r) - ss_holes) * pow(t1 + t2, -1.0f / ss_n1) / r;

            *outX = rr * tx;
            *outY = rr * ty;
        }

        void variation_flower(float tx, float ty, float weight, float flower_petals, float flower_holes, uint* seed, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);

            *seed = *seed * 1664525 + 1013904223;
            float rand_val = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            float rr = weight * (rand_val - flower_holes) * cos(flower_petals * theta) / r;

            *outX = rr * tx;
            *outY = rr * ty;
        }

        void variation_conic(float tx, float ty, float weight, float conic_eccentricity, float conic_holes, uint* seed, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float ct = tx / r;

            *seed = *seed * 1664525 + 1013904223;
            float rand_val = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            float rr = weight * (rand_val - conic_holes) * conic_eccentricity / (1.0f + conic_eccentricity * ct) / r;

            *outX = rr * tx;
            *outY = rr * ty;
        }

        void variation_parabola(float tx, float ty, float weight, float parabola_height, float parabola_width, uint* seed, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);

            *seed = *seed * 1664525 + 1013904223;
            float rand1 = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            *seed = *seed * 1664525 + 1013904223;
            float rand2 = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            *outX = parabola_height * weight * sin(r) * sin(r) * rand1;
            *outY = parabola_width * weight * cos(r) * rand2;
        }

        void variation_bent2(float tx, float ty, float weight, float bent2_x, float bent2_y, float* outX, float* outY) {
            float nx = tx;
            float ny = ty;

            if (nx < 0.0f) nx = nx * bent2_x;
            if (ny < 0.0f) ny = ny * bent2_y;

            *outX = weight * nx;
            *outY = weight * ny;
        }

        void variation_bipolar(float tx, float ty, float weight, float bipolar_shift, float* outX, float* outY) {
            float x2y2 = tx * tx + ty * ty;
            float t = x2y2 + 1.0f;
            float x2 = 2.0f * tx;
            float ps = -M_PI_2 * bipolar_shift;
            float y = 0.5f * atan2(2.0f * ty, x2y2 - 1.0f) + ps;

            if (y > M_PI_2) {
                y = -M_PI_2 + fmod(y + M_PI_2, M_PI);
            } else if (y < -M_PI_2) {
                y = M_PI_2 - fmod(M_PI_2 - y, M_PI);
            }

            *outX = weight * 0.25f * M_2_PI * log((t + x2) / (t - x2));
            *outY = weight * M_2_PI * y;
        }

        void variation_boarders(float tx, float ty, float weight, uint* seed, float* outX, float* outY) {
            float roundX = rint(tx);
            float roundY = rint(ty);
            float offsetX = tx - roundX;
            float offsetY = ty - roundY;

            *seed = *seed * 1664525 + 1013904223;
            float rand_val = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            if (rand_val >= 0.75f) {
                *outX = weight * (offsetX * 0.5f + roundX);
                *outY = weight * (offsetY * 0.5f + roundY);
            } else {
                if (fabs(offsetX) >= fabs(offsetY)) {
                    if (offsetX >= 0.0f) {
                        *outX = weight * (offsetX * 0.5f + roundX + 0.25f);
                        *outY = weight * (offsetY * 0.5f + roundY + 0.25f * offsetY / offsetX);
                    } else {
                        *outX = weight * (offsetX * 0.5f + roundX - 0.25f);
                        *outY = weight * (offsetY * 0.5f + roundY - 0.25f * offsetY / offsetX);
                    }
                } else {
                    if (offsetY >= 0.0f) {
                        *outY = weight * (offsetY * 0.5f + roundY + 0.25f);
                        *outX = weight * (offsetX * 0.5f + roundX + offsetX / offsetY * 0.25f);
                    } else {
                        *outY = weight * (offsetY * 0.5f + roundY - 0.25f);
                        *outX = weight * (offsetX * 0.5f + roundX - offsetX / offsetY * 0.25f);
                    }
                }
            }
        }

        void variation_butterfly(float tx, float ty, float weight, float* outX, float* outY) {
            float wx = weight * 1.3029400317411197908970256609023f;
            float y2 = ty * 2.0f;
            float r = wx * sqrt(fabs(ty * tx) / (1e-6f + tx * tx + y2 * y2));

            *outX = r * tx;
            *outY = r * y2;
        }

        void variation_cell(float tx, float ty, float weight, float cell_size, float* outX, float* outY) {
            float inv_cell_size = 1.0f / cell_size;

            int x = (int)floor(tx * inv_cell_size);
            int y = (int)floor(ty * inv_cell_size);

            float dx = tx - x * cell_size;
            float dy = ty - y * cell_size;

            // Interleave cells
            if (y >= 0) {
                if (x >= 0) {
                    y *= 2;
                    x *= 2;
                } else {
                    y *= 2;
                    x = -(2 * x + 1);
                }
            } else {
                if (x >= 0) {
                    y = -(2 * y + 1);
                    x *= 2;
                } else {
                    y = -(2 * y + 1);
                    x = -(2 * x + 1);
                }
            }

            *outX = weight * (dx + x * cell_size);
            *outY = -weight * (dy + y * cell_size);
        }

        void variation_cpow(float tx, float ty, float weight, float cpow_r, float cpow_i, float cpow_power, uint* seed, float* outX, float* outY) {
            float a = atan2(ty, tx);
            float lnr = 0.5f * log(tx * tx + ty * ty);
            float va = 2.0f * M_PI / cpow_power;
            float vc = cpow_r / cpow_power;
            float vd = cpow_i / cpow_power;

            *seed = *seed * 1664525 + 1013904223;
            float rand_val = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);

            float ang = vc * a + vd * lnr + va * floor(cpow_power * rand_val);
            float m = weight * exp(vc * lnr - vd * a);

            *outX = m * cos(ang);
            *outY = m * sin(ang);
        }

        void variation_curve(float tx, float ty, float weight, float curve_xamp, float curve_yamp, float curve_xlength, float curve_ylength, float* outX, float* outY) {
            float pc_xlen = curve_xlength * curve_xlength;
            float pc_ylen = curve_ylength * curve_ylength;

            if (pc_xlen < 1e-20f) pc_xlen = 1e-20f;
            if (pc_ylen < 1e-20f) pc_ylen = 1e-20f;

            *outX = weight * (tx + curve_xamp * exp(-ty * ty / pc_xlen));
            *outY = weight * (ty + curve_yamp * exp(-tx * tx / pc_ylen));
        }

        void variation_edisc(float tx, float ty, float weight, float* outX, float* outY) {
            float sumsq = tx * tx + ty * ty;
            float tmp = sumsq + 1.0f;
            float tmp2 = 2.0f * tx;
            float r1 = sqrt(tmp + tmp2);
            float r2 = sqrt(tmp - tmp2);
            float xmax = (r1 + r2) * 0.5f;
            float a1 = log(xmax + sqrt(xmax - 1.0f));
            float a2 = -acos(tx / xmax);
            float w = weight / 11.57034632f;

            float snv = sin(a1);
            float csv = cos(a1);
            float snhu = sinh(a2);
            float cshu = cosh(a2);

            if (ty > 0.0f) snv = -snv;

            *outX = w * cshu * csv;
            *outY = w * snhu * snv;
        }

        void variation_elliptic(float tx, float ty, float weight, float* outX, float* outY) {
            float sumsq = tx * tx + ty * ty;
            float tmp = sumsq + 1.0f;
            float x2 = 2.0f * tx;
            float xmax = 0.5f * (sqrt(tmp + x2) + sqrt(tmp - x2));
            float a = tx / xmax;
            float b = 1.0f - a * a;
            float ssx = xmax - 1.0f;
            float w = weight / M_PI_2;

            if (b < 0.0f) {
                b = 0.0f;
            } else {
                b = sqrt(b);
            }

            if (ssx < 0.0f) {
                ssx = 0.0f;
            } else {
                ssx = sqrt(ssx);
            }

            *outX = w * atan2(a, b);

            if (ty > 0.0f) {
                *outY = w * log(xmax + ssx);
            } else {
                *outY = -w * log(xmax + ssx);
            }
        }

        void variation_escher(float tx, float ty, float weight, float escher_beta, float* outX, float* outY) {
            float a = atan2(ty, tx);
            float lnr = 0.5f * log(tx * tx + ty * ty);

            float seb = sin(escher_beta);
            float ceb = cos(escher_beta);

            float vc = 0.5f * (1.0f + ceb);
            float vd = 0.5f * seb;

            float m = weight * exp(vc * lnr - vd * a);
            float n = vc * a + vd * lnr;

            *outX = m * cos(n);
            *outY = m * sin(n);
        }

        void variation_foci(float tx, float ty, float weight, float* outX, float* outY) {
            float expx = exp(tx) * 0.5f;
            float expnx = 0.25f / expx;
            float sn = sin(ty);
            float cn = cos(ty);
            float tmp = weight / (expx + expnx - cn);

            *outX = tmp * (expx - expnx);
            *outY = tmp * sn;
        }

        void variation_lazysusan(float tx, float ty, float weight, float lazysusan_x, float lazysusan_y, float lazysusan_spin, float lazysusan_twist, float lazysusan_space, float* outX, float* outY) {
            float x = tx - lazysusan_x;
            float y = ty + lazysusan_y;
            float r = sqrt(x * x + y * y);

            if (r < weight) {
                float a = atan2(y, x) + lazysusan_spin + lazysusan_twist * (weight - r);
                r = weight * r;

                *outX = r * cos(a) + lazysusan_x;
                *outY = r * sin(a) - lazysusan_y;
            } else {
                r = weight * (1.0f + lazysusan_space / r);

                *outX = r * x + lazysusan_x;
                *outY = r * y - lazysusan_y;
            }
        }

        void variation_loonie(float tx, float ty, float weight, float* outX, float* outY) {
            float r2 = tx * tx + ty * ty;
            float w2 = weight * weight;

            if (r2 < w2) {
                float r = weight * sqrt(w2 / r2 - 1.0f);
                *outX = r * tx;
                *outY = r * ty;
            } else {
                *outX = weight * tx;
                *outY = weight * ty;
            }
        }

        void variation_pre_blur(float* tx, float* ty, float weight, uint* seed) {
            // Generate Gaussian-distributed random number
            float rndG = 0.0f;
            for (int i = 0; i < 4; i++) {
                *seed = *seed * 1664525 + 1013904223;
                rndG += ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF);
            }
            rndG = weight * (rndG - 2.0f);

            *seed = *seed * 1664525 + 1013904223;
            float rndA = ((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * 2.0f * M_PI;

            // Note: pre-blur modifies the input coordinates
            *tx += rndG * cos(rndA);
            *ty += rndG * sin(rndA);
        }

        void variation_modulus(float tx, float ty, float weight, float modulus_x, float modulus_y, float* outX, float* outY) {
            float xr = 2.0f * modulus_x;
            float yr = 2.0f * modulus_y;

            if (tx > modulus_x) {
                *outX = weight * (-modulus_x + fmod(tx + modulus_x, xr));
            } else if (tx < -modulus_x) {
                *outX = weight * (modulus_x - fmod(modulus_x - tx, xr));
            } else {
                *outX = weight * tx;
            }

            if (ty > modulus_y) {
                *outY = weight * (-modulus_y + fmod(ty + modulus_y, yr));
            } else if (ty < -modulus_y) {
                *outY = weight * (modulus_y - fmod(modulus_y - ty, yr));
            } else {
                *outY = weight * ty;
            }
        }

        void variation_oscilloscope(float tx, float ty, float weight, float oscope_separation, float oscope_frequency, float oscope_amplitude, float oscope_damping, float* outX, float* outY) {
            float tpf = 2.0f * M_PI * oscope_frequency;
            float t;

            if (oscope_damping == 0.0f) {
                t = oscope_amplitude * cos(tpf * tx) + oscope_separation;
            } else {
                t = oscope_amplitude * exp(-fabs(tx) * oscope_damping) * cos(tpf * tx) + oscope_separation;
            }

            if (fabs(ty) <= t) {
                *outX = weight * tx;
                *outY = -weight * ty;
            } else {
                *outX = weight * tx;
                *outY = weight * ty;
            }
        }

        void variation_polar2(float tx, float ty, float weight, float* outX, float* outY) {
            float p2v = weight / M_PI;
            float theta = atan2(ty, tx);
            float sumsq = tx * tx + ty * ty;

            *outX = p2v * theta;
            *outY = p2v * 0.5f * log(sumsq);
        }

        void variation_popcorn2(float tx, float ty, float weight, float popcorn2_x, float popcorn2_y, float popcorn2_c, float* outX, float* outY) {
            *outX = weight * (tx + popcorn2_x * sin(tan(ty * popcorn2_c)));
            *outY = weight * (ty + popcorn2_y * sin(tan(tx * popcorn2_c)));
        }

        void variation_scry(float tx, float ty, float weight, float* outX, float* outY) {
            float sumsq = tx * tx + ty * ty;
            float sqrt_r = sqrt(sumsq);
            float r = 1.0f / (sqrt_r * (sumsq + 1.0f / (weight + 1e-6f)));

            *outX = tx * r;
            *outY = ty * r;
        }

        void variation_separation(float tx, float ty, float weight, float separation_x, float separation_y, float separation_xinside, float separation_yinside, float* outX, float* outY) {
            float sx2 = separation_x * separation_x;
            float sy2 = separation_y * separation_y;

            if (tx > 0.0f) {
                *outX = weight * (sqrt(tx * tx + sx2) - tx * separation_xinside);
            } else {
                *outX = -weight * (sqrt(tx * tx + sx2) + tx * separation_xinside);
            }

            if (ty > 0.0f) {
                *outY = weight * (sqrt(ty * ty + sy2) - ty * separation_yinside);
            } else {
                *outY = -weight * (sqrt(ty * ty + sy2) + ty * separation_yinside);
            }
        }

        void variation_split(float tx, float ty, float weight, float split_xsize, float split_ysize, float* outX, float* outY) {
            if (cos(tx * split_xsize * M_PI) >= 0.0f) {
                *outY = weight * ty;
            } else {
                *outY = -weight * ty;
            }

            if (cos(ty * split_ysize * M_PI) >= 0.0f) {
                *outX = weight * tx;
            } else {
                *outX = -weight * tx;
            }
        }

        void variation_splits(float tx, float ty, float weight, float splits_x, float splits_y, float* outX, float* outY) {
            if (tx >= 0.0f) {
                *outX = weight * (tx + splits_x);
            } else {
                *outX = weight * (tx - splits_x);
            }

            if (ty >= 0.0f) {
                *outY = weight * (ty + splits_y);
            } else {
                *outY = weight * (ty - splits_y);
            }
        }

        void variation_stripes(float tx, float ty, float weight, float stripes_space, float stripes_warp, float* outX, float* outY) {
            float roundx = floor(tx + 0.5f);
            float offsetx = tx - roundx;

            *outX = weight * (offsetx * (1.0f - stripes_space) + roundx);
            *outY = weight * (ty + offsetx * offsetx * stripes_warp);
        }

        void variation_wedge(float tx, float ty, float weight, float wedge_angle, float wedge_hole, float wedge_count, float wedge_swirl, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);
            float a = theta + wedge_swirl * r;
            float c = floor((wedge_count * a + M_PI) / M_PI * 0.5f);

            float comp_fac = 1.0f - wedge_angle * wedge_count / M_PI * 0.5f;
            a = a * comp_fac + c * wedge_angle;

            r = weight * (r + wedge_hole);

            *outX = r * cos(a);
            *outY = r * sin(a);
        }

        void variation_wedge_julia(float tx, float ty, float weight, float wedge_julia_angle, float wedge_julia_count, float wedge_julia_power, float wedge_julia_dist, float wedge_julia_cf, uint* seed, float* outX, float* outY) {
            float sumsq = tx * tx + ty * ty;
            float r = weight * pow(sumsq, wedge_julia_dist);
            float theta = atan2(ty, tx);

            *seed = *seed * 1664525 + 1013904223;
            int t_rnd = (int)(((float)(*seed & 0xFFFFFF) / (float)0xFFFFFF) * wedge_julia_power);

            float a = (theta + 2.0f * M_PI * t_rnd) / wedge_julia_power;
            float c = floor((wedge_julia_count * a + M_PI) / M_PI * 0.5f);

            a = a * wedge_julia_cf + c * wedge_julia_angle;

            *outX = r * cos(a);
            *outY = r * sin(a);
        }

        void variation_wedge_sph(float tx, float ty, float weight, float wedge_sph_angle, float wedge_sph_hole, float wedge_sph_count, float wedge_sph_swirl, float* outX, float* outY) {
            float sqrt_r = sqrt(tx * tx + ty * ty);
            float r = 1.0f / (sqrt_r + 1e-6f);
            float theta = atan2(ty, tx);
            float a = theta + wedge_sph_swirl * r;
            float c = floor((wedge_sph_count * a + M_PI) / M_PI * 0.5f);

            float comp_fac = 1.0f - wedge_sph_angle * wedge_sph_count / M_PI * 0.5f;
            a = a * comp_fac + c * wedge_sph_angle;

            r = weight * (r + wedge_sph_hole);

            *outX = r * cos(a);
            *outY = r * sin(a);
        }

        void variation_whorl(float tx, float ty, float weight, float whorl_inside, float whorl_outside, float* outX, float* outY) {
            float r = sqrt(tx * tx + ty * ty);
            float theta = atan2(ty, tx);
            float a;

            if (r < weight) {
                a = theta + whorl_inside / (weight - r);
            } else {
                a = theta + whorl_outside / (weight - r);
            }

            *outX = weight * r * cos(a);
            *outY = weight * r * sin(a);
        }

        void variation_waves2(float tx, float ty, float weight, float waves2_scalex, float waves2_scaley, float waves2_freqx, float waves2_freqy, float* outX, float* outY) {
            *outX = weight * (tx + waves2_scalex * sin(ty * waves2_freqx));
            *outY = weight * (ty + waves2_scaley * sin(tx * waves2_freqy));
        }

        void variation_exp(float tx, float ty, float weight, float* outX, float* outY) {
            float expe = exp(tx);

            *outX = weight * expe * cos(ty);
            *outY = weight * expe * sin(ty);
        }

        void variation_log(float tx, float ty, float weight, float* outX, float* outY) {
            float sumsq = tx * tx + ty * ty;

            *outX = weight * 0.5f * log(sumsq);
            *outY = weight * atan2(ty, tx);
        }

        void variation_sin(float tx, float ty, float weight, float* outX, float* outY) {
            float sinsin = sin(tx);
            float sincos = cos(tx);
            float sinsinh = sinh(ty);
            float sincosh = cosh(ty);

            *outX = weight * sinsin * sincosh;
            *outY = weight * sincos * sinsinh;
        }

        void variation_cos(float tx, float ty, float weight, float* outX, float* outY) {
            float cossin = sin(tx);
            float coscos = cos(tx);
            float cossinh = sinh(ty);
            float coscosh = cosh(ty);

            *outX = weight * coscos * coscosh;
            *outY = -weight * cossin * cossinh;
        }

        void variation_tan(float tx, float ty, float weight, float* outX, float* outY) {
            float tansin = sin(2.0f * tx);
            float tancos = cos(2.0f * tx);
            float tansinh = sinh(2.0f * ty);
            float tancosh = cosh(2.0f * ty);
            float tanden = 1.0f / (tancos + tancosh);

            *outX = weight * tanden * tansin;
            *outY = weight * tanden * tansinh;
        }

        void variation_sec(float tx, float ty, float weight, float* outX, float* outY) {
            float secsin = sin(tx);
            float seccos = cos(tx);
            float secsinh = sinh(ty);
            float seccosh = cosh(ty);
            float secden = 2.0f / (cos(2.0f * tx) + cosh(2.0f * ty));

            *outX = weight * secden * seccos * seccosh;
            *outY = weight * secden * secsin * secsinh;
        }

        void variation_csc(float tx, float ty, float weight, float* outX, float* outY) {
            float cscsin = sin(tx);
            float csccos = cos(tx);
            float cscsinh = sinh(ty);
            float csccosh = cosh(ty);
            float cscden = 2.0f / (cosh(2.0f * ty) - cos(2.0f * tx));

            *outX = weight * cscden * cscsin * csccosh;
            *outY = -weight * cscden * csccos * cscsinh;
        }

        void variation_cot(float tx, float ty, float weight, float* outX, float* outY) {
            float cotsin = sin(2.0f * tx);
            float cotcos = cos(2.0f * tx);
            float cotsinh = sinh(2.0f * ty);
            float cotcosh = cosh(2.0f * ty);
            float cotden = 1.0f / (cotcosh - cotcos);

            *outX = weight * cotden * cotsin;
            *outY = weight * cotden * (-1.0f) * cotsinh;
        }

        void variation_sinh(float tx, float ty, float weight, float* outX, float* outY) {
            float sinhsin = sin(ty);
            float sinhcos = cos(ty);
            float sinhsinh = sinh(tx);
            float sinhcosh = cosh(tx);

            *outX = weight * sinhsinh * sinhcos;
            *outY = weight * sinhcosh * sinhsin;
        }

        // Apply variation based on type
        void apply_variation(int variationType, float tx, float ty, float weight, float* outX, float* outY, uint* seed) {
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
                case 12: variation_ex(tx, ty, weight, outX, outY); break;
                case 13: variation_julia(tx, ty, weight, outX, outY, seed); break;
                case 14: variation_bent(tx, ty, weight, outX, outY); break;
                case 15: variation_waves(tx, ty, weight, 0.1f, 0.1f, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 16: variation_fisheye(tx, ty, weight, outX, outY); break;
                case 17: variation_popcorn(tx, ty, weight, 0.1f, 0.1f, outX, outY); break;  // Default parameters
                case 18: variation_exponential(tx, ty, weight, outX, outY); break;
                case 19: variation_power(tx, ty, weight, outX, outY); break;
                case 20: variation_cosine(tx, ty, weight, outX, outY); break;
                case 21: variation_rings(tx, ty, weight, 0.2f, outX, outY); break;  // Default parameter
                case 22: variation_fan(tx, ty, weight, 0.5f, 0.3f, outX, outY); break;  // Default parameters
                case 23: variation_blob(tx, ty, weight, 0.2f, 1.0f, 2.0f, outX, outY); break;  // Default parameters
                case 24: variation_pdj(tx, ty, weight, 1.0f, 1.0f, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 25: variation_fan2(tx, ty, weight, 0.5f, 0.3f, outX, outY); break;  // Default parameters
                case 26: variation_rings2(tx, ty, weight, 0.5f, outX, outY); break;  // Default parameter
                case 27: variation_eyefish(tx, ty, weight, outX, outY); break;
                case 28: variation_bubble(tx, ty, weight, outX, outY); break;
                case 29: variation_cylinder(tx, ty, weight, outX, outY); break;
                case 30: variation_perspective(tx, ty, weight, 2.0f, 0.5f, outX, outY); break;  // Default parameters
                case 31: variation_noise(tx, ty, weight, seed, outX, outY); break;
                case 32: variation_julian(tx, ty, weight, 2.0f, 0.5f, seed, outX, outY); break;  // Default parameters
                case 33: variation_juliascope(tx, ty, weight, 2.0f, 0.5f, seed, outX, outY); break;  // Default parameters
                case 34: variation_blur(tx, ty, weight, seed, outX, outY); break;
                case 35: variation_gaussian_blur(tx, ty, weight, seed, outX, outY); break;
                case 36: variation_radial_blur(tx, ty, weight, 0.5f, seed, outX, outY); break;  // Default parameter
                case 37: variation_pie(tx, ty, weight, 6.0f, 0.0f, 0.5f, seed, outX, outY); break;  // Default parameters
                case 38: variation_ngon(tx, ty, weight, 2.0f, 5.0f, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 39: variation_curl(tx, ty, weight, 1.0f, 0.1f, outX, outY); break;  // Default parameters
                case 40: variation_rectangles(tx, ty, weight, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 41: variation_arch(tx, ty, weight, seed, outX, outY); break;
                case 42: variation_tangent(tx, ty, weight, outX, outY); break;
                case 43: variation_square(tx, ty, weight, seed, outX, outY); break;
                case 44: variation_rays(tx, ty, weight, seed, outX, outY); break;
                case 45: variation_blade(tx, ty, weight, seed, outX, outY); break;
                case 46: variation_secant2(tx, ty, weight, outX, outY); break;
                case 47: variation_twintrian(tx, ty, weight, seed, outX, outY); break;
                case 48: variation_cross(tx, ty, weight, outX, outY); break;
                case 49: variation_disc2(tx, ty, weight, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 50: variation_super_shape(tx, ty, weight, 4.0f, 1.0f, 1.0f, 1.0f, 0.5f, 0.0f, seed, outX, outY); break;  // Default parameters
                case 51: variation_flower(tx, ty, weight, 4.0f, 0.3f, seed, outX, outY); break;  // Default parameters
                case 52: variation_conic(tx, ty, weight, 1.0f, 0.0f, seed, outX, outY); break;  // Default parameters
                case 53: variation_parabola(tx, ty, weight, 1.0f, 1.0f, seed, outX, outY); break;  // Default parameters
                case 54: variation_bent2(tx, ty, weight, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 55: variation_bipolar(tx, ty, weight, 0.0f, outX, outY); break;  // Default parameter
                case 56: variation_boarders(tx, ty, weight, seed, outX, outY); break;
                case 57: variation_butterfly(tx, ty, weight, outX, outY); break;
                case 58: variation_cell(tx, ty, weight, 1.0f, outX, outY); break;  // Default parameter
                case 59: variation_cpow(tx, ty, weight, 1.0f, 0.0f, 2.0f, seed, outX, outY); break;  // Default parameters
                case 60: variation_curve(tx, ty, weight, 0.2f, 0.2f, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 61: variation_edisc(tx, ty, weight, outX, outY); break;
                case 62: variation_elliptic(tx, ty, weight, outX, outY); break;
                case 63: variation_escher(tx, ty, weight, 0.4f, outX, outY); break;  // Default parameter
                case 64: variation_foci(tx, ty, weight, outX, outY); break;
                case 65: variation_lazysusan(tx, ty, weight, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 66: variation_loonie(tx, ty, weight, outX, outY); break;
                case 67: *outX = tx; *outY = ty; break;  // pre_blur is handled separately as it modifies input
                case 68: variation_modulus(tx, ty, weight, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 69: variation_oscilloscope(tx, ty, weight, 1.0f, 1.0f, 1.0f, 0.0f, outX, outY); break;  // Default parameters
                case 70: variation_polar2(tx, ty, weight, outX, outY); break;
                case 71: variation_popcorn2(tx, ty, weight, 0.1f, 0.1f, 3.0f, outX, outY); break;  // Default parameters
                case 72: variation_scry(tx, ty, weight, outX, outY); break;
                case 73: variation_separation(tx, ty, weight, 1.0f, 1.0f, 0.0f, 0.0f, outX, outY); break;  // Default parameters
                case 74: variation_split(tx, ty, weight, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 75: variation_splits(tx, ty, weight, 0.5f, 0.5f, outX, outY); break;  // Default parameters
                case 76: variation_stripes(tx, ty, weight, 0.5f, 0.0f, outX, outY); break;  // Default parameters
                case 77: variation_wedge(tx, ty, weight, 0.5f, 0.0f, 1.0f, 0.0f, outX, outY); break;  // Default parameters
                case 78: variation_wedge_julia(tx, ty, weight, 0.5f, 1.0f, 2.0f, 0.5f, 1.0f, seed, outX, outY); break;  // Default parameters
                case 79: variation_wedge_sph(tx, ty, weight, 0.5f, 0.0f, 1.0f, 0.0f, outX, outY); break;  // Default parameters
                case 80: variation_whorl(tx, ty, weight, 0.5f, 0.5f, outX, outY); break;  // Default parameters
                case 81: variation_waves2(tx, ty, weight, 0.1f, 0.1f, 1.0f, 1.0f, outX, outY); break;  // Default parameters
                case 82: variation_exp(tx, ty, weight, outX, outY); break;
                case 83: variation_log(tx, ty, weight, outX, outY); break;
                case 84: variation_sin(tx, ty, weight, outX, outY); break;
                case 85: variation_cos(tx, ty, weight, outX, outY); break;
                case 86: variation_tan(tx, ty, weight, outX, outY); break;
                case 87: variation_sec(tx, ty, weight, outX, outY); break;
                case 88: variation_csc(tx, ty, weight, outX, outY); break;
                case 89: variation_cot(tx, ty, weight, outX, outY); break;
                case 90: variation_sinh(tx, ty, weight, outX, outY); break;
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
                        apply_variation(variation1, newX, newY, varWeight1, &varX, &varY, &seed);
                    } else {
                        // Apply second affine transform
                        newX = a2 * x + b2 * y + c2;
                        newY = d2 * x + e2 * y + f2;
                        // Apply second variation
                        apply_variation(variation2, newX, newY, varWeight2, &varX, &varY, &seed);
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
                        apply_variation(variation1, newX, newY, varWeight1, &varX, &varY, &seed);
                    } else {
                        // Apply second affine transform
                        newX = a2 * x + b2 * y + c2;
                        newY = d2 * x + e2 * y + f2;
                        // Apply second variation
                        apply_variation(variation2, newX, newY, varWeight2, &varX, &varY, &seed);
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