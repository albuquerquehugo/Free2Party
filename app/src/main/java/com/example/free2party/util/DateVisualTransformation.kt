package com.example.free2party.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.free2party.data.model.DatePattern

class DateVisualTransformation(val datePattern: DatePattern) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Raw text is expected to be digits (max 8)
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        
        when (datePattern) {
            DatePattern.YYYY_MM_DD -> {
                for (i in trimmed.indices) {
                    out += trimmed[i]
                    if (i == 3 || i == 5) out += "/"
                }
            }
            DatePattern.MM_DD_YYYY -> {
                for (i in trimmed.indices) {
                    out += trimmed[i]
                    if (i == 1 || i == 3) out += "/"
                }
            }
            DatePattern.DD_MM_YYYY -> {
                for (i in trimmed.indices) {
                    out += trimmed[i]
                    if (i == 1 || i == 3) out += "/"
                }
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when (datePattern) {
                    DatePattern.YYYY_MM_DD -> {
                        if (offset <= 3) offset
                        else if (offset <= 5) offset + 1
                        else offset + 2
                    }
                    else -> { // MM_DD_YYYY or DD_MM_YYYY
                        if (offset <= 1) offset
                        else if (offset <= 3) offset + 1
                        else offset + 2
                    }
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when (datePattern) {
                    DatePattern.YYYY_MM_DD -> {
                        if (offset <= 4) offset
                        else if (offset <= 7) offset - 1
                        else offset - 2
                    }
                    else -> { // MM_DD_YYYY or DD_MM_YYYY
                        if (offset <= 2) offset
                        else if (offset <= 5) offset - 1
                        else offset - 2
                    }
                }
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
