package io.voodoo.apps.ads.feature.ads.nativ

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.LinearLayoutCompat
import io.voodoo.apps.ads.R

// Or just pull https://github.com/florent37/ShapeOfView (hello florent :D)
class MaxNativeAdViewContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    private lateinit var rectF: RectF
    private val path = Path()
    private var cornerRadius: Float

    init {
        val attributes =
            getContext().obtainStyledAttributes(attrs, R.styleable.MaxNativeAdViewContainer)
        cornerRadius = attributes.getDimensionPixelSize(
            R.styleable.MaxNativeAdViewContainer_cornerRadius,
            0
        ).toFloat()
        attributes.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectF = RectF(0f, 0f, w.toFloat(), h.toFloat())
        resetPath()
    }

    override fun draw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipPath(path)
        super.draw(canvas)
        canvas.restoreToCount(save)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipPath(path)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }

    private fun resetPath() {
        path.reset()
        path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)
        path.close()
    }
}
