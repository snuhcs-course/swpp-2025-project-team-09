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
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility

@RunWith(AndroidJUnit4::class)
class TutorialActivityTest {

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val activityRule = ActivityScenarioRule(TutorialActivity::class.java)

    private val lastPageIndex = 9

    //------------------------------------
    // 1. Initial State
    //------------------------------------

    @Test
    fun initialState_showsFirstTutorialPage() {
        onView(withId(R.id.nextButton))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.next)))
        onView(withId(R.id.startButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    //------------------------------------
    // 2. Navigation
    //------------------------------------

    @Test
    fun clickNext_movesToNextPage() {
        onView(withId(R.id.nextButton)).perform(click())
        onView(withId(R.id.nextButton))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.next)))
    }

    @Test
    fun lastPage_showsStartButton() {
        repeat(lastPageIndex) {
            onView(withId(R.id.nextButton)).perform(click())
        }

        onView(withId(R.id.nextButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.startButton))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.tutorial_start)))
    }

    @Test
    fun clickStartOnLastPage_navigatesToMain() {
        repeat(lastPageIndex) {
            onView(withId(R.id.nextButton)).perform(click())
        }

        onView(withId(R.id.startButton)).perform(click())

        intended(hasComponent(MainActivity::class.java.name))
    }

    //------------------------------------
    // 3. UI Elements
    //------------------------------------

    @Test
    fun indicatorDots_updateCorrectly() {
        onView(withId(R.id.indicatorLayout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.nextButton)).perform(click())

        onView(withId(R.id.indicatorLayout))
            .check(matches(isDisplayed()))
    }
}