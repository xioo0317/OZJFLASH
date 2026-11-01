package com.android.ozjflash

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ozjflash.flash.NativeLibs
import com.android.ozjflash.terminal.TerminalEngine
import com.android.ozjflash.ui.LocalMainPagerState
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.bottombar.BottomBar
import com.android.ozjflash.ui.component.bottombar.MainPagerState
import com.android.ozjflash.ui.component.bottombar.NavigationBadgeState
import com.android.ozjflash.ui.component.bottombar.SideRail
import com.android.ozjflash.ui.component.bottombar.rememberMainPagerState
import com.android.ozjflash.ui.component.bottombar.useNavigationRail
import com.android.ozjflash.ui.navigation3.LocalNavigator
import com.android.ozjflash.ui.navigation3.Navigator
import com.android.ozjflash.ui.navigation3.Route
import com.android.ozjflash.ui.navigation3.rememberNavigator
import com.android.ozjflash.ui.screen.about.AboutScreen
import com.android.ozjflash.ui.screen.flash.FlashPager
import com.android.ozjflash.ui.screen.settings.SettingPager
import com.android.ozjflash.ui.screen.terminal.TerminalConsole
import com.android.ozjflash.ui.screen.terminal.TerminalPager
import com.android.ozjflash.ui.screen.terminal.rememberTerminalConsole
import com.android.ozjflash.ui.screen.tools.ToolsPager
import com.android.ozjflash.ui.screen.tools.rememberToolsState
import com.android.ozjflash.ui.theme.LocalColorMode
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.theme.LocalEnableFloatingBottomBar
import com.android.ozjflash.ui.theme.LocalEnableFloatingBottomBarBlur
import com.android.ozjflash.ui.theme.LocalEnableNavigationBadge
import com.android.ozjflash.ui.theme.OzjFlashTheme
import com.android.ozjflash.ui.util.rememberBlurBackdrop
import com.android.ozjflash.ui.viewmodel.AppViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec

/** 主界面分页数量：终端 / 刷写 / 工具箱 / 设置 */
private const val MAIN_PAGE_COUNT = 4

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 释放内置刷写工具（adb / fastboot / bash...）到 files/bin 并加载 JNI 桥接库
        val binPath = NativeLibs.prepare(this)
        NativeLibs.loadJni()

        setContent {
            val viewModel = viewModel<AppViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val appSettings = uiState.appSettings
            val uiMode = uiState.uiMode
            val darkMode = appSettings.colorMode.isDark ||
                (appSettings.colorMode.isSystem && isSystemInDarkTheme())

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            val navigator = rememberNavigator(Route.Main)
            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale) {
                Density(systemDensity.density * uiState.pageScale, systemDensity.fontScale)
            }

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalDensity provides density,
                LocalColorMode provides appSettings.colorMode.value,
                LocalEnableBlur provides uiState.enableBlur,
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides uiState.enableFloatingBottomBarBlur,
                LocalEnableNavigationBadge provides uiState.enableNavigationBadge,
                LocalUiMode provides uiMode,
            ) {
                OzjFlashTheme(appSettings = appSettings, uiMode = uiMode) {
                    val mainScreen = @Composable {
                        MainScreen(
                            viewModel = viewModel,
                            binPath = binPath,
                            initialPage = uiState.selectedMainPage,
                            onPageChanged = viewModel::setSelectedMainPage,
                        )
                    }
                    val navDisplay = @Composable {
                        NavDisplay(
                            backStack = navigator.backStack,
                            effects = NavDisplayEffects(cornerClipRadius = rememberNavSystemCornerRadius()),
                            onBack = { navigator.pop() }
                        ) {
                            entry<Route.Main> { mainScreen() }
                            entry<Route.Terminal> { mainScreen() }
                            entry<Route.Flash> { mainScreen() }
                            entry<Route.Tools> { mainScreen() }
                            entry<Route.Settings> { mainScreen() }
                            entry<Route.About> { AboutScreen(navigator) }
                        }
                    }
                    when (uiMode) {
                        UiMode.Material -> androidx.compose.material3.Scaffold(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) { navDisplay() }

                        UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Scaffold { navDisplay() }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    viewModel: AppViewModel,
    binPath: String,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
) {
    val navController = LocalNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val enableBlur = uiState.enableBlur
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val useNavigationRail = useNavigationRail(enableFloatingBottomBar)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { MAIN_PAGE_COUNT })
    val mainPagerState = rememberMainPagerState(
        pagerState = pagerState,
        animatePageChanges = !useNavigationRail,
    )

    val scope = rememberCoroutineScope()
    val shellPath = remember(binPath) { NativeLibs.shellPath(binPath) }
    val console = remember {
        TerminalEngine.USE_TERMUX_PTY = uiState.terminalPty
        TerminalEngine(scope, shellPath, NativeLibs.pathEnv(binPath))
    }.let { rememberTerminalConsole(it, binPath) }

    val toolsState = rememberToolsState(binPath)
    val navigationBadge = NavigationBadgeState(toolsMissingCount = toolsState.missingCount)

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) { onPageChanged(settledPage) }
    LaunchedEffect(mainPagerState.pagerState.currentPage) { mainPagerState.syncPage() }

    val uiMode = LocalUiMode.current
    val surfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surface
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
    }
    val blurBackdrop = rememberBlurBackdrop(enableBlur)
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    CompositionLocalProvider(LocalMainPagerState provides mainPagerState) {
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            Box(modifier = if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier) {
                HorizontalPager(
                    state = mainPagerState.pagerState,
                    beyondViewportPageCount = 1,
                    overscrollEffect = null,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = mainPagerState.pagerState,
                        snapAnimationSpec = PagerNavigationSpringSpec,
                    ),
                ) { page ->
                    when (page) {
                        0 -> TerminalPager(console, bottomInnerPadding, true)
                        1 -> FlashPager(console, bottomInnerPadding, true)
                        2 -> ToolsPager(console, binPath, bottomInnerPadding, true)
                        3 -> SettingPager(viewModel, navController, bottomInnerPadding, true)
                    }
                }
            }
        }

        if (useNavigationRail) {
            val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Start)
            val navBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
            Row {
                SideRail(navigationBadge)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .consumeWindowInsets(startInsets)
                ) {
                    pagerContent(navBarBottomPadding)
                }
            }
        } else {
            val bottomBar = @Composable {
                Box(modifier = Modifier.fillMaxWidth()) {
                    BottomBar(
                        blurBackdrop = blurBackdrop,
                        backdrop = backdrop,
                        navigationBadge = navigationBadge,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    bottomBar = bottomBar,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }

                UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Scaffold(
                    bottomBar = bottomBar
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
    }
}
