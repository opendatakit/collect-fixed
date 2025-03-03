package org.odk.collect.android.widgets

import android.content.Context
import android.view.LayoutInflater
import org.odk.collect.android.databinding.ArbitraryFileWidgetAnswerBinding

class ArbitraryFileWidgetAnswer(context: Context) : WidgetAnswer(context) {
    private val binding = ArbitraryFileWidgetAnswerBinding.inflate(LayoutInflater.from(context), this, true)

    override fun setAnswer(answer: String?) {
        binding.answer.text = answer
    }

    override fun getAnswer(): String {
        return binding.answer.text.toString()
    }
}
