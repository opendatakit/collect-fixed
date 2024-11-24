package org.odk.collect.android.widgets

import android.content.Context
import android.widget.FrameLayout

abstract class WidgetAnswer(context: Context) : FrameLayout(context) {
    abstract fun setAnswer(answer: String?)

    abstract fun getAnswer(): String

    abstract fun setTextSize(textSize: Float)
}
