package com.android.ozjflash.ui.screen.flash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.flash.FlashActions
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.screen.terminal.TerminalConsole

data class FlashAction(
    val label: String,
    val command: String,
)

class FlashState internal constructor() {
    var partition: String by mutableStateOf("boot")
        internal set
    var image: String by mutableStateOf("")
        internal set

    fun onPartition(value: String) { partition = value }
    fun onImage(value: String) { image = value }
}

@Composable
fun rememberFlashState(): FlashState = androidx.compose.runtime.remember { FlashState() }

@Composable
internal fun flashActions(state: FlashState): List<FlashAction> = listOf(
    FlashAction("fastboot devices", FlashActions.fastbootDevices()),
    FlashAction("getvar all", FlashActions.getvarAll()),
    FlashAction("reboot bootloader", FlashActions.rebootBootloader()),
    FlashAction("fastboot reboot-bootloader", FlashActions.rebootFastboot()),
    FlashAction("adb devices", FlashActions.adbDevices()),
    FlashAction("flash ${state.partition}", FlashActions.flashPartition(state.partition, state.image)),
    FlashAction("erase ${state.partition}", FlashActions.erasePartition(state.partition)),
)

@Composable
fun FlashPager(
    console: TerminalConsole,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val running by console.engine.running.collectAsStateWithLifecycle()
    val state = rememberFlashState()
    when (LocalUiMode.current) {
        UiMode.Miuix -> FlashMiuix(console, state, running, bottomInnerPadding, isCurrentPage)
        UiMode.Material -> FlashMaterial(console, state, running, bottomInnerPadding, isCurrentPage)
    }
}
