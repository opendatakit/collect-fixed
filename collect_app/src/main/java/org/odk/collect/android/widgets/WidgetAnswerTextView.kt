package org.odk.collect.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import com.google.android.material.textview.MaterialTextView
import org.odk.collect.android.R
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.widgets.utilities.QuestionFontSizeUtils
import org.odk.collect.settings.SettingsProvider
import javax.inject.Inject

class WidgetAnswerTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialTextView(context, attrs, R.attr.widgetAnswerTextViewStyle) {

    @Inject
    lateinit var settingsProvider: SettingsProvider

    init {
        DaggerUtils.getComponent(context).inject(this)

        val textSize = QuestionFontSizeUtils.getFontSize(
            settingsProvider.getUnprotectedSettings(),
            QuestionFontSizeUtils.FontSize.HEADLINE_6
        )
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize.toFloat())
    }
}
