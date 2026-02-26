package com.example.free2party.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class PhoneVisualTransformation(val mask: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Raw digits from input
        val trimmed = text.text
        var out = ""
        var maskIndex = 0
        var textIndex = 0

        while (maskIndex < mask.length && textIndex < trimmed.length) {
            if (mask[maskIndex] == '#') {
                out += trimmed[textIndex]
                textIndex++
            } else {
                out += mask[maskIndex]
            }
            maskIndex++
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var originalIndex = 0
                var transformedIndex = 0
                while (originalIndex < offset && transformedIndex < mask.length) {
                    if (mask[transformedIndex] == '#') {
                        originalIndex++
                    }
                    transformedIndex++
                }
                return transformedIndex
            }

            override fun transformedToOriginal(offset: Int): Int {
                var originalIndex = 0
                var transformedIndex = 0
                while (transformedIndex < offset && transformedIndex < mask.length) {
                    if (mask[transformedIndex] == '#') {
                        originalIndex++
                    }
                    transformedIndex++
                }
                return originalIndex
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
