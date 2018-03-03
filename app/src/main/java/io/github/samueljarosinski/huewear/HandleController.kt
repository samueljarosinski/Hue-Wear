package io.github.samueljarosinski.huewear

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator

private const val SCALE_RATIO: Float = 2.0f
private const val SCALE_ANIMATION_DURATION: Long = 200

typealias Position = Pair<Float, Float>
typealias OnMoveListener = (Position) -> Unit
typealias OnScrollListener = (Int) -> Unit

class HandleController(
    private val handleView: View,
    private val onMove: OnMoveListener,
    private val onScroll: OnScrollListener
) : View.OnTouchListener {

    private var screenHeight: Int = 0

    init {
        with(handleView.parent as View) {
            setOnTouchListener(this@HandleController)
            requestFocus()
            screenHeight = measuredHeight
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean = when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            onMove(event.getPosition())

            handleView.animate()
                .setPosition(event.getViewPosition(handleView))
                .scaleX(SCALE_RATIO)
                .scaleY(SCALE_RATIO)
                .setDuration(SCALE_ANIMATION_DURATION)
                .start()

            true
        }

        MotionEvent.ACTION_MOVE -> {
            // onScroll(255 - (event.getY(0) / screenHeight * 255).toInt())
            onMove(event.getPosition())

            handleView.animate()
                .setPosition(event.getViewPosition(handleView))
                .setDuration(0)
                .start()

            true
        }

        MotionEvent.ACTION_UP   -> {
            handleView.animate()
                .setPosition(event.getViewPosition(handleView))
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(SCALE_ANIMATION_DURATION)
                .start()

            true
        }

        else                    -> false
    }
}

private fun MotionEvent.getPosition(): Position =
    Position(rawX, rawY)

private fun MotionEvent.getViewPosition(view: View): Position =
    Position(rawX - (view.measuredWidth / 2), rawY - (view.measuredHeight / 2))

private fun ViewPropertyAnimator.setPosition(position: Position): ViewPropertyAnimator =
    x(position.first).y(position.second)
