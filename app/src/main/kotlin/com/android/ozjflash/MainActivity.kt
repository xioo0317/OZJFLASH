package com.android.ozjflash

import android.content.Context
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.android.ozjflash.CrashLogger
import com.android.ozjflash.flash.NativeLibs
import com.android.ozjflash.flash.UsbHost
import com.android.ozjflash.terminal.TerminalEngine
import com.android.ozjflash.terminal.rememberRealTerminal
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
import com.android.ozjflash.ui.screen.colorpalette.ColorPaletteScreen
import com.android.ozjflash.ui.screen.home.HomePager
import com.android.ozjflash.ui.screen.settings.SettingPager
import com.android.ozjflash.ui.screen.terminal.TerminalConsole
import com.android.ozjflash.ui.screen.terminal.TerminalPager
import com.android.ozjflash.ui.screen.terminal.rememberTerminalConsole
import com.android.ozjflash.ui.screen.tools.ToolsPager
import com.android.ozjflash.ui.screen.tools.ToolKind
import com.android.ozjflash.ui.screen.tools.ToolTerminalScreen
import com.android.ozjflash.flash.FlashScreen
import com.android.ozjflash.ui.screen.tools.rememberToolsState
import com.android.ozjflash.ui.theme.LocalColorMode
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.theme.LocalEnableFloatingBottomBar
import com.android.ozjflash.ui.theme.LocalEnableFloatingBottomBarBlur
import com.android.ozjflash.ui.theme.LocalEnableNavigationBadge
import com.android.ozjflash.ui.theme.OzjFlashTheme
import com.android.ozjflash.ui.util.rememberBlurBackdrop
import com.android.ozjflash.ui.viewmodel.AppViewModel
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
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

    private fun registerUsbReceiver() {
        val filter = UsbHost.intentFilter()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(UsbHost.receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(UsbHost.receiver, filter)
        }
        UsbHost.refresh(this)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(UsbHost.receiver) }
        super.onDestroy()
    }

    override fun attachBaseContext(newBase: Context) {
        // Activity 必须单独包一层 locale：Application 层的 createConfigurationContext
        // 不会传导到 Activity 的 Resources，导致切换语言后页面仍是系统语言
        val prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "system") ?: "system"
        super.attachBaseContext(newBase.wrapWithLanguage(lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // USB 授权是异步广播回调：注册后 UsbManager.requestPermission() 的系统弹框
        // 结果才能回到应用，工具页据此更新「已授权 / 未授权」
        registerUsbReceiver()

        // 建立内置刷写工具（adb / fastboot / bash...）的可执行链接并加载 JNI 桥接库
        // nativeLibraryDir 里的 lib*.so 才是真正可执行的本体（Android 10+ 禁止 exec 应用私有目录）
        val nativeDir = applicationInfo.nativeLibraryDir
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
                // 该属性自 API 29 起才有，低版本直接访问会 NoSuchMethodError
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
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
                            nativeDir = nativeDir,
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
                            entry<Route.About> { AboutScreen(navigator) }
                            entry<Route.ColorPalette> { ColorPaletteScreen(viewModel, navigator) }
                            entry<Route.Fastboot> { ToolTerminalScreen(navigator, ToolKind.FASTBOOT) }
                            entry<Route.Adb> { ToolTerminalScreen(navigator, ToolKind.ADB) }
                            entry<Route.Flash> { FlashScreen() }
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
    nativeDir: String?,
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
    val engine = remember {
        TerminalEngine(scope, shellPath, NativeLibs.pathEnv(binPath, nativeDir))
    }
    val console = rememberTerminalConsole(engine, binPath)

    // 终端页：Termux 上游真终端（libtermux.so PTY + ANSI 渲染）。
    // 主界面期间保持同一会话，随主界面销毁才回收子进程。
    val realTerminal = rememberRealTerminal(shellPath, binPath, nativeDir)
    DisposableEffect(realTerminal) {
        onDispose { realTerminal.session.finishIfRunning() }
    }

    // 启动横幅：把工具链探测结果与上次崩溃摘要直接打进终端，
    // 用户不用翻 logcat 就能看出是工具没链接上、不可执行，还是别的问题
    LaunchedEffect(console) {
        withContext(Dispatchers.IO) {
            val missing = NativeLibs.missingTools(binPath)
            val probe = NativeLibs.probeExec(binPath)
            val crash = CrashLogger.read(ozjApp)
            val banner = buildString {
                appendLine("\r\n[OZJFLASH] 工具目录   : $binPath")
                appendLine("[OZJFLASH] 原生库目录 : ${ozjApp.applicationInfo.nativeLibraryDir}")
                appendLine("[OZJFLASH] fastboot   : $probe")
                if (missing.isEmpty()) {
                    appendLine("[OZJFLASH] 工具链接就绪，可直接执行 fastboot / adb")
                } else {
                    appendLine("[OZJFLASH] 缺失工具   : ${missing.joinToString()}")
                }
                if (crash != null) {
                    appendLine("\r\n[OZJFLASH] 上次崩溃记录（已读取并清空，完整内容见 files/crash.log）：")
                    crash.lineSequence().take(24).forEach { appendLine(it) }
                }
            }
            console.append(banner)
            if (crash != null) CrashLogger.clear(ozjApp)
        }
    }

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
    // miuix-blur 依赖 android.graphics.RuntimeShader（API 33 才有），
    // 低版本上一旦触碰就会 NoClassDefFoundError 直接闪退，因此整条链路按版本降级
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val blurBackdrop = if (blurSupported) rememberBlurBackdrop(enableBlur) else null
    val backdrop: LayerBackdrop? = if (blurSupported) {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    } else null

    CompositionLocalProvider(LocalMainPagerState provides mainPagerState) {
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            Box(modifier = if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier) {
                HorizontalPager(
                    // 悬浮底栏的液态玻璃以页面内容为模糊源：与 KernelSU 一致，
                    // 只有同时开启「悬浮底栏」+「液态玻璃」时才把 pager 注册成 backdrop，
                    // 否则 backdrop 抓到空层，底栏会糊成一片黑
                    modifier = if (backdrop != null && enableFloatingBottomBar && LocalEnableFloatingBottomBarBlur.current) {
                        Modifier.layerBackdrop(backdrop)
                    } else Modifier,
                    state = mainPagerState.pagerState,
                    // 首次打开应用后切换导航栏会卡：HorizontalPager 默认只预组合相邻 1 页，
                    // 首次切到未组合的页（终端的真终端视图 / 设置页的模糊 shader）会在主线程一次性
                    // 组合，丢帧卡顿。这里把所有页在启动期一次性预组合，切换时只是重组已有子树，
                    // 与 KernelSU Manager 的预加载思路一致。代价是启动稍重，换取切换顺滑。
                    beyondViewportPageCount = MAIN_PAGE_COUNT - 1,
                    overscrollEffect = null,
                    // 主题设置里的「允许左右滑动切换页面」：关闭后只能用底栏/侧栏切换
                    userScrollEnabled = uiState.enablePagerSwipe,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = mainPagerState.pagerState,
                        snapAnimationSpec = PagerNavigationSpringSpec,
                    ),
                ) { page ->
                    when (page) {
                        0 -> HomePager(binPath, bottomInnerPadding, true)
                        1 -> ToolsPager(console, binPath, bottomInnerPadding, true)
                        2 -> TerminalPager(realTerminal, bottomInnerPadding, true)
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
