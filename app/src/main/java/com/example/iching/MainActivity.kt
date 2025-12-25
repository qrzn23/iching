package com.example.iching

import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.iching.data.HexagramEntry
import com.example.iching.data.HexagramRepository
import com.example.iching.domain.CastResult
import com.example.iching.domain.castCoins3
import com.example.iching.domain.changedBits
import com.example.iching.domain.changingLines
import com.example.iching.domain.primaryBits
import com.example.iching.domain.toKey
import com.example.iching.ui.theme.IChingTheme
import androidx.activity.compose.BackHandler
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureImmersiveMode()
        setContent {
            IChingTheme {
                IChingApp(onExit = { finish() })
            }
        }
    }

    private fun configureImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

private object Routes {
    const val CONSULT = "consult"
    const val CAST = "cast"
    const val INTERPRETATION = "interpretation"
    const val VIEWER = "viewer"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

private enum class Section {
    DESCRIPTION,
    JUDGMENT,
    IMAGE,
    CHANGING_LINES
}

private enum class LineState {
    UNCAST,
    YIN,
    YANG
}

private enum class AppButtonStyle {
    FILLED,
    MENU
}

private data class TrigramOption(
    val name: String,
    val bits: IntArray
)

@Composable
private fun IChingApp(onExit: () -> Unit) {
    val context = LocalContext.current
    val repository = remember {
        HexagramRepository(context.applicationContext.assets).also { it.load() }
    }
    val navController = rememberNavController()
    var castSeed by rememberSaveable { mutableStateOf<Long?>(null) }
    var castLines by rememberSaveable { mutableStateOf<List<Int>?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AppColors.Background,
                drawerTonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .systemBarsPadding()
                        .padding(top = 24.dp, bottom = 16.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppMenuItem(text = "History", onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.HISTORY)
                    })
                    AppMenuItem(text = "Hexagram Viewer", onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.VIEWER)
                    })
                    AppMenuItem(text = "Settings", onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.SETTINGS)
                    })
                    AppMenuItem(text = "Exit", onClick = {
                        scope.launch { drawerState.close() }
                        onExit()
                    })
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .systemBarsPadding()
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.CONSULT
            ) {
                composable(Routes.CONSULT) {
                    ConsultScreen(
                        onConsult = { navController.navigate(Routes.CAST) }
                    )
                }
                composable(Routes.CAST) {
                    CastScreen(
                        onCast = { result ->
                            castSeed = result.seed
                            castLines = result.lines.toList()
                            navController.navigate(Routes.INTERPRETATION)
                        }
                    )
                }
                composable(Routes.INTERPRETATION) {
                    val cast = buildSavedCast(castSeed, castLines)
                    val entry = cast?.let { repository.getByKeyPrimary(it.keyPrimary) }
                    InterpretationScreen(
                        entry = entry,
                        cast = cast
                    )
                }
                composable(Routes.VIEWER) {
                    HexagramViewerScreen(repository = repository)
                }
                composable(Routes.HISTORY) {
                    PlaceholderScreen(title = "History")
                }
                composable(Routes.SETTINGS) {
                    PlaceholderScreen(title = "Settings")
                }
            }
        }
    }
}

@Composable
private fun ConsultScreen(onConsult: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AppButton(
            text = "CONSULT",
            onClick = onConsult
        )
    }
}

@Composable
private fun CastScreen(onCast: (CastResult) -> Unit) {
    var seed by rememberSaveable { mutableStateOf<Long?>(null) }
    var allLines by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var revealedCount by rememberSaveable { mutableStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            val lineStates = buildCastingStates(allLines, revealedCount)
            HexagramGlyph(
                lineStates = lineStates,
                animate = true,
                lineColor = AppColors.TextPrimary,
                brokenColor = AppColors.TextSecondary,
                uncastColor = AppColors.UncastLine,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
            AppButton(
                text = "TOSS COINS",
                onClick = {
                    if (revealedCount == 0) {
                        val newSeed = System.currentTimeMillis()
                        seed = newSeed
                        allLines = castCoins3(newSeed).toList()
                        revealedCount = 1
                    } else if (revealedCount < 6) {
                        revealedCount += 1
                    }
                    if (revealedCount == 6 && seed != null) {
                        onCast(buildCastResult(seed!!, allLines.toIntArray()))
                    }
                }
            )
        }
    }
}

@Composable
private fun InterpretationScreen(
    entry: HexagramEntry?,
    cast: CastResult?
) {
    if (entry == null || cast == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No cast available.",
                color = AppColors.TextPrimary,
                fontSize = 18.sp
            )
        }
        return
    }

    val hasChangingLines = cast.changingLines.isNotEmpty()
    var section by rememberSaveable(cast.seed) { mutableStateOf(Section.DESCRIPTION) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val buttons = buildSectionButtons(hasChangingLines)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "%02d %s".format(entry.kingWen, entry.name.trim()),
                        color = AppColors.TextPrimary,
                        fontSize = 22.sp
                    )
                    HexagramGlyph(
                        lineStates = lineStatesFromLines(cast.lines),
                        animate = false,
                        lineColor = AppColors.TextPrimary,
                        brokenColor = AppColors.TextSecondary,
                        uncastColor = AppColors.UncastLine,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionButtonsColumn(
                        buttons = buttons,
                        selected = section,
                        onSelect = { section = it }
                    )
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = sectionText(entry, cast, section),
                                color = AppColors.TextSecondary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = "%02d %s".format(entry.kingWen, entry.name.trim()),
                color = AppColors.TextPrimary,
                fontSize = 22.sp
            )
            HexagramGlyph(
                lineStates = lineStatesFromLines(cast.lines),
                animate = false,
                lineColor = AppColors.TextPrimary,
                brokenColor = AppColors.TextSecondary,
                uncastColor = AppColors.UncastLine,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.7f)
            )
            SectionGrid(
                buttons = buttons,
                columns = 2,
                selected = section,
                onSelect = { section = it },
                modifier = Modifier.fillMaxWidth()
            )
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = sectionText(entry, cast, section),
                        color = AppColors.TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun HexagramViewerScreen(repository: HexagramRepository) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val trigramOptions = remember { trigramOptions() }
    var upperIndex by rememberSaveable { mutableStateOf(0) }
    var lowerIndex by rememberSaveable { mutableStateOf(0) }
    var section by rememberSaveable(upperIndex, lowerIndex) { mutableStateOf(Section.DESCRIPTION) }

    val upper = trigramOptions[upperIndex]
    val lower = trigramOptions[lowerIndex]
    val bits = buildHexagramBits(lower.bits, upper.bits)
    val key = toKey(bits)
    val entry = repository.getByKeyPrimary(key)
    val lines = IntArray(6) { index -> if (bits[index] == 1) 7 else 8 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.45f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = entry?.let { "%02d %s".format(it.kingWen, it.name.trim()) } ?: "Hexagram",
                        color = AppColors.TextPrimary,
                        fontSize = 22.sp
                    )
                    HexagramGlyph(
                        lineStates = lineStatesFromLines(lines),
                        animate = false,
                        lineColor = AppColors.TextPrimary,
                        brokenColor = AppColors.TextSecondary,
                        uncastColor = AppColors.UncastLine,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TrigramSelectors(
                        upper = upper.name,
                        lower = lower.name,
                        onUpperChange = { upperIndex = it },
                        onLowerChange = { lowerIndex = it },
                        options = trigramOptions
                    )
                }
                Column(
                    modifier = Modifier.weight(0.55f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionButtonsColumn(
                        buttons = buildSectionButtons(false),
                        selected = section,
                        onSelect = { section = it }
                    )
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            val text = entry?.let { viewerSectionText(it, section) } ?: "No entry found."
                            Text(
                                text = text,
                                color = AppColors.TextSecondary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = entry?.let { "%02d %s".format(it.kingWen, it.name.trim()) } ?: "Hexagram",
                color = AppColors.TextPrimary,
                fontSize = 22.sp
            )
            HexagramGlyph(
                lineStates = lineStatesFromLines(lines),
                animate = false,
                lineColor = AppColors.TextPrimary,
                brokenColor = AppColors.TextSecondary,
                uncastColor = AppColors.UncastLine,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.7f)
            )
            TrigramSelectors(
                upper = upper.name,
                lower = lower.name,
                onUpperChange = { upperIndex = it },
                onLowerChange = { lowerIndex = it },
                options = trigramOptions
            )
            SectionGrid(
                buttons = buildSectionButtons(false),
                columns = 2,
                selected = section,
                onSelect = { section = it },
                modifier = Modifier.fillMaxWidth()
            )
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    val text = entry?.let { viewerSectionText(it, section) } ?: "No entry found."
                    Text(
                        text = text,
                        color = AppColors.TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun TrigramSelectors(
    upper: String,
    lower: String,
    onUpperChange: (Int) -> Unit,
    onLowerChange: (Int) -> Unit,
    options: List<TrigramOption>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TrigramSelector(
            label = "Upper",
            selected = upper,
            onSelectIndex = onUpperChange,
            options = options
        )
        TrigramSelector(
            label = "Lower",
            selected = lower,
            onSelectIndex = onLowerChange,
            options = options
        )
    }
}

@Composable
private fun TrigramSelector(
    label: String,
    selected: String,
    onSelectIndex: (Int) -> Unit,
    options: List<TrigramOption>
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = AppColors.TextSecondary,
            fontSize = 12.sp
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            AppButton(
                text = selected.uppercase(),
                onClick = { expanded = true },
                style = AppButtonStyle.FILLED,
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(AppColors.MenuBackground, RoundedCornerShape(12.dp))
                    .padding(6.dp)
                    .widthIn(min = AppDimens.MenuMinWidth)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.forEachIndexed { index, option ->
                        AppButton(
                            text = option.name,
                            onClick = {
                                expanded = false
                                onSelectIndex(index)
                            },
                            style = AppButtonStyle.MENU,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun AppButton(
    text: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    style: AppButtonStyle = AppButtonStyle.FILLED,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val background = when (style) {
        AppButtonStyle.FILLED -> if (selected) AppColors.ButtonSelected else AppColors.ButtonIdle
        AppButtonStyle.MENU -> Color.Transparent
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = AppColors.TextPrimary,
            disabledContainerColor = AppColors.ButtonDisabled,
            disabledContentColor = AppColors.TextSecondary
        ),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        contentPadding = PaddingValues(
            horizontal = AppDimens.ButtonHorizontalPadding,
            vertical = AppDimens.ButtonVerticalPadding
        ),
        modifier = modifier
            .defaultMinSize(minHeight = AppDimens.ButtonMinHeight)
            .heightIn(min = AppDimens.ButtonMinHeight)
    ) {
        Text(
            text = text,
            fontSize = AppDimens.ButtonTextSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
}

@Composable
private fun AppMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppDimens.ButtonMinHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = AppColors.TextPrimary,
            fontSize = AppDimens.ButtonTextSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
        Divider(
            color = AppColors.MenuDivider,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun HexagramGlyph(
    lineStates: List<LineState>,
    animate: Boolean,
    lineColor: Color,
    brokenColor: Color,
    uncastColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (i in 5 downTo 0) {
            val state = lineStates[i]
            HexagramLine(
                state = state,
                animate = animate,
                lineColor = lineColor,
                brokenColor = brokenColor,
                uncastColor = uncastColor
            )
        }
    }
}

@Composable
private fun HexagramLine(
    state: LineState,
    animate: Boolean,
    lineColor: Color,
    brokenColor: Color,
    uncastColor: Color
) {
    val height = 8.dp
    val gap = 18.dp
    val isCast = state != LineState.UNCAST
    val targetAlpha = if (isCast) 1f else 0.35f
    val alpha = if (animate) {
        animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = tween(durationMillis = 200),
            label = "lineAlpha"
        ).value
    } else {
        targetAlpha
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state == LineState.YANG) {
            Box(
                modifier = Modifier
                    .height(height)
                    .fillMaxWidth()
                    .background(lineColor.copy(alpha = alpha))
            )
        } else {
            val color = if (state == LineState.UNCAST) uncastColor else brokenColor
            Box(
                modifier = Modifier
                    .height(height)
                    .weight(1f)
                    .background(color.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(gap))
            Box(
                modifier = Modifier
                    .height(height)
                    .weight(1f)
                    .background(color.copy(alpha = alpha))
            )
        }
    }
}

private fun sectionText(entry: HexagramEntry, cast: CastResult, section: Section): String {
    return when (section) {
        Section.DESCRIPTION -> entry.description
        Section.JUDGMENT -> entry.judgment
        Section.IMAGE -> entry.image
        Section.CHANGING_LINES -> formatChangingLines(entry, cast)
    }
}

private fun formatChangingLines(entry: HexagramEntry, cast: CastResult): String {
    val indices = cast.changingLines
    if (indices.isEmpty()) {
        return "No changing lines for this cast."
    }
    val parts = ArrayList<String>()
    for (index in indices) {
        val lineText = entry.linesCommentary.getOrNull(index).orEmpty()
        parts.add("Line ${index + 1}\n$lineText")
    }
    return parts.joinToString("\n\n")
}

private fun buildCastResult(seed: Long, lines: IntArray): CastResult {
    val keyPrimary = toKey(primaryBits(lines))
    val keyChanged = toKey(changedBits(lines))
    val moving = changingLines(lines)
    return CastResult(
        seed = seed,
        lines = lines,
        keyPrimary = keyPrimary,
        keyChanged = keyChanged,
        changingLines = moving
    )
}

private fun buildSavedCast(seed: Long?, lines: List<Int>?): CastResult? {
    if (seed == null || lines == null || lines.size != 6) {
        return null
    }
    return buildCastResult(seed, lines.toIntArray())
}

private fun viewerSectionText(entry: HexagramEntry, section: Section): String {
    return when (section) {
        Section.DESCRIPTION -> entry.description
        Section.JUDGMENT -> entry.judgment
        Section.IMAGE -> entry.image
        Section.CHANGING_LINES -> entry.description
    }
}

private fun lineStatesFromLines(lines: IntArray): List<LineState> {
    return List(lines.size) { index ->
        val value = lines[index]
        if (value == 7 || value == 9) LineState.YANG else LineState.YIN
    }
}

private fun buildCastingStates(lines: List<Int>, revealedCount: Int): List<LineState> {
    val states = MutableList(6) { LineState.UNCAST }
    for (index in 0 until minOf(revealedCount, lines.size)) {
        val value = lines[index]
        states[index] = if (value == 7 || value == 9) LineState.YANG else LineState.YIN
    }
    return states
}

private fun buildSectionButtons(hasChangingLines: Boolean): List<Section> {
    val buttons = arrayListOf(Section.DESCRIPTION, Section.JUDGMENT, Section.IMAGE)
    if (hasChangingLines) {
        buttons.add(Section.CHANGING_LINES)
    }
    return buttons
}

@Composable
private fun SectionButtonsColumn(
    buttons: List<Section>,
    selected: Section,
    onSelect: (Section) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.GridSpacing)
    ) {
        for (section in buttons) {
            val label = when (section) {
                Section.DESCRIPTION -> "DESCRIPTION"
                Section.JUDGMENT -> "THE JUDGMENT"
                Section.IMAGE -> "THE IMAGE"
                Section.CHANGING_LINES -> "CHANGING LINES"
            }
            AppButton(
                text = label,
                selected = section == selected,
                onClick = { onSelect(section) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun trigramOptions(): List<TrigramOption> {
    return listOf(
        TrigramOption("Heaven", intArrayOf(1, 1, 1)),
        TrigramOption("Earth", intArrayOf(0, 0, 0)),
        TrigramOption("Thunder", intArrayOf(1, 0, 0)),
        TrigramOption("Water", intArrayOf(0, 1, 0)),
        TrigramOption("Lake", intArrayOf(1, 1, 0)),
        TrigramOption("Mountain", intArrayOf(0, 0, 1)),
        TrigramOption("Fire", intArrayOf(1, 0, 1)),
        TrigramOption("Wind", intArrayOf(0, 1, 1))
    )
}

private fun buildHexagramBits(lower: IntArray, upper: IntArray): IntArray {
    val bits = IntArray(6)
    for (i in 0 until 3) {
        bits[i] = lower[i]
        bits[i + 3] = upper[i]
    }
    return bits
}

@Composable
private fun SectionGrid(
    buttons: List<Section>,
    columns: Int,
    selected: Section,
    onSelect: (Section) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = if (columns <= 0) 0 else (buttons.size + columns - 1) / columns
    val gridHeight = if (rows == 0) 0.dp else {
        (AppDimens.ButtonMinHeight * rows) + (AppDimens.GridSpacing * (rows - 1))
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.height(gridHeight),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.GridSpacing),
        verticalArrangement = Arrangement.spacedBy(AppDimens.GridSpacing),
        userScrollEnabled = false
    ) {
        items(buttons) { section ->
            val label = when (section) {
                Section.DESCRIPTION -> "DESCRIPTION"
                Section.JUDGMENT -> "THE JUDGMENT"
                Section.IMAGE -> "THE IMAGE"
                Section.CHANGING_LINES -> "CHANGING LINES"
            }
            AppButton(
                text = label,
                selected = section == selected,
                onClick = { onSelect(section) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private object AppColors {
    val Background = Color(0xFF0F1114)
    val TextPrimary = Color(0xFFE6E8EB)
    val TextSecondary = Color(0xFFB9BDC2)
    val UncastLine = Color(0xFF6C7076)
    val ButtonIdle = Color(0xFF1D2025)
    val ButtonSelected = Color(0xFF2A2D33)
    val ButtonDisabled = Color(0xFF15181C)
    val MenuBackground = Color(0xFF14171B)
    val MenuButton = Color(0xFF1A1D21)
    val MenuDivider = Color(0xFF24272C)
}

private object AppDimens {
    val ButtonMinHeight = 48.dp
    val ButtonHorizontalPadding = 16.dp
    val ButtonVerticalPadding = 12.dp
    val ButtonCornerRadius = 10.dp
    val ButtonTextSize = 13.sp
    val GridSpacing = 10.dp
    val MenuMinWidth = 200.dp
}
