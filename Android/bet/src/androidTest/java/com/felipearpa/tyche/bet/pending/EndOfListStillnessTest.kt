package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** The cards above stay put when the last item shrinks at the very end of a list. */
class EndOfListStillnessTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenTheLastItemShrinksAtTheEndTheItemsAboveDoNotMove() {
        var lastHeight by mutableStateOf(300.dp)
        lateinit var listState: LazyListState
        composeTestRule.setContent {
            listState = rememberLazyListState()
            val stillness = rememberEndOfListStillness(listState)
            LazyColumn(state = listState, modifier = Modifier.size(width = 200.dp, height = 500.dp)) {
                items(count = 10, key = { index -> index }) { Spacer(Modifier.fillMaxWidth().height(200.dp)) }
                item(key = LAST_KEY) {
                    Spacer(with(stillness) { Modifier.trackLastItem(LAST_KEY) }.fillMaxWidth().height(lastHeight))
                }
            }
        }
        composeTestRule.runOnIdle { runBlocking { listState.scrollToItem(10) } }
        composeTestRule.waitForIdle()
        val before = firstVisible(listState)

        lastHeight = 100.dp
        composeTestRule.waitForIdle()

        firstVisible(listState) shouldBe before
        listState.canScrollForward shouldBe false
    }

    @Test
    fun awayFromTheEndTheLastItemShrinksNormally() {
        var lastHeight by mutableStateOf(300.dp)
        lateinit var listState: LazyListState
        composeTestRule.setContent {
            listState = rememberLazyListState()
            val stillness = rememberEndOfListStillness(listState)
            LazyColumn(state = listState, modifier = Modifier.size(width = 200.dp, height = 500.dp)) {
                items(count = 10, key = { index -> index }) { Spacer(Modifier.fillMaxWidth().height(200.dp)) }
                item(key = LAST_KEY) {
                    Spacer(with(stillness) { Modifier.trackLastItem(LAST_KEY) }.fillMaxWidth().height(lastHeight))
                }
            }
        }

        lastHeight = 100.dp
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { runBlocking { listState.scrollToItem(10) } }
        composeTestRule.waitForIdle()

        val last = listState.layoutInfo.visibleItemsInfo.last()
        last.key shouldBe LAST_KEY
        last.size shouldBe with(composeTestRule.density) { 100.dp.roundToPx() }
    }

    private fun firstVisible(state: LazyListState) = state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset

    private companion object {
        const val LAST_KEY = "last"
    }
}
