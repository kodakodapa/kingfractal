package com.github.kodakodapa.kingfractal.gui

import com.github.kodakodapa.kingfractal.outputs.ImageData
import com.github.kodakodapa.kingfractal.utils.FractalParams
import com.github.kodakodapa.kingfractal.utils.JuliaParams
import com.github.kodakodapa.kingfractal.utils.MandelbrotParams
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

        val outputSize = width * height * ARGB_CHANNELS.toLong() // RGB


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
                    CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.zoom)))
                    CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.centerX)))
                    CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.centerY)))
                    CL.clSetKernelArg(kernel, argIndex, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(params.maxIterations)))
                }
                is JuliaParams -> {
                    CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.zoom)))
                    CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.centerX)))
                    CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.centerY)))
                    CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.juliaReal)))
                    CL.clSetKernelArg(kernel, argIndex++, Sizeof.cl_float.toLong(), Pointer.to(floatArrayOf(params.juliaImag)))
                    CL.clSetKernelArg(kernel, argIndex, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(params.maxIterations)))
                }
            }

            // Execute kernel
            val globalWorkSize = longArrayOf(width.toLong(), height.toLong())
            CL.clEnqueueNDRangeKernel(commandQueue, kernel, 2, null, globalWorkSize, null, 0, null, null)

            // Read result
            val outputArray = ByteArray(outputSize.toInt())
            CL.clEnqueueReadBuffer(commandQueue, outputMem, true, 0, outputSize,
                Pointer.to(outputArray), 0, null, null)

            // Wait for completion
            CL.clFinish(commandQueue)

            // Create ImageData with the correct dimensions
            return ImageData.fromByteArray(width, height, outputArray)

        } finally {
            // Clean up memory objects
            CL.clReleaseMemObject(outputMem)
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