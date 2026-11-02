package com.android.ozjflash.ui.screen.colorpalette

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.ozjflash.R
import com.android.ozjflash.ui.component.bottombar.useNavigationRail
import com.android.ozjflash.ui.component.material.ExpressiveScaffold
import com.android.ozjflash.ui.component.material.ExpressiveToggleButton
import com.android.ozjflash.ui.component.material.SegmentedColumn
import com.android.ozjflash.ui.component.material.SegmentedDropdownItem
import com.android.ozjflash.ui.component.material.SegmentedSwitchItem
import com.android.ozjflash.ui.component.material.TonalCard
import com.android.ozjflash.ui.component.material.TopBarBackButton
import com.android.ozjflash.ui.component.material.expressiveTopAppBarColors
import com.android.ozjflash.ui.theme.ColorMode
import com.android.ozjflash.ui.theme.rememberOzjFlashColorScheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/** Material3 Expressive 主体的主题设置页（照搬 KernelSU ColorPaletteScreenMaterial） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteScreenMaterial(
    state: ColorPaletteUiState,
    actions: ColorPaletteActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState = state.uiState
    val currentColorMode = state.currentColorMode
    val currentKeyColor = uiState.appSettings.keyColor
    val colorStyle = uiState.appSettings.paletteStyle
    val colorSpec = uiState.appSettings.colorSpec
    val haptic = LocalHapticFeedback.current

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = { TopBarBackButton(onClick = actions::back) },
                title = { Text(stringResource(R.string.settings_theme)) },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { paddingValues ->
        val navBars = WindowInsets.navigationBars.asPaddingValues()
        val captionBar = WindowInsets.captionBar.asPaddingValues()
        val isDark = currentColorMode.isDark || (currentColorMode.isSystem && isSystemInDarkTheme())
        val isAmoled = currentColorMode.isAmoled
        val seedColor = if (currentKeyColor == 0) Color.Unspecified else Color(currentKeyColor)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                ThemePreviewCardMaterial(
                    keyColor = currentKeyColor,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    enableFloatingBottomBar = uiState.enableFloatingBottomBar,
                    enableFloatingBottomBarBlur = uiState.enableFloatingBottomBarBlur,
                    paletteStyle = colorStyle,
                    colorSpec = colorSpec,
                )
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        ColorButtonMaterial(
                            color = Color.Unspecified,
                            isSelected = currentKeyColor == 0,
                            isDark = isDark,
                            isAmoled = isAmoled,
                            paletteStyle = colorStyle,
                            colorSpec = colorSpec,
                            onClick = { actions.setKeyColor(0) },
                        )
                    }
                    items(paletteColorEntries.drop(1)) { (value, _) ->
                        ColorButtonMaterial(
                            color = Color(value),
                            isSelected = currentKeyColor == value,
                            isDark = isDark,
                            isAmoled = isAmoled,
                            paletteStyle = colorStyle,
                            colorSpec = colorSpec,
                            onClick = { actions.setKeyColor(value) },
                        )
                    }
                }
            }

            item {
                val options = listOf(
                    ColorMode.SYSTEM to stringResource(R.string.settings_theme_mode_system),
                    ColorMode.LIGHT to stringResource(R.string.settings_theme_mode_light),
                    ColorMode.DARK to stringResource(R.string.settings_theme_mode_dark),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        options.forEachIndexed { index, (mode, label) ->
                            ExpressiveToggleButton(
                                checked = currentColorMode.toPaletteIndex() == index,
                                onCheckedChange = {
                                    if (it) {
                                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        actions.setColorModeIndex(index)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        ColorMode.LIGHT -> Icons.Filled.Brightness7
                                        ColorMode.DARK -> Icons.Filled.Brightness3
                                        ColorMode.DARK_AMOLED -> Icons.Filled.Brightness1
                                        else -> Icons.Filled.Brightness4
                                    },
                                    contentDescription = label,
                                )
                            }
                        }
                    }
                }
            }

            item {
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf(
                        {
                            val styles = PaletteStyle.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Style,
                                title = stringResource(R.string.settings_color_style),
                                items = styles.map { it.name },
                                selectedIndex = styles.indexOf(colorStyle).coerceAtLeast(0),
                                onItemSelected = { index -> actions.setPaletteStyle(styles[index].name) },
                            )
                        },
                        {
                            val specs = ColorSpec.SpecVersion.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.DesignServices,
                                title = stringResource(R.string.settings_color_spec),
                                items = specs.map { it.name },
                                selectedIndex = specs.indexOf(colorSpec).coerceAtLeast(0),
                                onItemSelected = { index -> actions.setColorSpec(specs[index].name) },
                            )
                        },
                        {
                            val colorItems = paletteColorEntries.map { stringResource(it.second) }
                            val colorValues = paletteColorEntries.map { it.first }
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Colorize,
                                title = stringResource(R.string.settings_key_color),
                                items = colorItems,
                                selectedIndex = colorValues.indexOf(currentKeyColor).coerceAtLeast(0),
                                onItemSelected = { index -> actions.setKeyColor(colorValues[index]) },
                            )
                        },
                    ),
                )
            }

            item {
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.CallToAction,
                                title = stringResource(R.string.settings_floating_bottom_bar),
                                summary = stringResource(R.string.settings_floating_bottom_bar_summary),
                                checked = uiState.enableFloatingBottomBar,
                                onCheckedChange = actions::setFloatingBottomBar,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.WaterDrop,
                                title = stringResource(R.string.settings_enable_glass),
                                summary = stringResource(R.string.settings_enable_glass_summary),
                                enabled = uiState.enableFloatingBottomBar,
                                checked = uiState.enableFloatingBottomBarBlur,
                                onCheckedChange = actions::setFloatingBottomBarBlur,
                            )
                        },
                    ),
                )
            }

            item {
                TonalCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val sliderState = rememberSliderState(
                        value = uiState.pageScale,
                        trackRange = 0.8f..1.1f,
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_page_scale),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.settings_page_scale_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${(sliderState.value * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Slider(
                            state = sliderState,
                            onValueChangeFinished = { actions.setPageScale(sliderState.value) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(
                        16.dp + navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()
                    )
                )
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCardMaterial(
    keyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    enableFloatingBottomBar: Boolean,
    enableFloatingBottomBarBlur: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
    val useRail = useNavigationRail(enableFloatingBottomBar)

    val previewScheme = rememberOzjFlashColorScheme(
        seedColor = if (keyColor == 0) Color.Unspecified else Color(keyColor),
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio),
            color = previewScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, color = previewScheme.outlineVariant),
        ) {
            val content: @Composable ColumnScope.() -> Unit = {
                Box(
                    modifier = Modifier
                        .height(if (useRail) 36.dp else 48.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, top = if (useRail) 8.dp else 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = previewScheme.onSurface,
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val showInfoCard = maxHeight >= 72.dp
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TonalCard(
                            containerColor = previewScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            content = { },
                        )
                        if (showInfoCard) {
                            TonalCard(
                                containerColor = previewScheme.surfaceBright,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                content = { },
                            )
                        }
                    }
                }
            }

            if (useRail) {
                Row {
                    Surface(
                        color = previewScheme.surfaceContainer,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(36.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Filled.Home, null, tint = previewScheme.primary)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) { content() }
                }
            } else {
                Box {
                    Column { content() }

                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    ) {
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (enableFloatingBottomBar) {
                                Row(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (enableFloatingBottomBarBlur)
                                                previewScheme.surfaceContainer.copy(alpha = 0.4f)
                                            else previewScheme.surfaceContainer
                                        )
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    repeat(4) {
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (it == 0) previewScheme.primary
                                                    else previewScheme.onSurface
                                                ),
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .fillMaxWidth()
                                        .background(previewScheme.surfaceContainer)
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.Home, null, tint = previewScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorButtonMaterial(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val previewScheme = rememberOzjFlashColorScheme(
        seedColor = color,
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = previewScheme.surfaceContainer,
        modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = previewScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
                drawArc(
                    color = previewScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f)
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, previewScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(previewScheme.primary, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = previewScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp),
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(previewScheme.primary, CircleShape),
                    )
                }
            }
        }
    }
}
