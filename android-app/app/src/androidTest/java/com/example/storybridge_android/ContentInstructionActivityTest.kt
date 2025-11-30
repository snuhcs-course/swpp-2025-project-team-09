package com.example.storybridge_android.ui.session

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.storybridge_android.R
import com.example.storybridge_android.ui.camera.CameraSessionActivity
import com.example.storybridge_android.ui.session.instruction.ContentInstructionActivity
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentInstructionActivityTest {

    private lateinit var scenario: ActivityScenario<ContentInstructionActivity>

    @Before
    fun setup() {
        Intents.init()
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            ContentInstructionActivity::class.java
        ).apply {
            putExtra("session_id", "test-session-123")
        }
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun teardown() {
        Intents.release()
        scenario.close()
    }

    @Test
    fun startButton_navigatesToCameraSessionActivity() {
        onView(withId(R.id.contentInstructionButton)).perform(click())

        Intents.intended(
            allOf(
                hasComponent(CameraSessionActivity::class.java.name),
                IntentMatchers.hasExtra("session_id", "test-session-123"),
                IntentMatchers.hasExtra("page_index", 1)
            )
        )
    }

    @Test
    fun screenLoads_correctButtonText() {
        onView(withId(R.id.contentInstructionButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun missingSessionId_finishesActivity() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            ContentInstructionActivity::class.java
        )

        val scenario = ActivityScenario.launch<ContentInstructionActivity>(intent)

        var destroyed = false
        try {
            scenario.onActivity { }
        } catch (e: NullPointerException) {
            destroyed = true
        }

        assert(destroyed)
    }
}
