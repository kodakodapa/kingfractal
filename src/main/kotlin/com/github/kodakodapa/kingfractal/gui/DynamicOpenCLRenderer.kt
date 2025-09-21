package com.github.kodakodapa.kingfractal.gui

import com.github.kodakodapa.kingfractal.outputs.ImageData
import com.github.kodakodapa.kingfractal.utils.FractalParams
import com.github.kodakodapa.kingfractal.utils.JuliaParams
import com.github.kodakodapa.kingfractal.utils.MandelbrotParams
import com.github.kodakodapa.kingfractal.utils.BuddhabrotParams
import com.github.kodakodapa.kingfractal.outputs.ARGB_CHANNELS
import org.jocl.*

/**
 * OpenCL renderer that can handle dynamic image dimensions
 * without being constrained by the dataFactory at initialization time
 */
class DynamicOpenCLRenderer(
    private val kernelSource: String,
    private val kernelName: String
) {

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
        program = CL.clCreateProgramWithSource(context, 1, arrayOf(kernelSource), null, null)

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
        kernel = CL.clCreateKernel(program, kernelName, null)

        isInitialized = true
        println("OpenCL initialized successfully on device: ${getDeviceName()}")
    }

    fun renderFractal(width: Int, height: Int, params: FractalParams): ImageData {
        require(isInitialized) { "Must be initialized" }
        requireNotNull(context) { "OpenCL context not initialized" }
        requireNotNull(commandQueue) { "Command queue not initialized" }
        requireNotNull(kernel) { "Kernel not initialized" }

        return when (params) {
            is BuddhabrotParams -> renderBuddhabrot(width, height, params)
            else -> renderStandardFractal(width, height, params)
        }
    }

    private fun renderStandardFractal(width: Int, height: Int, params: FractalParams): ImageData {
        val outputSize = width * height * ARGB_CHANNELS.toLong()

        // Create OpenCL memory objects
        val outputMem = CL.clCreateBuffer(context, CL.CL_MEM_WRITE_ONLY, outputSize, null, null)

        try {
            // Set kernel arguments
            var argIndex = 0
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_mem.toLong(), Pointer.to(outputMem))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(width)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(height)))

            // Set fractal-specific parameters
            when (params) {
                is MandelbrotParams -> {
                    CL.clSetKernelArg(
                        kernel,
                        argIndex++,
                        Sizeof.cl_float.toLong(),
                        Pointer.to(floatArrayOf(params.zoom))
                    )
                    CL.clSetKernelArg(
                        kernel,
                        argIndex++,
                        Sizeof.cl_float.toLong(),
                        Pointer.to(floatArrayOf(params.centerX))
                    )
                    CL.clSetKernelArg(
                        kernel,
                        argIndex++,
                        Sizeof.cl_float.toLong(),
                        Pointer.to(floatArrayOf(params.centerY))
                    )
                    CL.clSetKernelArg(
                        kernel,
                        argIndex,
                        Sizeof.cl_int.toLong(),
                        Pointer.to(intArrayOf(params.maxIterations))
                    )
                }

                is JuliaParams -> {
                    CL.clSetKernelArg(
                        kernel,
                        argIndex++,
                        Sizeof.cl_float.toLong(),
                        Pointer.to(floatArrayOf(params.zoom))
                    )
                    CL.clSetKernelArg(
                        kernel,
                        argIndex++,
                        Sizeof.cl_float.toLong(),
                        Pointer.to(floatArrayOf(params.centerX))
                    )
                    CL.clSetKernelArg(
                        kernel,
                        argIndex++,
                        Sizeof.cl_float.toLong(),
                        Pointer.to(floatArrayOf(params.centerY))
                    )
                    CL.clSetKernelArg(
                        kernel,
                        argIndex++,
                        Sizeof.cl_float.toLong(),
                        Pointer.to(floatArrayOf(params.juliaReal))
                    )
                    CL.clSetKernelArg(
                        kernel,
                        argIndex++,
                        Sizeof.cl_float.toLong(),
                        Pointer.to(floatArrayOf(params.juliaImag))
                    )
                    CL.clSetKernelArg(
                        kernel,
                        argIndex,
                        Sizeof.cl_int.toLong(),
                        Pointer.to(intArrayOf(params.maxIterations))
                    )
                }

                is BuddhabrotParams -> {
                    // This should not happen as Buddhabrot is handled separately
                    throw IllegalArgumentException("BuddhabrotParams should be handled by renderBuddhabrot method")
                }
            }

            // Execute kernel
            val globalWorkSize = longArrayOf(width.toLong(), height.toLong())
            CL.clEnqueueNDRangeKernel(commandQueue, kernel, 2, null, globalWorkSize, null, 0, null, null)

            // Read result
            val outputArray = ByteArray(outputSize.toInt())
            CL.clEnqueueReadBuffer(
                commandQueue, outputMem, true, 0, outputSize,
                Pointer.to(outputArray), 0, null, null
            )

            // Wait for completion
            CL.clFinish(commandQueue)

            // Create ImageData with the correct dimensions
            return ImageData.fromByteArray(width, height, outputArray)

        } finally {
            // Clean up memory objects
            CL.clReleaseMemObject(outputMem)
        }
    }

    private fun renderBuddhabrot(width: Int, height: Int, params: BuddhabrotParams): ImageData {
        val outputSize = width * height * ARGB_CHANNELS.toLong()
        val workGroupSize = 2048L // Number of work items
        val randomStatesSize = workGroupSize * Sizeof.cl_uint.toLong()

        // Initialize output buffer to zero
        val outputMem = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE, outputSize, null, null)

        // Create random states buffer
        val randomStatesMem = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE, randomStatesSize, null, null)

        try {
            // Initialize output buffer to zero
            val zeroBuffer = ByteArray(outputSize.toInt())
            CL.clEnqueueWriteBuffer(commandQueue, outputMem, true, 0, outputSize, Pointer.to(zeroBuffer), 0, null, null)

            // Initialize random states with different seeds
            val randomStates = IntArray(workGroupSize.toInt()) { it * 12345 + System.currentTimeMillis().toInt() }
            CL.clEnqueueWriteBuffer(
                commandQueue, randomStatesMem, true, 0, randomStatesSize,
                Pointer.to(randomStates), 0, null, null
            )

            // Set kernel arguments
            var argIndex = 0
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_mem.toLong(), Pointer.to(outputMem))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(width)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(height)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.zoom)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.centerX)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.centerY)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(params.maxIterations)))
            CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(params.sampleCount)))
            CL.clSetKernelArg(kernel, argIndex, Sizeof.cl_mem.toLong(), Pointer.to(randomStatesMem))

            // Execute kernel - 1D work group for Buddhabrot
            val globalWorkSize = longArrayOf(workGroupSize)
            CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, null, 0, null, null)

            // Read result
            val outputArray = ByteArray(outputSize.toInt())
            CL.clEnqueueReadBuffer(
                commandQueue, outputMem, true, 0, outputSize,
                Pointer.to(outputArray), 0, null, null
            )

            // Wait for completion
            CL.clFinish(commandQueue)

            // Normalize the histogram data to 0-255 range
            val normalizedArray = normalizeBuddhabrotOutput(outputArray, width, height)

            // Create ImageData with the correct dimensions
            return ImageData.fromByteArray(width, height, normalizedArray)

        } finally {
            // Clean up memory objects
            CL.clReleaseMemObject(outputMem)
            CL.clReleaseMemObject(randomStatesMem)
        }
    }

    private fun normalizeBuddhabrotOutput(rawOutput: ByteArray, width: Int, height: Int): ByteArray {
        // Find the maximum hit count
        var maxHits = 0
        for (i in rawOutput.indices step ARGB_CHANNELS) {
            val hits = rawOutput[i].toInt() and 0xFF
            if (hits > maxHits) maxHits = hits
        }

        // Normalize to 0-255 range with logarithmic scaling
        val normalizedOutput = ByteArray(rawOutput.size)
        for (i in rawOutput.indices step ARGB_CHANNELS) {
            val hits = rawOutput[i].toInt() and 0xFF
            val normalizedValue = if (maxHits > 0) {
                // Logarithmic scaling for better visualization
                val logValue = if (hits > 0) kotlin.math.ln(1.0 + hits.toDouble()) else 0.0
                val maxLogValue = kotlin.math.ln(1.0 + maxHits.toDouble())
                ((logValue / maxLogValue) * 255.0).toInt().coerceIn(0, 255)
            } else {
                0
            }

            normalizedOutput[i] = normalizedValue.toByte()     // A
            normalizedOutput[i + 1] = normalizedValue.toByte() // R
            normalizedOutput[i + 2] = normalizedValue.toByte() // G
            normalizedOutput[i + 3] = normalizedValue.toByte() // B
        }

        return normalizedOutput
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
            println("Error during cleanup: ${e.message}")
        }

        kernel = null
        program = null
        commandQueue = null
        context = null
        device = null
        isInitialized = false
        println("OpenCL resources cleaned up")
    }
}