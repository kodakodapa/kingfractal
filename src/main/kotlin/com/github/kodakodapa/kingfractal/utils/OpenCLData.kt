package com.github.kodakodapa.kingfractal.utils

// Interface for OpenCL data types
interface OpenCLData {
    fun toByteArray(): ByteArray
    fun getBufferSize(): Long
}
