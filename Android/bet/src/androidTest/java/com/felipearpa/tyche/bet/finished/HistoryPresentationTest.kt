package com.felipearpa.tyche.bet.finished

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.historyBetPlaceholderModel
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * History's row and summary as TalkBack reads them, its placeholders, and its date format. The
 * announcement is the single content description of each real row; placeholders expose nothing.
 */
class HistoryPresentationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aRowAnnouncesDateResultBetAndPointsInOrderAndOpensOnce() {
        var opened = 0
        setLocalized(Locale.US) {
            HistoryBetItem(
                poolGamblerBet = positiveRow(),
                dateFormat = HistoryMatchDateFormat(Locale.US, is24HourClock = false, today = TODAY),
                modifier = Modifier.fillMaxWidth().testTag(ROW_TAG),
                onClick = { opened++ },
            )
        }

        rowDescription() shouldBe
            "Sunday, Jul 19, 2:00 PM. Final score: Inglaterra 1, Argentina 2. Your bet: 2 to 1. 2 points"
        composeTestRule.onNodeWithTag(ROW_TAG).assertHasClickAction().performClick()
        opened shouldBe 1
    }

    @Test
    fun unavailableValuesAreDescribedRatherThanReadAsZero() {
        setLocalized(Locale.US) {
            HistoryBetItem(
                poolGamblerBet = positiveRow().copy(matchScore = null, betScore = null, score = null),
                dateFormat = HistoryMatchDateFormat(Locale.US, is24HourClock = false, today = TODAY),
                modifier = Modifier.fillMaxWidth().testTag(ROW_TAG),
            )
        }

        val description = rowDescription()
        description shouldContain "Inglaterra versus Argentina, final score unavailable"
        description shouldContain "No bet placed"
        description shouldContain "Points unavailable"
        description shouldNotContain "0 points"
    }

    @Test
    fun aSinglePointIsSpokenInTheSingularInSpanish() {
        setLocalized(SPANISH) {
            HistoryBetItem(
                poolGamblerBet = positiveRow().copy(score = 1),
                dateFormat = HistoryMatchDateFormat(SPANISH, is24HourClock = false, today = TODAY),
                modifier = Modifier.fillMaxWidth().testTag(ROW_TAG),
            )
        }

        val description = rowDescription()
        description shouldNotContain ".."
        description shouldContain "Tu apuesta: 2 a 1"
        description shouldContain ". 1 punto"
        description shouldNotContain "1 puntos"
    }

    @Test
    fun theSummaryAnnouncesCurrentPoolPointsWithSingularAndPluralWords() {
        val cases = listOf(
            Triple(Locale.US, HistoryPoints.Earned(120), "120 points earned in this pool"),
            Triple(Locale.US, HistoryPoints.Earned(1), "1 point earned in this pool"),
            Triple(Locale.US, HistoryPoints.Unavailable, "Points earned in this pool unavailable"),
            Triple(SPANISH, HistoryPoints.Earned(1), "1 punto ganado en esta polla"),
        )
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                cases.forEachIndexed { index, (locale, points, _) ->
                    Localized(locale) {
                        TycheTheme {
                            HistoryPointsSummary(points = points, modifier = Modifier.testTag("$SUMMARY_TAG$index"))
                        }
                    }
                }
            }
        }

        cases.forEachIndexed { index, (_, _, expected) ->
            composeTestRule.onNodeWithTag("$SUMMARY_TAG$index").fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription].single() shouldBe expected
        }
    }

    @Test
    fun positivePointsAreSignedAndZeroOrUnavailableNeverShowAPlus() {
        val points = listOf(
            HistoryPoints.Earned(120),
            HistoryPoints.Earned(1),
            HistoryPoints.Earned(0),
            HistoryPoints.Unavailable,
        )
        val texts = mutableMapOf<HistoryPoints, String>()
        setLocalized(Locale.US) {
            points.forEach { value -> texts[value] = historyPointsShortText(value) }
        }
        composeTestRule.waitForIdle()

        texts shouldBe mapOf(
            HistoryPoints.Earned(120) to "+120 pts",
            HistoryPoints.Earned(1) to "+1 pt",
            HistoryPoints.Earned(0) to "0 pts",
            HistoryPoints.Unavailable to "— pts",
        )
    }

    @Test
    fun placeholdersExposeNoValuesOrActions() {
        setLocalized(Locale.US) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.testTag(ROW_TAG)) {
                HistoryPointsSummary(points = historyPointsPlaceholderModel, isPlaceholder = true)
                HistoryBetItem(
                    poolGamblerBet = historyBetPlaceholderModel(),
                    dateFormat = HistoryMatchDateFormat(Locale.US, is24HourClock = false, today = TODAY),
                    isPlaceholder = true,
                    onClick = { error("A placeholder must not open a match") },
                )
            }
        }

        composeTestRule.onAllNodesWithText("X", substring = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("pt", substring = true).assertCountEquals(0)
        composeTestRule.onNodeWithTag(ROW_TAG).assertHasNoClickAction()
        composeTestRule.onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
        ).assertCountEquals(0)
    }

    @Test
    fun theDateFollowsTheLocaleOrderAndLanguage() {
        val match = LocalDateTime(2026, 7, 19, 14, 0)
        HistoryMatchDateFormat(Locale.US, is24HourClock = false, today = TODAY).date(match) shouldBe "Sunday, Jul 19"
        // Spanish puts the day before the month.
        HistoryMatchDateFormat(SPANISH, is24HourClock = false, today = TODAY).date(match).let { date ->
            date shouldStartWith "domingo"
            date shouldContain "19 de jul"
        }
    }

    @Test
    fun aMatchFromAnotherYearIncludesIt() {
        val format = HistoryMatchDateFormat(Locale.US, is24HourClock = false, today = TODAY)

        format.date(LocalDateTime(2025, 6, 11, 20, 0)) shouldContain "2025"
        format.date(LocalDateTime(2026, 6, 11, 20, 0)) shouldNotContain "2026"
    }

    @Test
    fun theTimeFollowsTheClockPreference() {
        val match = LocalDateTime(2026, 7, 19, 14, 0)

        HistoryMatchDateFormat(Locale.US, is24HourClock = false, today = TODAY).time(match) shouldBe "2:00 PM"
        HistoryMatchDateFormat(Locale.US, is24HourClock = true, today = TODAY).time(match) shouldBe "14:00"
    }

    private fun rowDescription(): String =
        composeTestRule.onNodeWithTag(ROW_TAG).fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].single()

    private fun setLocalized(locale: Locale, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            Localized(locale) { TycheTheme { content() } }
        }
    }

    private fun positiveRow(): PoolGamblerBetModel = historyBetPreviewModels().first()
}

/** Renders [content] with resources for [locale], whatever the device language. */
@Composable
private fun Localized(locale: Locale, content: @Composable () -> Unit) {
    val base = InstrumentationRegistry.getInstrumentation().targetContext
    val configuration = Configuration(base.resources.configuration).apply { setLocale(locale) }
    val context = base.createConfigurationContext(configuration)
    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides configuration,
        LocalResources provides context.resources,
        content = content,
    )
}

private val TODAY = LocalDate(2026, 10, 3)
private val SPANISH = Locale.forLanguageTag("es-CO")
private const val ROW_TAG = "historyRow"
private const val SUMMARY_TAG = "historySummary"
