package com.android.ozjflash.ui.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.component.GlassBackButton
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.material.ExpressiveScaffold
import com.android.ozjflash.ui.component.material.TonalCard
import com.android.ozjflash.ui.component.material.TopBarBackButton
import com.android.ozjflash.ui.component.material.expressiveTopAppBarColors
import com.android.ozjflash.ui.navigation3.Navigator
import top.yukonga.miuix.kmp.icon.extended.Back

/** 关于页：GPU/Material/Miuix 三个主体各自不同的呈现 */
@Composable
fun AboutScreen(navigator: Navigator) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutMiuix(navigator)
        UiMode.Material -> AboutMaterial(navigator)
    }
}

@Composable
private fun aboutSections(): List<Pair<String, String>> = listOf(
    stringResource(R.string.about_app_name_title) to stringResource(R.string.about_app_name_value),
    stringResource(R.string.about_ui_title) to stringResource(R.string.about_ui_value),
    stringResource(R.string.about_tools_title) to stringResource(R.string.about_tools_value),
    stringResource(R.string.about_disclaimer_title) to stringResource(R.string.about_disclaimer_value),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutMaterial(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = { TopBarBackButton(onClick = { navigator.pop() }) },
                colors = TopAppBarDefaults.largeTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            aboutSections().forEach { (title, body) ->
                TonalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }
}

@Composable
fun AboutMiuix(navigator: Navigator) {
    top.yukonga.miuix.kmp.basic.Scaffold(
        topBar = {
            top.yukonga.miuix.kmp.basic.SmallTopAppBar(
                title = stringResource(R.string.about),
                navigationIcon = {
                    GlassBackButton(
                        imageVector = top.yukonga.miuix.kmp.icon.MiuixIcons.Back,
                        onClick = { navigator.pop() },
                        mirrorForRtl = true,
                    )
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            aboutSections().forEach { (title, body) ->
                top.yukonga.miuix.kmp.basic.Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        top.yukonga.miuix.kmp.basic.Text(title, fontSize = 14.sp)
                        top.yukonga.miuix.kmp.basic.Text(
                            body,
                            fontSize = 12.sp,
                            color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }
        }
    }
}
