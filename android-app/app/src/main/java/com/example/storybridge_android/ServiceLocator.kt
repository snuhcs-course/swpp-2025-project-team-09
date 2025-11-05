package com.example.storybridge_android

import com.example.storybridge_android.data.*

object ServiceLocator {
    lateinit var userRepository: UserRepository
    lateinit var sessionRepository: SessionRepository
    lateinit var pageRepository: PageRepository
    lateinit var processRepository: ProcessRepository


    init {
        reset()
    }

    fun reset() {
        userRepository = UserRepositoryImpl()
        sessionRepository = SessionRepositoryImpl()
        pageRepository = PageRepositoryImpl()
        processRepository = ProcessRepositoryImpl()
    }
}