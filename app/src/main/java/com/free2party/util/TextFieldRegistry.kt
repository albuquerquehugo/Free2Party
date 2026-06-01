package com.free2party.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow

object TextFieldRegistry {
    private val bounds = mutableMapOf<Any, LayoutCoordinates>()

    fun register(key: Any, coordinates: LayoutCoordinates) {
        bounds[key] = coordinates
    }

    fun unregister(key: Any) {
        bounds.remove(key)
    }

    fun isPointInsideAnyTextField(position: Offset, rootCoordinates: LayoutCoordinates?): Boolean {
        val rootCoords = rootCoordinates ?: return false
        if (!rootCoords.isAttached) return false

        val positionInWindow = rootCoords.localToWindow(position)
        return bounds.values.any { coordinates ->
            if (coordinates.isAttached) {
                val boundsInWindow = coordinates.boundsInWindow()
                boundsInWindow.contains(positionInWindow)
            } else {
                false
            }
        }
    }
}
