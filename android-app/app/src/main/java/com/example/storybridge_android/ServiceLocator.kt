package com.example.storybridge_android

import com.example.storybridge_android.data.DefaultUserRepository
import com.example.storybridge_android.data.UserRepository

object ServiceLocator {
    lateinit var userRepository: UserRepository

    init {
        reset()
    }

    fun reset() {
        userRepository = DefaultUserRepository()
    }
}