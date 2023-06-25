package com.skit.switchs

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.addListener
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.roundToInt

/**
 * PS: 只是为了实现效果，没有做任何优化或属性等内容
 */
class SwitchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val DURATION = 500L
    }

    // 太阳周围环的画笔
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = 255 / 3
        color = Color.parseColor("#FFFFFF")
    }

    // 太阳和月亮的画笔
    private val sunAndMoonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setShadowLayer(20f, 20f, 10f, Color.parseColor("#808080"))
    }

    // 月坑的画笔
    private val lunarPitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF8897AC")
    }

    // 月亮的画笔
    private val startsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    // 裁剪路径
    private var clipPath: Path = Path()

    // 星星位图组
    private lateinit var bitmap1: Bitmap
    private lateinit var bitmap2: Bitmap
    private lateinit var bitmap3: Bitmap

    var changeCallback: (Boolean) -> Unit = {}

    init {
        setOnClickListener {
            AnimatorSet().apply {
                addListener(onEnd = {
                    changeCallback(isSelected)
                })
                if (!isSelected) {
                    val selected = false
                    playTogether(
                        cloudsOffsetAnim(selected),
                        sunOffsetAnim(selected),
                        backgroundColorAnim(selected),
                        startsOffsetAnim(selected),
                    )
                } else {
                    val selected = true
                    playTogether(
                        cloudsOffsetAnim(selected),
                        sunOffsetAnim(selected),
                        backgroundColorAnim(selected),
                        startsOffsetAnim(selected),
                    )
                }
            }.start()
            isSelected = !isSelected
        }
    }

    private fun sunOffsetAnim(selected: Boolean): Animator {
        val target = if (selected) {
            0f
        } else {
            measuredWidth.toFloat() - getSunAndMoonCircleWidth() - (getSpacing() * 2)
        }
        return ObjectAnimator.ofFloat(this, "sunOffset", sunOffset, target)
            .apply {
                interpolator = FastOutSlowInInterpolator()
                duration = DURATION
            }
    }

    private fun startsOffsetAnim(selected: Boolean): Animator {
        val target = if (selected) measuredHeight.toFloat() else 0f
        return ObjectAnimator.ofFloat(
            this,
            "startsOffset",
            startsOffset,
            target
        ).apply {
            duration = DURATION
        }
    }

    private fun cloudsOffsetAnim(selected: Boolean): Animator {
        val target = if (selected) 0f else measuredHeight.toFloat()
        return ObjectAnimator.ofFloat(
            this,
            "cloudsOffset",
            cloudsOffset,
            target
        ).apply {
            duration = DURATION
        }
    }

    private fun backgroundColorAnim(selected: Boolean): Animator {
        return ObjectAnimator.ofInt(
            this,
            "surfaceColor",
            surfaceColor,
            Color.parseColor(if (selected) "#3875B7" else "#171D2E"),
        ).apply {
            setEvaluator(ArgbEvaluator())
            duration = DURATION
            addUpdateListener {
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas ?: return
        super.onDraw(canvas)
        canvas.save()
        clipRound(canvas) // 裁剪圆角
        drawBackground(canvas) // 绘制背景
        drawThreeCircle(canvas) // 太阳周围的三个实心圆环绘制
        drawClouds(canvas, true) // 绘制远处云层
        drawSunAndMoon(canvas) // 绘制太阳和月亮
        drawClouds(canvas, false) // 绘制近处云层

        if (this::bitmap1.isInitialized) { // 绘制星星
            drawStarts(canvas)
        } else { // 初始化星星
            invalidateStartBitmap()
        }
        canvas.restore()
    }

    private fun clipRound(canvas: Canvas) {
        if (clipPath.isEmpty) { // 裁剪圆角路径空时，初始化裁剪路径
            val round = measuredWidth.toFloat() / 2
            clipPath.addRoundRect(
                0f,
                0f,
                measuredWidth.toFloat(),
                measuredHeight.toFloat(),
                round,
                round,
                Path.Direction.CW
            )
        }
        canvas.clipPath(clipPath)
    }

    // 初始化星星位图
    private fun invalidateStartBitmap() {
        val measuredHeight = measuredHeight
        val startsHeight = measuredHeight / 4
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_start, context.theme)!!
        bitmap1 = drawable.toBitmap(startsHeight, startsHeight)

        val drawable2 = ResourcesCompat.getDrawable(resources, R.drawable.ic_start, context.theme)!!
        bitmap2 = drawable2.toBitmap(startsHeight / 2, startsHeight / 2)

        val drawable3 = ResourcesCompat.getDrawable(resources, R.drawable.ic_start, context.theme)!!
        bitmap3 = drawable3.toBitmap(startsHeight / 3, startsHeight / 3)
        // 星星偏移量
        startsOffset = measuredHeight.toFloat()
        invalidate()
    }

    private fun drawStarts(canvas: Canvas) {
        canvas.save()
        val spacing = measuredWidth / 14f
        // 偏移一下画笔
        canvas.translate(spacing, -startsOffset)
        // 画星星位图
        canvas.drawBitmap(bitmap2, spacing, spacing, startsPaint)
        canvas.drawBitmap(bitmap1, spacing * 3f, spacing / 2f, startsPaint)
        canvas.drawBitmap(bitmap3, spacing * 5f, spacing, startsPaint)
        canvas.drawBitmap(bitmap1, spacing * 5f, spacing * 2f, startsPaint)
        canvas.drawBitmap(bitmap1, spacing * 1.8f, spacing * 3f, startsPaint)
        canvas.drawBitmap(bitmap3, spacing / 1.5f, spacing * 4f, startsPaint)
        canvas.drawBitmap(bitmap3, spacing * 3f, spacing * 5f, startsPaint)
        canvas.restore()
    }

    // 太阳偏移量
    private var sunOffset = 0f
        set(value) {
            field = value
            invalidate()
        }

    // 星星偏移量
    private var startsOffset = 0f
        set(value) {
            field = value
            invalidate()
        }

    // 云层偏移量
    private var cloudsOffset = 0f
        set(value) {
            field = value
            invalidate()
        }

    // 背景颜色
    private var surfaceColor = Color.parseColor("#32649B")
        set(value) {
            field = value
            invalidate()
        }

    private fun drawSunAndMoon(canvas: Canvas) {
        val circleRadius = getSunAndMoonCircleRadius()
        val spacing = getSpacing()
        val startX = sunOffset + circleRadius + spacing
        sunAndMoonPaint.color = Color.TRANSPARENT
        canvas.drawCircle(
            startX,
            measuredHeight.toFloat() / 2,
            circleRadius,
            sunAndMoonPaint
        )
        sunAndMoonPaint.color = Color.parseColor("#FDB830")
        // 新建图层
        canvas.save()
        // 画布裁剪
        canvas.clipPath(Path().apply {
            addCircle(
                startX,
                measuredHeight.toFloat() / 2,
                circleRadius,
                Path.Direction.CW
            )
        })

        // 太阳
        canvas.drawCircle(
            startX,
            measuredHeight.toFloat() / 2,
            circleRadius,
            sunAndMoonPaint
        )

        // 月球
        sunAndMoonPaint.color = Color.parseColor("#BDC1CC")
        val fl = measuredWidth.toFloat() - getSunAndMoonCircleWidth() - (getSpacing() * 2)
        val startX2 = fl + circleRadius + spacing
        canvas.drawCircle(
            fl + circleRadius + spacing,
            measuredHeight.toFloat() / 2,
            circleRadius,
            sunAndMoonPaint
        )

        // 月坑
        val circleStartX = startX2 - (circleRadius / 2)
        val circleStartY = measuredHeight.toFloat() / 2
        val smallCircleRadius = circleRadius / 2.8f
        val list = mutableListOf(
            Triple(
                circleStartX,
                circleStartY + smallCircleRadius * 0.2f,
                smallCircleRadius,
            ),
            Triple(
                circleStartX + smallCircleRadius * 1.8f,
                circleStartY - smallCircleRadius * 1.5f,
                smallCircleRadius / 1.5f,
            ),
            Triple(
                circleStartX + smallCircleRadius * 2.5f,
                circleStartY + smallCircleRadius * 1f,
                smallCircleRadius / 1.5f,
            )
        )
        list.forEach {
            canvas.drawCircle(it.first, it.second, it.third, lunarPitPaint)
        }
        // 恢复图层
        canvas.restore()
    }

    // 左右间距
    private fun getSpacing() = (measuredHeight - getSunAndMoonCircleRadius()) / 4

    // 太阳和月亮的半径
    private fun getSunAndMoonCircleRadius() = measuredHeight.toFloat() / 2.5f

    // 太阳和月亮的直径
    private fun getSunAndMoonCircleWidth() = getSunAndMoonCircleRadius() * 2

    private val cloudsPath = Path()
    private fun drawClouds(canvas: Canvas, isBottom: Boolean) {
        val cloudCircleRadius = getCloudCircleRadius()
        if (cloudsPath.isEmpty) { // 云层路径为空时，初始化云层路径
            initCloudsPath()
        }
        canvas.save()
        canvas.translate(0f, cloudsOffset)
        if (isBottom) {
            canvas.save()
            ringPaint.alpha = 255 / 2
            canvas.translate(0f, -cloudCircleRadius / 2)
            canvas.drawPath(cloudsPath, ringPaint)
            canvas.restore()
        } else {
            ringPaint.alpha = 255
            canvas.drawPath(cloudsPath, ringPaint)
        }
        canvas.restore()
    }

    private fun getCloudCircleRadius() = measuredWidth.toFloat() / 6f

    private fun initCloudsPath() {
        val circleRadius = getCloudCircleRadius()
        val widthSplitStep = circleRadius
        val startX = circleRadius + getSpacing()
        val heightStep = measuredHeight.toFloat() / 9.5f
        val startY = measuredHeight.toFloat() + circleRadius - heightStep + (cloudsOffset)
        cloudsPath.addCircle(
            startX,
            startY,
            circleRadius,
            Path.Direction.CW
        )
        cloudsPath.addCircle(
            startX + widthSplitStep,
            startY - heightStep,
            circleRadius,
            Path.Direction.CW
        )
        cloudsPath.addCircle(
            startX + (widthSplitStep * 2),
            startY - (heightStep * 2),
            circleRadius,
            Path.Direction.CW
        )
        cloudsPath.addCircle(
            startX + (widthSplitStep * 3),
            startY - (heightStep * 1.8f),
            circleRadius,
            Path.Direction.CW
        )
        cloudsPath.addCircle(
            startX + (widthSplitStep * 4),
            startY - (heightStep * 3),
            circleRadius,
            Path.Direction.CW
        )
        cloudsPath.addCircle(
            startX + (widthSplitStep * 5),
            startY - (heightStep * 5),
            circleRadius,
            Path.Direction.CW
        )
    }

    private fun drawThreeCircle(canvas: Canvas) {
        ringPaint.alpha = (255 / 3.5).roundToInt()
        val widthStep = getSunAndMoonCircleRadius()
        val startX = sunOffset + getSpacing() + widthStep
        val radius = getSunAndMoonCircleRadius() / 1.5f
        canvas.drawCircle(
            startX,
            measuredHeight.toFloat() / 2,
            radius + widthStep,
            ringPaint
        )
        canvas.drawCircle(
            startX,
            measuredHeight.toFloat() / 2,
            radius + widthStep * 2,
            ringPaint
        )
        canvas.drawCircle(
            startX,
            measuredHeight.toFloat() / 2,
            radius + widthStep * 3,
            ringPaint
        )
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(surfaceColor)
    }
}