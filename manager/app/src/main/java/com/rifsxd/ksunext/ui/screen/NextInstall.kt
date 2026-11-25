package com.rifsxd.ksunext.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.getKernelVersion
import com.rifsxd.ksunext.ui.component.DialogHandle
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.component.rememberCustomDialog
import com.rifsxd.ksunext.ui.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextInstallScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    var installMethod by remember { mutableStateOf<InstallMethod?>(null) }
    var lkmSelection by remember { mutableStateOf<LkmSelection>(LkmSelection.KmiNone) }
    var partitionSelectionIndex by remember { mutableIntStateOf(0) }
    var partitionsState by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasCustomSelected by remember { mutableStateOf(false) }

    val cardBorder = rememberNextBorder()
    val onInstall = {
        installMethod?.let { method ->
            val isOta = method is InstallMethod.DirectInstallToInactiveSlot
            val partitionSelection = partitionsState.getOrNull(partitionSelectionIndex)
            val flashIt = FlashIt.FlashBoot(
                boot = if (method is InstallMethod.SelectFile) method.uri else null,
                lkm = lkmSelection,
                ota = isOta,
                partition = partitionSelection
            )
            navigator.navigate(FlashScreenDestination(flashIt))
        }
    }

    val currentKmi by produceState(initialValue = "") {
        value = withContext(Dispatchers.IO) { getCurrentKmi() }
    }

    val selectKmiDialog = rememberSelectKmiDialog { kmi ->
        kmi?.let {
            lkmSelection = LkmSelection.KmiString(it)
            onInstall()
        }
    }

    val onClickNext = {
        if (lkmSelection == LkmSelection.KmiNone && currentKmi.isBlank()) {
            selectKmiDialog.show()
        } else {
            onInstall()
        }
    }

    val selectLkmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val isKo = isKoFile(context, uri)
                if (isKo) {
                    lkmSelection = LkmSelection.LkmUri(uri)
                } else {
                    lkmSelection = LkmSelection.KmiNone
                    Toast.makeText(context, R.string.install_only_support_ko_file, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val onLkmUpload = {
        selectLkmLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/octet-stream"
        })
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            NextSimpleTopBar(
                title = stringResource(R.string.install),
                pillBorder = cardBorder,
                onBack = dropUnlessResumed { navigator.popBackStack() },
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
                .padding(top = 12.dp, bottom = 24.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NextSelectInstallMethod(
                border = cardBorder,
                onSelected = { method ->
                    installMethod = method
                    hasCustomSelected = false
                }
            )

            NextCard(
                cornerRadius = 20.dp,
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ADVANCED OPTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    (installMethod as? InstallMethod.DirectInstall)?.let {
                        val isInitBoot = currentKmi.contains("init_boot")
                        val defaultPartition = if (isInitBoot) "init_boot" else "boot"

                        LaunchedEffect(it) {
                            partitionsState = getDefaultPartition()
                            val defaultIndex = partitionsState.indexOf(defaultPartition)
                            if (defaultIndex >= 0) partitionSelectionIndex = defaultIndex
                        }

                        val displayPartitions = partitionsState.map { name ->
                            if (name == defaultPartition) "$name (default)" else name
                        }
                        val defaultIndex = partitionsState.indexOf(defaultPartition).coerceAtLeast(0)

                        NextChoosePartitionItem(
                            partitions = displayPartitions,
                            defaultIndex = defaultIndex,
                            onPartitionSelected = { index ->
                                partitionSelectionIndex = index
                                hasCustomSelected = true
                            }
                        )
                    }

                    NextLkmFileItem(
                        lkmSelection = lkmSelection,
                        onLkmUpload = onLkmUpload
                    )

                    val isOta = installMethod is InstallMethod.DirectInstallToInactiveSlot
                    val ksuVersion = if (Natives.isManager) Natives.version else null
                    val lkmMode = ksuVersion?.let { Natives.isLkmMode }
                    val isGki = getKernelVersion().isGKI()
                    val isButtonEnabled = installMethod != null && (!isOta || lkmMode == true || isGki)

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = isButtonEnabled,
                        onClick = { onClickNext() }
                    ) {
                        Text(
                            text = stringResource(id = R.string.install_next),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NextLkmFileItem(
    lkmSelection: LkmSelection,
    onLkmUpload: () -> Unit
) {
    val fileName = when (lkmSelection) {
        is LkmSelection.LkmUri -> {
            val uriStr = lkmSelection.uri.toString()
            uriStr.substringAfterLast('/')
        }
        else -> ""
    }

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onLkmUpload() },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        leadingContent = {
            Icon(Icons.Filled.FileUpload, contentDescription = null)
        },
        headlineContent = {
            Text(
                text = stringResource(id = R.string.install_upload_lkm_file),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            Text(
                text = if (fileName.isNotEmpty()) {
                    stringResource(id = R.string.install_selected_lkm_file, fileName)
                } else {
                    stringResource(id = R.string.install_upload_lkm_file_placeholder)
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextChoosePartitionItem(
    partitions: List<String>,
    defaultIndex: Int,
    onPartitionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(defaultIndex) }

    LaunchedEffect(defaultIndex) { selectedIndex = defaultIndex }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = partitions.getOrElse(selectedIndex) { "" },
            onValueChange = {},
            readOnly = true,
            shape = RoundedCornerShape(14.dp),
            label = { Text(stringResource(id = R.string.install_choose_partition)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            partitions.forEachIndexed { index, partition ->
                DropdownMenuItem(
                    text = { Text(partition) },
                    onClick = {
                        selectedIndex = index
                        expanded = false
                        onPartitionSelected(index)
                    }
                )
            }
        }
    }
}

@Composable
private fun NextSelectInstallMethod(
    border: BorderStroke?,
    onSelected: (InstallMethod) -> Unit = {}
) {
    val rootAvailable = rootAvailable()
    val isAbDevice = produceState(initialValue = false) {
        value = isAbDevice()
    }.value
    val defaultPartitionName = produceState(initialValue = "boot") {
        value = getDefaultPartition().firstOrNull() ?: "boot"
    }.value

    val radioOptions = mutableListOf<InstallMethod>(InstallMethod.SelectFile())
    if (rootAvailable) {
        radioOptions.add(InstallMethod.DirectInstall)
        if (isAbDevice) {
            radioOptions.add(InstallMethod.DirectInstallToInactiveSlot)
        }
    }

    var selectedOption by remember { mutableStateOf<InstallMethod?>(null) }
    var currentSelectingMethod by remember { mutableStateOf<InstallMethod?>(null) }

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val option = when (currentSelectingMethod) {
                    is InstallMethod.SelectFile -> InstallMethod.SelectFile(
                        uri,
                        summary = R.string.install_select_file_summary
                    )
                    else -> null
                }
                option?.let { opt ->
                    selectedOption = opt
                    onSelected(opt)
                }
            }
        }
    }

    val confirmDialog = rememberConfirmDialog(
        onConfirm = {
            selectedOption = InstallMethod.DirectInstallToInactiveSlot
            onSelected(InstallMethod.DirectInstallToInactiveSlot)
        },
        onDismiss = null
    )
    val dialogTitle = stringResource(id = android.R.string.dialog_alert_title)
    val dialogContent = stringResource(id = R.string.install_inactive_slot_warning)

    val onClick = { option: InstallMethod ->
        currentSelectingMethod = option
        when (option) {
            is InstallMethod.SelectFile -> {
                selectImageLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/octet-stream"
                })
            }
            is InstallMethod.DirectInstall -> {
                selectedOption = option
                onSelected(option)
            }
            is InstallMethod.DirectInstallToInactiveSlot -> {
                confirmDialog.showConfirm(dialogTitle, dialogContent)
            }
        }
    }

    NextCard(
        cornerRadius = 20.dp,
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "INSTALLATION METHOD",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.primary
            )

            radioOptions.forEach { option ->
                val interactionSource = remember { MutableInteractionSource() }
                val isSelected = option.javaClass == selectedOption?.javaClass
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .selectable(
                            selected = isSelected,
                            onClick = { onClick(option) },
                            role = Role.RadioButton,
                            indication = LocalIndication.current,
                            interactionSource = interactionSource
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            interactionSource = interactionSource
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = stringResource(id = option.label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            option.summary?.let {
                                Text(
                                    text = stringResource(id = it, defaultPartitionName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSelectKmiDialog(onSelected: (String?) -> Unit): DialogHandle {
    return rememberCustomDialog { dismiss ->
        val supportedKmi by produceState(initialValue = emptyList<String>()) {
            value = getSupportedKmis()
        }
        val options = supportedKmi.map { value ->
            ListOption(titleText = value)
        }

        var selection by remember { mutableStateOf<String?>(null) }
        ListDialog(
            state = rememberUseCaseState(
                visible = true,
                onFinishedRequest = { onSelected(selection) },
                onCloseRequest = { dismiss() }
            ),
            header = Header.Default(
                title = stringResource(R.string.select_kmi),
            ),
            selection = ListSelection.Single(
                showRadioButtons = true,
                options = options,
            ) { _, option ->
                selection = option.titleText
            }
        )
    }
}
*/