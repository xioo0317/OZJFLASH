package com.android.ozjflash.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.ozjflash.R
import com.android.ozjflash.ui.component.material.ExpressiveScaffold
import com.android.ozjflash.ui.component.material.SegmentedColumn
import com.android.ozjflash.ui.component.material.SegmentedListItem
import com.android.ozjflash.ui.component.material.TonalCard
import com.android.ozjflash.ui.component.material.expressiveTopAppBarColors

/**
 * Material3 Expressive 主体的主页（跟进 KernelSU HomePagerMaterial 的写法：
 * ExpressiveScaffold + LargeFlexibleTopAppBar + SegmentedColumn / TonalCard）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePagerMaterial(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            StatusCardMaterial(state)
            InfoCardMaterial(state)
            SupportLinksMaterial(actions)
            Spacer(modifier = Modifier.height(bottomInnerPadding))
        }
    }
}

@Composable
private fun StatusCardMaterial(state: HomeUiState) {
    val ready = state.toolsReady
    val containerColor = if (ready) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = containerColor,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (ready) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.contentColorFor(containerColor),
                modifier = Modifier.size(40.dp),
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = stringResource(
                        if (ready) R.string.home_status_title_ready else R.string.home_status_title_missing,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.contentColorFor(containerColor),
                )
                Text(
                    text = stringResource(
                        if (ready) R.string.home_status_ready else R.string.home_status_missing,
                        state.toolsMissing.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.contentColorFor(containerColor),
                )
                if (!ready) {
                    Text(
                        text = state.toolsMissing.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.contentColorFor(containerColor),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCardMaterial(state: HomeUiState) {
    SegmentedColumn(
        modifier = Modifier.fillMaxWidth(),
        content = listOf(
            {
                InfoRow(
                    icon = Icons.Filled.Android,
                    title = stringResource(R.string.home_app_version),
                    content = state.appVersion,
                )
            },
            {
                InfoRow(
                    icon = Icons.Filled.Android,
                    title = stringResource(R.string.home_android_version),
                    content = "${state.androidVersion} (API ${state.sdk})",
                )
            },
            {
                InfoRow(
                    icon = Icons.Filled.Smartphone,
                    title = stringResource(R.string.home_device_model),
                    content = state.deviceModel,
                )
            },
            {
                InfoRow(
                    icon = Icons.Filled.Tag,
                    title = stringResource(R.string.home_abi),
                    content = state.abi,
                )
            },
            {
                InfoRow(
                    icon = Icons.Filled.Fingerprint,
                    title = stringResource(R.string.home_fingerprint),
                    content = state.fingerprint,
                )
            },
            {
                InfoRow(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.home_selinux_status),
                    content = state.selinuxStatus,
                )
            },
        ),
    )
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, content: String) {
    ListItem(
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        leadingContent = {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(content, style = MaterialTheme.typography.bodySmall) },
    )
}

@Composable
private fun SupportLinksMaterial(actions: HomeActions) {
    val githubUrl = stringResource(R.string.home_github_url)
    val learnUrl = stringResource(R.string.home_learn_url)
    SegmentedColumn(
        modifier = Modifier.fillMaxWidth(),
        content = listOf(
            {
                SegmentedListItem(
                    onClick = { actions.onOpenUrl(githubUrl) },
                    headlineContent = { Text(stringResource(R.string.home_support_title)) },
                    supportingContent = { Text(stringResource(R.string.home_support_content)) },
                    leadingContent = { Icon(Icons.Rounded.Star, stringResource(R.string.home_support_title)) },
                )
            },
            {
                SegmentedListItem(
                    onClick = { actions.onOpenUrl(learnUrl) },
                    headlineContent = { Text(stringResource(R.string.home_learn_title)) },
                    supportingContent = { Text(stringResource(R.string.home_learn_content)) },
                    leadingContent = {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, stringResource(R.string.home_learn_title))
                    },
                )
            },
        ),
    )
}
