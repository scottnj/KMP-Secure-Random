package com.scottnj.kmp_secure_random

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "kmp_secure_random",
    ) {
        App()
    }
}