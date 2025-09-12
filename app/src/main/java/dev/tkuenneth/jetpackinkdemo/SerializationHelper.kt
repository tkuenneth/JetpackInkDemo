package dev.tkuenneth.jetpackinkdemo

import android.os.Bundle
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch


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

    private fun serializeStrokeInputBatch(inputs: StrokeInputBatch): SerializedStrokeInputBatch {
        val serializedInputs = mutableListOf<SerializedStrokeInput>()
        val scratchInput = StrokeInput()

        for (i in 0 until inputs.size) {
            inputs.populate(i, scratchInput)
            serializedInputs.add(
                SerializedStrokeInput(
                    x = scratchInput.x,
                    y = scratchInput.y,
                    timeMillis = scratchInput.elapsedTimeMillis.toFloat(),
                    pressure = scratchInput.pressure,
                    tiltRadians = scratchInput.tiltRadians,
                    orientationRadians = scratchInput.orientationRadians,
                    strokeUnitLengthCm = scratchInput.strokeUnitLengthCm,
                )
            )
        }

        val toolType =
            when (inputs.getToolType()) {
                InputToolType.STYLUS -> SerializedToolType.STYLUS
                InputToolType.TOUCH -> SerializedToolType.TOUCH
                InputToolType.MOUSE -> SerializedToolType.MOUSE
                else -> SerializedToolType.UNKNOWN
            }

        return SerializedStrokeInputBatch(
            toolType = toolType,
            strokeUnitLengthCm = inputs.getStrokeUnitLengthCm(),
            inputs = serializedInputs,
        )
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

    private fun deserializeStrokeInputBatch(
        serializedBatch: SerializedStrokeInputBatch
    ): StrokeInputBatch {
        val toolType =
            when (serializedBatch.toolType) {
                SerializedToolType.STYLUS -> InputToolType.STYLUS
                SerializedToolType.TOUCH -> InputToolType.TOUCH
                SerializedToolType.MOUSE -> InputToolType.MOUSE
                else -> InputToolType.UNKNOWN
            }

        val batch = MutableStrokeInputBatch()

        serializedBatch.inputs.forEach { input ->
            batch.add(
                type = toolType,
                x = input.x,
                y = input.y,
                elapsedTimeMillis = input.timeMillis.toLong(),
                pressure = input.pressure,
                tiltRadians = input.tiltRadians,
                orientationRadians = input.orientationRadians,
            )
        }

        return batch
    }

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
                    putSerializable("strokeInputs", serializedInputs)
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

                entity.getSerializable(
                    "strokeInputs",
                    SerializedStrokeInputBatch::class.java
                )?.let { serializedInputs ->

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

data class SerializedStrokeInputBatch(
    val toolType: SerializedToolType,
    val strokeUnitLengthCm: Float,
    val inputs: List<SerializedStrokeInput>
) : java.io.Serializable

data class SerializedStrokeInput(
    val x: Float,
    val y: Float,
    val timeMillis: Float,
    val pressure: Float,
    val tiltRadians: Float,
    val orientationRadians: Float,
    val strokeUnitLengthCm: Float
)

enum class SerializedToolType {
    STYLUS,
    TOUCH,
    MOUSE,
    UNKNOWN
}
