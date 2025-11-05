package com.github.kodakodapa.kingfractal.threedimensional

import com.github.kodakodapa.kingfractal.colors.ARGBColor
import com.github.kodakodapa.kingfractal.threedimensional.kernels.RayTracingKernels
import org.jocl.*
import kotlin.math.PI
import kotlin.math.tan

class GPURayTracer {

    private var context: cl_context? = null
    private var commandQueue: cl_command_queue? = null
    private var program: cl_program? = null
    private var kernel: cl_kernel? = null
    private var device: cl_device_id? = null
    var isInitialized = false

    fun initialize() {
        // Enable exceptions
        CL.setExceptionsEnabled(true)

        // Get platform
        val numPlatformsArray = IntArray(1)
        CL.clGetPlatformIDs(0, null, numPlatformsArray)
        val numPlatforms = numPlatformsArray[0]

        val platforms = arrayOfNulls<cl_platform_id>(numPlatforms)
        CL.clGetPlatformIDs(platforms.size, platforms, null)
        val platform = platforms[0]

        // Get devices
        val numDevicesArray = IntArray(1)
        CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_GPU, 0, null, numDevicesArray)
        val numDevices = numDevicesArray[0]

        if (numDevices == 0) {
            // Fall back to CPU
            CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_CPU, 0, null, numDevicesArray)
        }

        val devices = arrayOfNulls<cl_device_id>(numDevicesArray[0])
        CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, devices.size, devices, null)
        device = devices[0]

        // Create context
        val contextProperties = cl_context_properties()
        contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM.toLong(), platform)
        context = CL.clCreateContext(contextProperties, 1, arrayOf(device), null, null, null)

        // Create command queue
        commandQueue = CL.clCreateCommandQueueWithProperties(context, device, null, null)

        // Create program
        program = CL.clCreateProgramWithSource(
            context, 1,
            arrayOf(RayTracingKernels.rayTracingKernel), null, null
        )

        // Build program
        val buildOptions = "-cl-fast-relaxed-math -cl-mad-enable"
        val buildResult = CL.clBuildProgram(program, 0, null, buildOptions, null, null)

        if (buildResult != CL.CL_SUCCESS) {
            // Get build log
            val logSize = LongArray(1)
            CL.clGetProgramBuildInfo(program, device, CL.CL_PROGRAM_BUILD_LOG, 0, null, logSize)

            val logBuffer = ByteArray(logSize[0].toInt())
            CL.clGetProgramBuildInfo(
                program,
                device,
                CL.CL_PROGRAM_BUILD_LOG,
                logBuffer.size.toLong(),
                Pointer.to(logBuffer),
                null
            )

            val log = String(logBuffer)
            throw RuntimeException("OpenCL build failed: $log")
        }

        // Create kernel
        kernel = CL.clCreateKernel(program, "raytrace", null)

        isInitialized = true
        println("GPU Ray Tracer initialized successfully on device: ${getDeviceName()}")
    }

    fun render(
        camera: Camera,
        world: HittableList,
        imageWidth: Int,
        imageHeight: Int,
        maxDepth: Int = 10,
        samplesPerPixel: Int = 10
    ): Array<Array<ARGBColor>> {
        require(isInitialized) { "Must be initialized" }
        requireNotNull(context) { "OpenCL context not initialized" }
        requireNotNull(commandQueue) { "Command queue not initialized" }
        requireNotNull(kernel) { "Kernel not initialized" }

        // Convert world to sphere array for GPU
        val spheres = world.objects.filterIsInstance<Sphere>()
        val numSpheres = spheres.size

        // Pack sphere data: each sphere is 7 floats (center.xyz, radius, color.rgb)
        val sphereData = FloatArray(numSpheres * 7)
        spheres.forEachIndexed { index, sphere ->
            val offset = index * 7
            sphereData[offset] = sphere.center.x.toFloat()
            sphereData[offset + 1] = sphere.center.y.toFloat()
            sphereData[offset + 2] = sphere.center.z.toFloat()
            sphereData[offset + 3] = sphere.radius.toFloat()
            sphereData[offset + 4] = sphere.material.color.red / 255.0f
            sphereData[offset + 5] = sphere.material.color.green / 255.0f
            sphereData[offset + 6] = sphere.material.color.blue / 255.0f
        }

        // Calculate camera vectors
        val aspectRatio = imageWidth.toDouble() / imageHeight.toDouble()
        val halfHeight = tan(camera.fov * PI / 180.0 / 2.0)
        val halfWidth = aspectRatio * halfHeight

        val w = (camera.position - camera.target).normalize()
        val u = camera.up.cross(w).normalize()
        val v = w.cross(u)

        val horizontal = u * (halfWidth * 2.0)
        val vertical = v * (halfHeight * 2.0)
        val lowerLeftCorner = camera.position - u * halfWidth - v * halfHeight - w

        // Create OpenCL memory objects
        val outputSize = (imageWidth * imageHeight * 4).toLong()
        val outputMem = CL.clCreateBuffer(context, CL.CL_MEM_WRITE_ONLY, outputSize, null, null)
        val spheresMem = CL.clCreateBuffer(
            context,
            CL.CL_MEM_READ_ONLY or CL.CL_MEM_COPY_HOST_PTR,
            (sphereData.size * Sizeof.cl_float).toLong(),
            Pointer.to(sphereData),
            null
        )

        try {
            // Set kernel arguments
            var argIndex = 0
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_mem.toLong(), Pointer.to(outputMem))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(imageWidth)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(imageHeight)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(samplesPerPixel)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(maxDepth)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_mem.toLong(), Pointer.to(spheresMem))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(numSpheres)))

            // Camera origin
            CL.clSetKernelArg(
                kernel, argIndex++, Sizeof.cl_float3.toLong(),
                Pointer.to(floatArrayOf(
                    camera.position.x.toFloat(),
                    camera.position.y.toFloat(),
                    camera.position.z.toFloat()
                ))
            )

            // Camera lower left corner
            CL.clSetKernelArg(
                kernel, argIndex++, Sizeof.cl_float3.toLong(),
                Pointer.to(floatArrayOf(
                    lowerLeftCorner.x.toFloat(),
                    lowerLeftCorner.y.toFloat(),
                    lowerLeftCorner.z.toFloat()
                ))
            )

            // Camera horizontal vector
            CL.clSetKernelArg(
                kernel, argIndex++, Sizeof.cl_float3.toLong(),
                Pointer.to(floatArrayOf(
                    horizontal.x.toFloat(),
                    horizontal.y.toFloat(),
                    horizontal.z.toFloat()
                ))
            )

            // Camera vertical vector
            CL.clSetKernelArg(
                kernel, argIndex++, Sizeof.cl_float3.toLong(),
                Pointer.to(floatArrayOf(
                    vertical.x.toFloat(),
                    vertical.y.toFloat(),
                    vertical.z.toFloat()
                ))
            )

            // Random seed
            CL.clSetKernelArg(
                kernel, argIndex, Sizeof.cl_uint.toLong(),
                Pointer.to(intArrayOf(System.currentTimeMillis().toInt()))
            )

            // Execute kernel
            val globalWorkSize = longArrayOf(imageWidth.toLong(), imageHeight.toLong())
            CL.clEnqueueNDRangeKernel(commandQueue, kernel, 2, null, globalWorkSize, null, 0, null, null)

            // Read result
            val outputArray = ByteArray(outputSize.toInt())
            CL.clEnqueueReadBuffer(
                commandQueue, outputMem, true, 0, outputSize,
                Pointer.to(outputArray), 0, null, null
            )

            // Wait for completion
            CL.clFinish(commandQueue)

            // Convert to ARGBColor array
            val image = Array(imageHeight) { Array(imageWidth) { ARGBColor.BLACK } }
            for (y in 0 until imageHeight) {
                for (x in 0 until imageWidth) {
                    val pixelIndex = (y * imageWidth + x) * 4
                    val alpha = outputArray[pixelIndex].toInt() and 0xFF
                    val red = outputArray[pixelIndex + 1].toInt() and 0xFF
                    val green = outputArray[pixelIndex + 2].toInt() and 0xFF
                    val blue = outputArray[pixelIndex + 3].toInt() and 0xFF
                    image[y][x] = ARGBColor(alpha, red, green, blue)
                }
            }

            return image

        } finally {
            // Clean up memory objects
            CL.clReleaseMemObject(outputMem)
            CL.clReleaseMemObject(spheresMem)
        }
    }

    private fun getDeviceName(): String {
        val size = LongArray(1)
        CL.clGetDeviceInfo(device, CL.CL_DEVICE_NAME, 0, null, size)

        val buffer = ByteArray(size[0].toInt())
        CL.clGetDeviceInfo(device, CL.CL_DEVICE_NAME, buffer.size.toLong(), Pointer.to(buffer), null)

        return String(buffer).trim { it <= ' ' }
    }

    fun cleanup() {
        try {
            kernel?.let { CL.clReleaseKernel(it) }
            program?.let { CL.clReleaseProgram(it) }
            commandQueue?.let { CL.clReleaseCommandQueue(it) }
            context?.let { CL.clReleaseContext(it) }
        } catch (e: Exception) {
            println("Error during GPU Ray Tracer cleanup: ${e.message}")
        }

        kernel = null
        program = null
        commandQueue = null
        context = null
        device = null
        isInitialized = false
        println("GPU Ray Tracer resources cleaned up")
    }
}
