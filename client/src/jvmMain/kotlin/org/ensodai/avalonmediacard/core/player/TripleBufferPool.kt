package org.ensodai.avalonmediacard.core.player

import com.sun.jna.Pointer
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.util.concurrent.atomic.AtomicBoolean

class TripleBufferPool(val width: Int, val height: Int) {
    val bytesPerPixel = 4
    val stride = width * bytesPerPixel
    val bufferSize = (stride * height).toLong()

    val bitmaps: Array<Bitmap> = Array(3) {
        val bmp = Bitmap()
        bmp.allocPixels(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE))
        bmp
    }

    val nativePointers: Array<Pointer> = Array(3) { index ->
        val pixmap = bitmaps[index].peekPixels() ?: throw IllegalStateException("Failed to peek pixels for buffer $index")
        Pointer(pixmap.addr)
    }

    // Three buffers:
    // back: currently being rendered into by libmpv
    // middle: latest fully rendered frame from libmpv
    // front: currently being displayed by Compose Canvas
    private var backIndex = 0
    private var middleIndex = 1
    private var frontIndex = 2

    private val newFrameAvailable = AtomicBoolean(false)
    private val swapLock = Any()

    @Volatile
    var isReleased = false
        private set

    fun getBackNativePointer(): Pointer = nativePointers[backIndex]

    fun swapAfterWrite() {
        if (isReleased) return
        bitmaps[backIndex].notifyPixelsChanged()
        synchronized(swapLock) {
            val temp = backIndex
            backIndex = middleIndex
            middleIndex = temp
            newFrameAvailable.set(true)
        }
    }

    fun swapBeforeRead(): Int {
        if (newFrameAvailable.compareAndSet(true, false)) {
            synchronized(swapLock) {
                val temp = frontIndex
                frontIndex = middleIndex
                middleIndex = temp
            }
        }
        return frontIndex
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        synchronized(swapLock) {
            bitmaps.forEach { runCatching { it.close() } }
        }
    }
}
