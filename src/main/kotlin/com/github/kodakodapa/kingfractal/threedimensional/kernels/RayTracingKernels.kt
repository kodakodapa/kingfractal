package com.github.kodakodapa.kingfractal.threedimensional.kernels

object RayTracingKernels {

    val rayTracingKernel = """
        // Structure to represent a sphere in GPU memory
        typedef struct {
            float3 center;
            float radius;
            float3 color;  // RGB as float3 (0-1 range)
        } Sphere;

        // LCG random number generator
        float rand_float(uint* seed) {
            *seed = (*seed) * 1664525u + 1013904223u;
            return (float)(*seed) / 4294967296.0f;
        }

        // Generate random float in range [min, max]
        float rand_range(uint* seed, float min, float max) {
            return min + (max - min) * rand_float(seed);
        }

        // Generate random unit vector in unit sphere
        float3 random_in_unit_sphere(uint* seed) {
            for (int i = 0; i < 10; i++) {  // Limit attempts to prevent infinite loop
                float3 p = (float3)(
                    rand_range(seed, -1.0f, 1.0f),
                    rand_range(seed, -1.0f, 1.0f),
                    rand_range(seed, -1.0f, 1.0f)
                );
                if (dot(p, p) < 1.0f) {
                    return normalize(p);
                }
            }
            // Fallback if we can't find a point in sphere
            return (float3)(0.0f, 1.0f, 0.0f);
        }

        // Ray-sphere intersection
        bool hit_sphere(
            float3 ray_origin,
            float3 ray_direction,
            const __global Sphere* sphere,
            float t_min,
            float t_max,
            float* t_out,
            float3* normal_out,
            float3* point_out
        ) {
            float3 oc = ray_origin - sphere->center;
            float a = dot(ray_direction, ray_direction);
            float half_b = dot(oc, ray_direction);
            float c = dot(oc, oc) - sphere->radius * sphere->radius;
            float discriminant = half_b * half_b - a * c;

            if (discriminant < 0.0f) return false;

            float sqrtd = sqrt(discriminant);
            float root = (-half_b - sqrtd) / a;

            if (root < t_min || root > t_max) {
                root = (-half_b + sqrtd) / a;
                if (root < t_min || root > t_max) {
                    return false;
                }
            }

            *t_out = root;
            *point_out = ray_origin + ray_direction * root;
            float3 outward_normal = (*point_out - sphere->center) / sphere->radius;
            bool front_face = dot(ray_direction, outward_normal) < 0.0f;
            *normal_out = front_face ? outward_normal : -outward_normal;

            return true;
        }

        // Find closest sphere hit
        int find_closest_hit(
            float3 ray_origin,
            float3 ray_direction,
            const __global Sphere* spheres,
            int num_spheres,
            float t_min,
            float t_max,
            float* t_out,
            float3* normal_out,
            float3* point_out
        ) {
            int hit_index = -1;
            float closest_t = t_max;

            for (int i = 0; i < num_spheres; i++) {
                float t;
                float3 normal;
                float3 point;

                if (hit_sphere(ray_origin, ray_direction, &spheres[i], t_min, closest_t, &t, &normal, &point)) {
                    closest_t = t;
                    *t_out = t;
                    *normal_out = normal;
                    *point_out = point;
                    hit_index = i;
                }
            }

            return hit_index;
        }

        // Compute ray color iteratively (not recursive, as GPU doesn't handle recursion well)
        float3 ray_color(
            float3 ray_origin,
            float3 ray_direction,
            const __global Sphere* spheres,
            int num_spheres,
            int max_depth,
            uint* seed
        ) {
            float3 color = (float3)(1.0f, 1.0f, 1.0f);
            float3 current_origin = ray_origin;
            float3 current_direction = ray_direction;

            for (int depth = 0; depth < max_depth; depth++) {
                float t;
                float3 normal;
                float3 point;

                int hit_index = find_closest_hit(
                    current_origin,
                    current_direction,
                    spheres,
                    num_spheres,
                    0.001f,
                    1000000.0f,
                    &t,
                    &normal,
                    &point
                );

                if (hit_index >= 0) {
                    // Hit a sphere - compute diffuse scatter
                    float3 scatter_direction = normal + random_in_unit_sphere(seed);

                    // Prevent degenerate scatter direction
                    if (fabs(scatter_direction.x) < 1e-8f &&
                        fabs(scatter_direction.y) < 1e-8f &&
                        fabs(scatter_direction.z) < 1e-8f) {
                        scatter_direction = normal;
                    }

                    // Attenuate by material color
                    color *= spheres[hit_index].color;

                    // Update ray for next iteration
                    current_origin = point;
                    current_direction = scatter_direction;
                } else {
                    // Ray didn't hit anything - return sky color
                    float3 unit_direction = normalize(current_direction);
                    float t_sky = 0.5f * (unit_direction.y + 1.0f);
                    float3 sky_color = (1.0f - t_sky) * (float3)(1.0f, 1.0f, 1.0f) +
                                       t_sky * (float3)(0.5f, 0.7f, 1.0f);
                    color *= sky_color;
                    break;
                }
            }

            return color;
        }

        __kernel void raytrace(
            __global unsigned char* output,
            const int width,
            const int height,
            const int samples_per_pixel,
            const int max_depth,
            const __global Sphere* spheres,
            const int num_spheres,
            const float3 camera_origin,
            const float3 camera_lower_left,
            const float3 camera_horizontal,
            const float3 camera_vertical,
            const uint random_seed
        ) {
            int x = get_global_id(0);
            int y = get_global_id(1);

            if (x >= width || y >= height) return;

            // Initialize random seed for this pixel
            uint seed = random_seed + (y * width + x) * 1013904223u;

            // Accumulate color samples
            float3 pixel_color = (float3)(0.0f, 0.0f, 0.0f);

            for (int s = 0; s < samples_per_pixel; s++) {
                // Add random offset for anti-aliasing
                float u = ((float)x + rand_float(&seed)) / (float)(width - 1);
                float v = ((float)(height - 1 - y) + rand_float(&seed)) / (float)(height - 1);

                // Compute ray direction
                float3 ray_direction = camera_lower_left +
                                      u * camera_horizontal +
                                      v * camera_vertical -
                                      camera_origin;

                // Trace ray and accumulate color
                float3 sample_color = ray_color(
                    camera_origin,
                    ray_direction,
                    spheres,
                    num_spheres,
                    max_depth,
                    &seed
                );

                pixel_color += sample_color;
            }

            // Average samples and apply gamma correction (gamma = 2.0, so sqrt)
            pixel_color /= (float)samples_per_pixel;
            pixel_color = sqrt(pixel_color);  // Gamma correction

            // Clamp to [0, 1] and convert to 8-bit
            pixel_color = clamp(pixel_color, 0.0f, 1.0f);

            int pixel_index = (y * width + x) * 4;
            output[pixel_index]     = 255;  // Alpha
            output[pixel_index + 1] = (unsigned char)(pixel_color.x * 255.0f);  // Red
            output[pixel_index + 2] = (unsigned char)(pixel_color.y * 255.0f);  // Green
            output[pixel_index + 3] = (unsigned char)(pixel_color.z * 255.0f);  // Blue
        }
    """.trimIndent()
}
