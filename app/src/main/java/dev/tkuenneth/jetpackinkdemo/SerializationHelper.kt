package dev.tkuenneth.jetpackinkdemo

import android.os.Bundle
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.storage.BrushFamilySerialization
import androidx.ink.storage.StrokeInputBatchSerialization
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalInkCustomBrushApi::class)
class SerializationHelper {

    private fun serializeStrokeInputBatch(inputs: StrokeInputBatch): ByteArray {
        val outputStream = ByteArrayOutputStream()
        StrokeInputBatchSerialization.encode(inputs, outputStream)
        return outputStream.toByteArray()
    }

    private fun deserializeStrokeInputBatch(serializedBatch: ByteArray): StrokeInputBatch =
        StrokeInputBatchSerialization.decode(ByteArrayInputStream(serializedBatch))

    fun serializeStrokes(strokes: List<Stroke>): List<Bundle> {
        return mutableListOf<Bundle>().apply {
            strokes.forEach { stroke ->
                val brush = stroke.brush
                val outputStream = ByteArrayOutputStream()
                BrushFamilySerialization.encode(brush.family, outputStream)
                val serializedBrushFamily = outputStream.toByteArray()

                val serializedInputs = serializeStrokeInputBatch(stroke.inputs)
                add(Bundle().apply {
                    putFloat("brushSize", brush.size)
                    putLong("brushColor", brush.colorLong)
                    putFloat("brushEpsilon", brush.epsilon)
                    putByteArray("brushFamily", serializedBrushFamily)
                    putByteArray("strokeInputs", serializedInputs)
                })
            }
        }
    }

    fun deserializeStrokes(savedStrokes: ArrayList<Bundle>): List<Stroke> {
        return mutableListOf<Stroke>().apply {
            savedStrokes.forEach { entity ->
                val brushFamily = entity.getByteArray("brushFamily")?.let {
                    BrushFamilySerialization.decode(ByteArrayInputStream(it))
                }

                entity.getByteArray("strokeInputs")?.let { serializedInputs ->
                    if (brushFamily != null) {
                        val brush = Brush.createWithColorLong(
                            family = brushFamily,
                            colorLong = entity.getLong("brushColor"),
                            size = entity.getFloat("brushSize"),
                            epsilon = entity.getFloat("brushEpsilon"),
                        )
                        val inputs = deserializeStrokeInputBatch(serializedInputs)
                        add(Stroke(brush = brush, inputs = inputs))
                    }
                }
            }
        }
    }
}
