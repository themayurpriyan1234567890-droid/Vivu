package com.example.vivuvizualizer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max

class SpectrumView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    @Volatile private var levels = FloatArray(48)
    private var fftMode = false

    fun setData(data: ByteArray, isFft: Boolean = false) {
        fftMode = isFft
        val next = FloatArray(48)
        if (isFft && data.isNotEmpty()) {
            val bins = max(1, (data.size - 2) / 2)
            for (i in next.indices) {
                val start = 1 + (i * bins / next.size) * 2
                val end = max(start + 1, 1 + ((i + 1) * bins / next.size) * 2)
                var total = 0f
                var count = 0
                var j = start
                while (j < end && j + 1 < data.size) {
                    val real = data[j].toInt()
                    val imaginary = data[j + 1].toInt()
                    total += (abs(real) + abs(imaginary)).toFloat()
                    count++
                    j += 2
                }
                next[i] = ((if (count > 0) total / count else 0f) / 48f).coerceIn(0f, 1f)
            }
        } else {
            for (i in next.indices) {
                val index = i * data.size / next.size
                next[i] = if (data.isNotEmpty()) abs(data[index].toInt() - 128) / 128f else 0f
            }
        }
        levels = next
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = levels.size
        val gap = width * 0.006f
        val barWidth = (width - gap * (count + 1)) / count
        val maxHeight = height * 0.34f
        val baseline = height * 0.82f
        for (i in 0 until count) {
            val value = levels[i].coerceIn(0f, 1f)
            val eased = max(0.035f, value)
            val barHeight = maxHeight * eased
            val left = gap + i * (barWidth + gap)
            val top = baseline - barHeight
            paint.color = android.graphics.Color.rgb(
                (80 + 120 * i / count),
                (80 + 130 * value).toInt().coerceIn(0, 255),
                255
            )
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(
                RectF(left, top, left + barWidth, baseline),
                barWidth / 2, barWidth / 2, paint
            )
        }
    }
}
