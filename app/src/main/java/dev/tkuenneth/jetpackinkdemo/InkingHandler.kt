package dev.tkuenneth.jetpackinkdemo

import android.view.MotionEvent
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor

class InkingHandler(
    private val view: InProgressStrokesView,
    private val addStrokesLambda: (Collection<Stroke>) -> Unit
) : InProgressStrokesFinishedListener {

    private var currentPointerId: Int? = null
    private var currentStrokeId: InProgressStrokeId? = null
    private val predictor = MotionEventPredictor.newInstance(view)

    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        addStrokesLambda(strokes.values)
        view.removeFinishedStrokes(strokes.keys)
    }

    private fun invokeIfConditionsMet(
        event: MotionEvent,
        action: (strokeId: InProgressStrokeId) -> Unit
    ) {
        val pointerIdFromEvent = event.getPointerId(event.actionIndex)
        currentStrokeId?.let {
            if (pointerIdFromEvent == currentPointerId) {
                action(it)
            }
        }
    }

    fun handleMotionEvent(event: MotionEvent, latestBrush: Brush): Boolean {
        predictor.record(event)
        val predictedMotionEvent = predictor.predict()
        try {
            return when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.requestUnbufferedDispatch(event)
                    event.getPointerId(event.actionIndex).let { pointerId ->
                        currentPointerId = pointerId
                        currentStrokeId = view.startStroke(
                            event = event,
                            pointerId = pointerId,
                            brush = latestBrush
                        )
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    currentPointerId?.let { pointerId ->
                        currentStrokeId?.let { strokeId ->
                            for (pointerIndex in 0 until event.pointerCount) {
                                if (event.getPointerId(pointerIndex) != pointerId) continue
                                view.addToStroke(
                                    event, pointerId, strokeId, predictedMotionEvent
                                )
                            }
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    invokeIfConditionsMet(event) { strokeId ->
                        view.finishStroke(
                            event,
                            event.getPointerId(event.actionIndex),
                            strokeId
                        )
                    }
                    view.performClick()
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    invokeIfConditionsMet(event) { strokeId ->
                        view.cancelStroke(strokeId, event)
                    }
                    true
                }

                else -> false
            }
        } finally {
            predictedMotionEvent?.recycle()
        }
    }
}
