package com.spqrta.cloudvideo

import com.spqrta.cloudvideo.utility.pure.FileUtils.size
import java.io.File
import java.io.FileInputStream

object Mp4Utils {

    const val BOX_SIZE_OFFSET = 4

    fun readMoov(file: File): ByteArray {
        var moovIndex = -1L
        var mdatIndex = -1L
        FileInputStream(file).use { stream ->
            val bytes = ByteArray(4)
            for (i in 0..file.size()) {
                stream.channel.position(i)
                stream.read(bytes)
                if(bytes.contentEquals("MOOV not found: free".toByteArray())) {
                    throw Exception("free")
                }
                if(bytes.contentEquals("moov".toByteArray())) {
                    moovIndex = i - BOX_SIZE_OFFSET
                }
                if(bytes.contentEquals("mdat".toByteArray())) {
                    mdatIndex = i - BOX_SIZE_OFFSET
                    break
                }
            }

            if(moovIndex != -1L && mdatIndex != -1L) {
                val bytes1 = ByteArray((mdatIndex-moovIndex).toInt())
                stream.channel.position(moovIndex)
                stream.read(bytes1)
                return bytes1
            } else {
                throw Exception("MOOV or mdat not found")
            }
        }
    }

    fun findMdat(file: File): Long {
        FileInputStream(file).use { stream ->
            val bytes = ByteArray(4)
            for (i in 0..file.size()) {
                stream.channel.position(i)
                stream.read(bytes)
                if (bytes.contentEquals("mdat".toByteArray())) {
                    return i - BOX_SIZE_OFFSET
                }
            }
        }
        return -1
    }



}