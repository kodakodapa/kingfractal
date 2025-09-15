package org.example.com.github.kodakodapa.kingfractal

import org.jocl.*
import java.nio.FloatBuffer
import kotlin.math.*

class OpenCLRenderer {

    private var context: cl_context? = null
    private var commandQueue: cl_command_queue? = null
    private var program: cl_program? = null
    private var kernel: cl_kernel? = null
    private var device: cl_device_id? = null
    private var kernelSource: String? = null

    var isInitialized = false


}