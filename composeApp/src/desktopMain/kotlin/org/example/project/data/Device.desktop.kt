package org.example.project.data

actual fun getDeviceName(): String {
    return System.getProperty("user.name")
}