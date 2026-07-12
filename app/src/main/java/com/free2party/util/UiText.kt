package com.free2party.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(
        @get:StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    class PluralStringResource(
        @get:PluralsRes val resId: Int,
        val quantity: Int,
        vararg val args: Any
    ) : UiText()

    data class Composite(val parts: List<UiText>, val separator: String = "") : UiText()

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
            is PluralStringResource -> pluralStringResource(resId, quantity, *args)
            is Composite -> {
                var result = ""
                parts.forEachIndexed { index, part ->
                    result += part.asString()
                    if (index < parts.size - 1) result += separator
                }
                result
            }
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
            is PluralStringResource -> context.resources.getQuantityString(resId, quantity, *args)
            is Composite -> parts.joinToString(separator) { it.asString(context) }
        }
    }
}
