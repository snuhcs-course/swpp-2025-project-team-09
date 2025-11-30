package com.example.storybridge_android

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.storybridge_android.ui.main.MainActivity
import com.example.storybridge_android.ui.landing.TutorialActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TutorialActivityTest {

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val activityRule = ActivityScenarioRule(TutorialActivity::class.java)

    @Test
    fun initialState_showsFirstTutorialPage() {
        onView(withId(R.id.nextButton))
            .check(matches(withText(R.string.next)))
    }

    @Test
    fun clickNext_movesToNextPage() {
        onView(withId(R.id.nextButton)).perform(click())
        onView(withId(R.id.nextButton))
            .check(matches(withText(R.string.next)))
    }

    @Test
    fun lastPage_showsStartButton() {
        repeat(7) {
            onView(withId(R.id.nextButton)).perform(click())
        }

        onView(withId(R.id.nextButton))
            .check(matches(withText(R.string.tutorial_start)))
    }

    @Test
    fun clickStartOnLastPage_navigatesToMain() {
        repeat(7) {
            onView(withId(R.id.nextButton)).perform(click())
        }
        onView(withId(R.id.nextButton)).perform(click())
        intended(hasComponent(MainActivity::class.java.name))
    }

    @Test
    fun indicatorDots_updateCorrectly() {
        onView(withId(R.id.indicatorLayout))
            .check(matches(isDisplayed()))
        onView(withId(R.id.nextButton)).perform(click())
        onView(withId(R.id.indicatorLayout))
            .check(matches(isDisplayed()))
    }
}