package com.example.tester2.data.recorder

import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()
    fun maxAmplitude(): Int
}
