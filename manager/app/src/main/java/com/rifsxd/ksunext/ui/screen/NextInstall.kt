package com.rifsxd.ksunext.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.getKernelVersion
import com.rifsxd.ksunext.isKernel510
import com.rifsxd.ksunext.ui.component.SwitchItem
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.util.LkmSelection
import com.rifsxd.ksunext.ui.util.ZipUtils
import com.rifsxd.ksunext.ui.util.getCurrentKmi
import com.rifsxd.ksunext.ui.util.isAbDevice
import com.rifsxd.ksunext.ui.util.rootAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextInstallScreen(navigator: DestinationsNavigator) {
    var installMethod by remember { mutableStateOf<InstallMethod?>(null) }
    var lkmSelection by remember { mutableStateOf<LkmSelection>(LkmSelection.KmiNone) }
    var advancedOptionsShown by rememberSaveable { mutableStateOf(false) }
    var allowShell by rememberSaveable { mutableStateOf(false) }
    var enableAdb by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val cardBorder = rememberNextBorder()

    val onInstall = {
        installMethod?.let { method ->
            if (Natives.isLateLoadMode && method !is InstallMethod.SelectFile) {
                return@let
            }

            if (method is InstallMethod.AnyKernel) {
                method.uri?.let {
                    navigator.navigate(
                        FlashScreenDestination(FlashIt.FlashAnyKernel(it))
                    )
                }
                return@let
            }

            val flashIt = FlashIt.FlashBoot(
                boot = if (method is InstallMethod.SelectFile) method.uri else null,
                lkm = lkmSelection,
                ota = method is InstallMethod.DirectInstallToInactiveSlot,
                allowShell = allowShell,
                enableAdb = enableAdb
            )
            navigator.navigate(FlashScreenDestination(flashIt))
        }
    }

    val currentKmi by produceState(initialValue = "") {
        value = runCatching { getCurrentKmi().trim() }.getOrDefault("")
    }

    val selectKmiDialog = rememberSelectKmiDialog { kmi ->
        kmi?.let {
            lkmSelection = LkmSelection.KmiString(it)
            onInstall()
        }
    }

    val onClickNext = {
        when (installMethod) {
            is InstallMethod.AnyKernel -> onInstall()
            else -> {
                if (lkmSelection == LkmSelection.KmiNone && currentKmi.isBlank()) {
                    selectKmiDialog.show()
                } else {
                    onInstall()
                }
            }
        }
    }

    val selectLkmLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.data?.let { uri ->
                    lkmSelection = LkmSelection.LkmUri(uri)
                }
            }
        }

    val onLkmUpload = {
        selectLkmLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/octet-stream"
        })
    }

    val kernelVersion = getKernelVersion()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            NextInstallTopBar(
                title = stringResource(R.string.install),
                pillBorder = cardBorder,
                onBack = dropUnlessResumed { navigator.popBackStack() },
                actions = {
                    if (kernelVersion.isGKI()) {
                        IconButton(
                            onClick = onLkmUpload,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NextCard(
                cornerRadius = 22.dp,
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                NextSelectInstallMethod(
                    selectedMethod = installMethod,
                    onFlashModulesRequested = { uri ->
                        navigator.navigate(FlashScreenDestination(FlashIt.FlashModules(listOf(uri))))
                    },
                    onSelected = { method -> installMethod = method }
                )
            }

            val rotationState by animateFloatAsState(
                targetValue = if (advancedOptionsShown) 180f else 0f,
                label = "NextInstallAdvRotation"
            )

            NextCard(
                cornerRadius = 22.dp,
                border = cardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { advancedOptionsShown = !advancedOptionsShown }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.advanced_options),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = stringResource(R.string.expand),
                            modifier = Modifier.graphicsLayer { rotationZ = rotationState }
                        )
                    }

                    AnimatedVisibility(
                        visible = advancedOptionsShown,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            SwitchItem(
                                title = stringResource(id = R.string.allow_shell),
                                summary = stringResource(id = R.string.allow_shell_summary),
                                checked = allowShell,
                                onCheckedChange = { allowShell = it }
                            )
                            SwitchItem(
                                title = stringResource(id = R.string.enable_adb),
                                summary = stringResource(id = R.string.enable_adb_summary),
                                checked = enableAdb,
                                onCheckedChange = { enableAdb = it }
                            )
                        }
                    }
                }
            }

            (lkmSelection as? LkmSelection.LkmUri)?.let {
                NextCard(
                    cornerRadius = 16.dp,
                    border = cardBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.selected_lkm,
                            it.uri.lastPathSegment ?: "(file)"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = installMethod != null,
                onClick = { onClickNext() }
            ) {
                Text(
                    text = stringResource(id = R.string.install_next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
        }
    }
}

@Composable
private fun NextSelectInstallMethod(
    selectedMethod: InstallMethod?,
    onFlashModulesRequested: (Uri) -> Unit = {},
    onSelected: (InstallMethod) -> Unit = {}
) {
    val context = LocalContext.current
    val rootAvailable = rootAvailable()
    val isAbDevice = produceState(initialValue = false) {
        value = isAbDevice()
    }.value
    val kernelVersion = getKernelVersion()
    val selectFileTip = stringResource(
        id = R.string.select_file_tip,
        if (kernelVersion.isKernel510()) "boot" else "init_boot/vendor_boot"
    )
    val radioOptions = mutableListOf<InstallMethod>()

    radioOptions.add(InstallMethod.SelectFile(summary = selectFileTip))

    if (rootAvailable) {
        if (!Natives.isLateLoadMode) {
            if (kernelVersion.isGKI()) {
                radioOptions.add(InstallMethod.DirectInstall)
                if (isAbDevice) {
                    radioOptions.add(InstallMethod.DirectInstallToInactiveSlot)
                }
            }
            radioOptions.add(InstallMethod.AnyKernel())
        }
    }

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val option = InstallMethod.SelectFile(uri, summary = selectFileTip)
                onSelected(option)
            }
        }
    }

    val selectAnyKernelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                if (ZipUtils.isAnyKernel3Zip(context, uri)) {
                    onSelected(InstallMethod.AnyKernel(uri))
                } else {
                    onFlashModulesRequested(uri)
                }
            }
        }
    }

    val confirmDialog = rememberConfirmDialog(
        onConfirm = { onSelected(InstallMethod.DirectInstallToInactiveSlot) },
        onDismiss = null
    )
    val dialogTitle = stringResource(id = android.R.string.dialog_alert_title)
    val dialogContent = stringResource(id = R.string.install_inactive_slot_warning)

    val onClick = { option: InstallMethod ->
        when (option) {
            is InstallMethod.SelectFile -> {
                selectImageLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/octet-stream"
                })
            }
            is InstallMethod.AnyKernel -> {
                selectAnyKernelLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/zip"
                    putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
                    )
                    addCategory(Intent.CATEGORY_OPENABLE)
                })
            }
            is InstallMethod.DirectInstall -> onSelected(option)
            is InstallMethod.DirectInstallToInactiveSlot -> confirmDialog.showConfirm(dialogTitle, dialogContent)
        }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        radioOptions.forEach { option ->
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = option.javaClass == selectedMethod?.javaClass,
                        onValueChange = { onClick(option) },
                        role = Role.RadioButton,
                        indication = LocalIndication.current,
                        interactionSource = interactionSource
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                RadioButton(
                    selected = option.javaClass == selectedMethod?.javaClass,
                    onClick = { onClick(option) },
                    interactionSource = interactionSource
                )
                Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)) {
                    Text(
                        text = stringResource(id = option.label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    option.summary?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextInstallTopBar(
    title: String,
    pillBorder: BorderStroke?,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        },
        navigationIcon = {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = pillBorder,
                modifier = Modifier.padding(start = 8.dp).size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        actions = actions,
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}
