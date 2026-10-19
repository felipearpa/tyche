package com.felipearpa.tyche.bet.pending

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.launch

/** How a score box looks, from its value, its neighbor's value, and focus. */
internal enum class BetScoreBoxAppearance {
    /** No value and none on the other side either: a dashed outline around "?". */
    EMPTY,

    /** No value while the other side has one: an amber dashed outline around an amber "?". */
    MISSING,

    /** A value: a filled box. */
    FILLED,

    /** The box being typed into: a green border. */
    FOCUSED;

    companion object {
        fun of(value: Int?, otherValue: Int?, isFocused: Boolean): BetScoreBoxAppearance = when {
            isFocused -> FOCUSED
            value != null -> FILLED
            otherValue == null -> EMPTY
            else -> MISSING
        }
    }
}

/**
 * The score a box keeps from typed text: ASCII digits only, leading zeros removed, at most two
 * digits. Text without digits is no score (null), never 0.
 */
internal fun parseBetScoreInput(text: String): Int? {
    val digits = text.filter { char -> char in '0'..'9' }
    if (digits.isEmpty()) return null
    val significant = digits.trimStart('0')
    if (significant.isEmpty()) return 0
    return significant.take(MAXIMUM_SCORE_DIGITS).toInt()
}

private const val MAXIMUM_SCORE_DIGITS = 2

/**
 * A team's editable score: a 52-dp box with the number keyboard. Focusing it selects its value,
 * so a typed digit replaces it. [onInput] receives the value after [parseBetScoreInput] for every
 * edit the gambler types, also one that leaves the value as it was (2 typed over a selected 2),
 * so the card can move focus on the first digit; values the card sets by other means (steppers,
 * chips, a restore) only redraw the box.
 */
@Composable
internal fun BetScoreBox(
    value: Int?,
    otherValue: Int?,
    teamName: String,
    isFocused: Boolean,
    imeAction: ImeAction,
    onInput: (Int?) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val colors = pendingBetColors()
    val state = rememberTextFieldState(initialText = value?.toString().orEmpty())
    val scope = rememberCoroutineScope()
    val currentOnInput by rememberUpdatedState(onInput)
    var hasFocus by remember { mutableStateOf(false) }
    val input = remember(state) { BetScoreInput(onTyped = { typed -> scope.launch { currentOnInput(typed) } }) }

    // Only a new value from the card redraws the text; typed text the row has not echoed back yet
    // stays as typed.
    LaunchedEffect(value) {
        if (parseBetScoreInput(state.text.toString()) != value) state.setTextAndPlaceCursorAtEnd(value?.toString().orEmpty())
    }

    val appearance = BetScoreBoxAppearance.of(value = value, otherValue = otherValue, isFocused = isFocused)
    val boxLabel = stringResource(R.string.bet_score_box_accessibility, teamName)
    // An empty box beside a filled one is the score the card still needs before it saves.
    val emptyValue = if (otherValue != null) {
        stringResource(R.string.bet_score_box_missing_accessibility_value, teamName)
    } else {
        stringResource(R.string.bet_score_box_empty_accessibility_value)
    }

    BasicTextField(
        state = state,
        inputTransformation = input,
        textStyle = PendingBetStyle.scoreTextStyle().copy(color = colors.primaryText, textAlign = TextAlign.Center),
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(colors.focusedOutline),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        onKeyboardAction = { onImeAction() },
        modifier = modifier
            .size(PendingBetStyle.scoreBoxSize)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused == hasFocus) return@onFocusChanged
                hasFocus = focusState.isFocused
                if (hasFocus) {
                    state.edit { selectAll() }
                    input.keepSelectionThroughNextCaretMove = state.text.isNotEmpty()
                } else {
                    // The selection made on focus would otherwise stay highlighted on the idle box.
                    state.edit { placeCursorAtEnd() }
                }
                onFocusChange(hasFocus)
            }
            .semantics {
                contentDescription = boxLabel
                if (value == null) stateDescription = emptyValue
            },
        decorator = { innerTextField ->
            BetScoreBoxFrame(appearance = appearance, colors = colors) {
                innerTextField()
                if (value == null && !isFocused) BetScoreQuestionMark(appearance = appearance, colors = colors)
            }
        },
    )
}

/**
 * The score box's input rule. Runs for every edit the gambler makes, and for caret moves: an
 * edit is normalized with [parseBetScoreInput] and reported to [onTyped], even when it leaves the
 * text as it was; a caret move only keeps the selection made on focus, which the tap that
 * focused the box would otherwise collapse. Only the change count tells a typed digit that
 * leaves the text as it was from a caret move, hence the experimental [TextFieldBuffer.changes].
 */
@OptIn(ExperimentalFoundationApi::class)
private class BetScoreInput(private val onTyped: (Int?) -> Unit) : InputTransformation {
    var keepSelectionThroughNextCaretMove = false

    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount == 0) {
            if (keepSelectionThroughNextCaretMove) selectAll()
            keepSelectionThroughNextCaretMove = false
            return
        }
        keepSelectionThroughNextCaretMove = false
        val parsed = parseBetScoreInput(asCharSequence().toString())
        val normalized = parsed?.toString().orEmpty()
        if (!asCharSequence().contentEquals(normalized)) {
            replace(0, length, normalized)
            placeCursorAtEnd()
        }
        onTyped(parsed)
    }
}

/**
 * A locked or placeholder card's score: the same box geometry, muted, with no input. A match
 * locked without a prediction shows a dash in each box.
 */
@Composable
internal fun BetScoreReadOnlyBox(
    value: Int?,
    teamName: String,
    modifier: Modifier = Modifier,
) {
    val colors = pendingBetColors()
    val boxLabel = stringResource(R.string.bet_score_box_accessibility, teamName)
    val spokenValue = value?.toString() ?: stringResource(R.string.bet_score_box_empty_accessibility_value)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(PendingBetStyle.scoreBoxSize)
            .clearAndSetSemantics {
                contentDescription = boxLabel
                stateDescription = spokenValue
            },
    ) {
        Text(
            text = value?.toString() ?: LOCKED_WITHOUT_BET,
            style = PendingBetStyle.scoreTextStyle(),
            color = colors.secondaryText,
        )
    }
}

private const val LOCKED_WITHOUT_BET = "–"

/** The box's fill and outline. Only colors change between appearances, never the size. */
@Composable
private fun BetScoreBoxFrame(
    appearance: BetScoreBoxAppearance,
    colors: PendingBetColors,
    content: @Composable () -> Unit,
) {
    val fill by animateColorAsState(
        targetValue = when (appearance) {
            BetScoreBoxAppearance.EMPTY -> Color.Transparent
            BetScoreBoxAppearance.MISSING -> colors.amberFill
            BetScoreBoxAppearance.FILLED, BetScoreBoxAppearance.FOCUSED -> colors.controlFill
        },
        animationSpec = PendingBetStyle.spec(),
        label = "scoreBoxFill",
    )
    val stroke by animateColorAsState(
        targetValue = when (appearance) {
            BetScoreBoxAppearance.EMPTY -> colors.emptyOutline
            BetScoreBoxAppearance.MISSING -> colors.amber
            BetScoreBoxAppearance.FOCUSED -> colors.focusedOutline
            BetScoreBoxAppearance.FILLED -> Color.Transparent
        },
        animationSpec = PendingBetStyle.spec(),
        label = "scoreBoxStroke",
    )
    val isDashed = appearance == BetScoreBoxAppearance.EMPTY || appearance == BetScoreBoxAppearance.MISSING
    val strokeWidth = if (isDashed) PendingBetStyle.outlineWidth else PendingBetStyle.focusedOutlineWidth
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PendingBetStyle.scoreBoxSize)
            .background(fill, RoundedCornerShape(PendingBetStyle.scoreBoxCornerRadius))
            .roundedOutline(color = stroke, width = strokeWidth, isDashed = isDashed),
    ) {
        content()
    }
}

/** An inset rounded outline, dashed for empty boxes. */
private fun Modifier.roundedOutline(color: Color, width: Dp, isDashed: Boolean): Modifier = drawBehind {
    val strokePx = width.toPx()
    val inset = strokePx / 2
    val radius = PendingBetStyle.scoreBoxCornerRadius.toPx() - inset
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokePx, size.height - strokePx),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(
            width = strokePx,
            pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
        ),
    )
}

@Composable
private fun BetScoreQuestionMark(appearance: BetScoreBoxAppearance, colors: PendingBetColors) {
    val color by animateColorAsState(
        targetValue = if (appearance == BetScoreBoxAppearance.MISSING) colors.amber else colors.secondaryText,
        animationSpec = PendingBetStyle.spec(),
        label = "questionMark",
    )
    Text(text = "?", style = PendingBetStyle.scoreTextStyle(), color = color)
}

@PreviewLightDark
@Composable
private fun BetScoreBoxPreview() {
    TycheTheme {
        Surface(color = pendingBetColors().cardFill) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                modifier = Modifier.background(pendingBetColors().cardFill),
            ) {
                listOf(null to null, null to 2, 2 to 1).forEach { (value, other) ->
                    BetScoreBox(
                        value = value,
                        otherValue = other,
                        teamName = "Colombia",
                        isFocused = false,
                        imeAction = ImeAction.Next,
                        onInput = {},
                        onFocusChange = {},
                        onImeAction = {},
                    )
                }
                BetScoreBox(
                    value = 2,
                    otherValue = 1,
                    teamName = "Colombia",
                    isFocused = true,
                    imeAction = ImeAction.Next,
                    onInput = {},
                    onFocusChange = {},
                    onImeAction = {},
                )
                BetScoreReadOnlyBox(value = 2, teamName = "Colombia")
                BetScoreReadOnlyBox(value = null, teamName = "Colombia")
            }
        }
    }
}
