package dev.tkuenneth.jetpackinkdemo

import android.os.Bundle
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.storage.StrokeInputBatchSerialization
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SerializationHelper {

    companion object {
        private val stockBrushToEnumValues =
            mapOf(
                StockBrushes.markerV1 to SerializedStockBrush.MARKER_V1,
                StockBrushes.pressurePenV1 to SerializedStockBrush.PRESSURE_PEN_V1,
                StockBrushes.highlighterV1 to SerializedStockBrush.HIGHLIGHTER_V1,
            )

        private val enumToStockBrush =
            stockBrushToEnumValues.entries.associate { (key, value) -> value to key }
    }

    private fun serializeBrush(brush: Brush): SerializedBrush {
        return SerializedBrush(
            size = brush.size,
            color = brush.colorLong,
            epsilon = brush.epsilon,
            stockBrush = stockBrushToEnumValues[brush.family] ?: SerializedStockBrush.MARKER_V1,
        )
    }

    private fun serializeStrokeInputBatch(inputs: StrokeInputBatch): ByteArray {
        val outputStream = ByteArrayOutputStream()
        StrokeInputBatchSerialization.encode(inputs, outputStream)
        return outputStream.toByteArray()
    }

    private fun deserializeBrush(serializedBrush: SerializedBrush): Brush {
        val stockBrushFamily = enumToStockBrush[serializedBrush.stockBrush] ?: StockBrushes.markerV1

        return Brush.createWithColorLong(
            family = stockBrushFamily,
            colorLong = serializedBrush.color,
            size = serializedBrush.size,
            epsilon = serializedBrush.epsilon,
        )
    }

    private fun deserializeStrokeInputBatch(serializedBatch: ByteArray): StrokeInputBatch =
        StrokeInputBatchSerialization.decode(ByteArrayInputStream(serializedBatch))

    fun serializeStrokes(strokes: List<Stroke>): List<Bundle> {
        return mutableListOf<Bundle>().apply {
            strokes.forEach { stroke ->
                val brush = stroke.brush
                val serializedBrush = serializeBrush(stroke.brush)
                val serializedInputs = serializeStrokeInputBatch(stroke.inputs)
                add(Bundle().apply {
                    putFloat("brushSize", brush.size)
                    putLong("brushColor", brush.colorLong)
                    putFloat("brushEpsilon", brush.epsilon)
                    putInt("stockBrush", serializedBrush.stockBrush.ordinal)
                    putByteArray("strokeInputs", serializedInputs)
                })
            }
        }
    }

    fun deserializeStrokes(savedStrokes: ArrayList<Bundle>): List<Stroke> {
        return mutableListOf<Stroke>().apply {
            savedStrokes.forEach { entity ->
                val serializedBrush =
                    SerializedBrush(
                        size = entity.getFloat("brushSize"),
                        color = entity.getLong("brushColor"),
                        epsilon = entity.getFloat("brushEpsilon"),
                        stockBrush = SerializedStockBrush.entries[entity.getInt("stockBrush")]
                    )

                entity.getByteArray("strokeInputs")?.let { serializedInputs ->
                    val brush = deserializeBrush(serializedBrush)
                    val inputs = deserializeStrokeInputBatch(serializedInputs)
                    add(Stroke(brush = brush, inputs = inputs))
                }
            }
        }
    }
}

data class SerializedBrush(
    val size: Float,
    val color: Long,
    val epsilon: Float,
    val stockBrush: SerializedStockBrush
)

enum class SerializedStockBrush {
    MARKER_V1,
    PRESSURE_PEN_V1,
    HIGHLIGHTER_V1
}
