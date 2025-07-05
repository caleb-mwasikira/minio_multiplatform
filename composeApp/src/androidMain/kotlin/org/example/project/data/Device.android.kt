package org.example.project.data

import android.os.Build

actual fun getDeviceName(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    return if (model.startsWith(manufacturer)) {
        model.capitalize()
    } else {
        "$manufacturer $model".capitalize()
    }
}
