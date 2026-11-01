package com.android.ozjflash.ui.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.navigation3.Navigator

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
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.about)) },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            aboutSections().forEach { (title, body) ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(body, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
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
                    top.yukonga.miuix.kmp.basic.IconButton(onClick = { navigator.pop() }) {
                        top.yukonga.miuix.kmp.basic.Icon(
                            imageVector = top.yukonga.miuix.kmp.icon.MiuixIcons.Back,
                            contentDescription = null,
                            tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground
                        )
                    }
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
