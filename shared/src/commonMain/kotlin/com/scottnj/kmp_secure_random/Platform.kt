package com.scottnj.kmp_secure_random

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform