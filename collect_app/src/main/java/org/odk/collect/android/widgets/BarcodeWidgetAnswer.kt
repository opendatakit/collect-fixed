package org.odk.collect.android.widgets

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import org.odk.collect.android.databinding.BarcodeWidgetAnswerBinding

class BarcodeWidgetAnswer(context: Context) : WidgetAnswer(context) {
    private val binding = BarcodeWidgetAnswerBinding.inflate(LayoutInflater.from(context), this, true)

    override fun setAnswer(answer: String?) {
        binding.answer.text = stripInvalidCharacters(answer)
    }

    override fun getAnswer(): String {
        return binding.answer.text.toString()
    }

    override fun setTextSize(textSize: Float) {
        binding.answer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
    }

    // Remove control characters, invisible characters and unused code points.
    private fun stripInvalidCharacters(data: String?): String? {
        return data?.replace("\\p{C}".toRegex(), "")
    }
}
