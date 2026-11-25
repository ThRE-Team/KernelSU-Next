package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.system.OsConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.*
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.LocalNavBarEnabled
import com.rifsxd.ksunext.ui.LocalScrollState
import com.rifsxd.ksunext.ui.component.*
import com.rifsxd.ksunext.ui.rememberScrollConnection
import com.rifsxd.ksunext.ui.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextSettingScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current
    val cardBorder = rememberNextBorder()

    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    val scrollState = LocalScrollState.current
    val navBarEnabled = LocalNavBarEnabled.current
    val isNavBarHidden = (scrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
    val navBarPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp

    val bottomBarScrollState = LocalScrollState.current
    val bottomBarScrollConnection = if (bottomBarScrollState != null) {
        rememberScrollConnection(
            isScrollingDown = bottomBarScrollState.isScrollingDown,
            scrollOffset = bottomBarScrollState.scrollOffset,
            previousScrollOffset = bottomBarScrollState.previousScrollOffset,
            threshold = 30f
        )
    } else null

    val aboutDialog = rememberCustomDialog { AboutDialog(it) }
    val loadingDialog = rememberLoadingDialog()

    var isUnrooted by remember { mutableStateOf(!rootAvailable()) }
    LaunchedEffect(Unit) { isUnrooted = !rootAvailable() }

    val exportBugreportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            loadingDialog.show()
            context.contentResolver.openOutputStream(uri)?.use { output ->
                val bugReport = if (isUnrooted) getBugreportFileUnrooted(context) else getBugreportFile(context)
                bugReport.inputStream().use { it.copyTo(output) }
            }
            loadingDialog.hide()
            snackBarHost.showSnackbar(context.getString(R.string.log_saved))
        }
    }

    var suCompatStatus by rememberSaveable { mutableStateOf("") }
    var kernelUmountStatus by rememberSaveable { mutableStateOf("") }
    var adbRootStatus by rememberSaveable { mutableStateOf("") }
    var shellSuAdbOnlyStatus by rememberSaveable { mutableStateOf("") }
    var isShellSuAdbOnlyEnabled by rememberSaveable { mutableStateOf(false) }
    var selinuxHideStatus by rememberSaveable { mutableStateOf("") }
    var sulogStatus by rememberSaveable { mutableStateOf("") }
    var isSulogEnabled by rememberSaveable { mutableStateOf(false) }
    var avcSpoofStatus by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        suCompatStatus = getFeatureStatus("su_compat")
        kernelUmountStatus = getFeatureStatus("kernel_umount")
        sulogStatus = getFeatureStatus("sulog")
        isSulogEnabled = if (sulogStatus == "forced") {
            getFeatureValue("sulog") == 1L
        } else {
            getFeaturePersistValue("sulog") == 1L
        }
        adbRootStatus = getFeatureStatus("adb_root")
        shellSuAdbOnlyStatus = getFeatureStatus("shell_su_adb_only")
        isShellSuAdbOnlyEnabled = if (shellSuAdbOnlyStatus == "forced") {
            getFeatureValue("shell_su_adb_only") == 1L
        } else {
            getFeaturePersistValue("shell_su_adb_only") == 1L
        }
        selinuxHideStatus = getFeatureStatus("selinux_hide")
        avcSpoofStatus = getFeatureStatus("avc_spoof")
    }

    Scaffold(
        topBar = {
            NextSimpleTopBar(
                title = stringResource(R.string.settings),
                pillBorder = cardBorder,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost, modifier = Modifier.padding(bottom = navBarPadding)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .let { modifier ->
                    if (bottomBarScrollConnection != null) {
                        modifier
                            .nestedScroll(bottomBarScrollConnection)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                    } else {
                        modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp)
                .padding(bottom = navBarPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (ksuVersion != null) {
                NextKernelFeaturesCard(
                    suCompatStatus = suCompatStatus,
                    kernelUmountStatus = kernelUmountStatus,
                    sulogStatusParam = sulogStatus,
                    isSulogEnabled = isSulogEnabled,
                    onSulogEnabledChange = { isSulogEnabled = it },
                    adbRootStatus = adbRootStatus,
                    shellSuAdbOnlyStatusParam = shellSuAdbOnlyStatus,
                    isShellSuAdbOnlyEnabled = isShellSuAdbOnlyEnabled,
                    onShellSuAdbOnlyEnabledChange = { isShellSuAdbOnlyEnabled = it },
                    selinuxHideStatus = selinuxHideStatus,
                    avcSpoofStatus = avcSpoofStatus,
                    border = cardBorder,
                    scope = scope
                )
                NextSecurityCard(
                    navigator = navigator,
                    loadingDialog = loadingDialog,
                    border = cardBorder
                )
            }

            NextAppSettingsCard(
                navigator = navigator,
                prefs = prefs,
                aboutDialog = aboutDialog,
                exportBugreportLauncher = exportBugreportLauncher,
                loadingDialog = loadingDialog,
                scope = scope,
                context = context,
                isUnrooted = isUnrooted,
                border = cardBorder
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun NextSettingSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun NextKernelFeaturesCard(
    suCompatStatus: String,
    kernelUmountStatus: String,
    sulogStatusParam: String,
    isSulogEnabled: Boolean,
    onSulogEnabledChange: (Boolean) -> Unit,
    adbRootStatus: String,
    shellSuAdbOnlyStatusParam: String,
    isShellSuAdbOnlyEnabled: Boolean,
    onShellSuAdbOnlyEnabledChange: (Boolean) -> Unit,
    selinuxHideStatus: String,
    avcSpoofStatus: String,
    border: BorderStroke?,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    val suCompatSupported = suCompatStatus == "supported"
    val kernelUmountSupported = kernelUmountStatus == "supported"
    val sulogSupported = sulogStatusParam == "supported"
    val adbRootSupported = adbRootStatus == "supported"
    val shellSuAdbOnlySupported = shellSuAdbOnlyStatusParam == "supported"
    val selinuxHideSupported = selinuxHideStatus == "supported"
    val avcSpoofSupported = avcSpoofStatus == "supported"

    NextCard(
        cornerRadius = 20.dp,
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NextSettingSectionHeader(stringResource(R.string.home_kernel))

            var umountChecked by rememberSaveable {
                mutableStateOf(Natives.isDefaultUmountModules())
            }
            SwitchItem(
                icon = Icons.Filled.FolderDelete,
                title = stringResource(R.string.settings_umount_modules_default),
                summary = stringResource(R.string.settings_umount_modules_default_summary),
                checked = umountChecked,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) {
                if (Natives.setDefaultUmountModules(it)) umountChecked = it
            }

            var isSuEnabled by rememberSaveable { mutableStateOf(Natives.isSuEnabled()) }
            val suCompatSummary = when (suCompatStatus) {
                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                "forced" -> stringResource(id = R.string.feature_status_forced_summary)
                else -> stringResource(R.string.settings_enable_su_summary)
            }
            SwitchItem(
                icon = Icons.Filled.RemoveModerator,
                title = stringResource(R.string.settings_enable_su),
                summary = suCompatSummary,
                checked = isSuEnabled,
                enabled = suCompatSupported,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) { checked ->
                val prefsLocal = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                if (Natives.setSuEnabled(checked)) {
                    execKsud("feature save", true)
                    prefsLocal.edit { putInt("su_compat_mode", if (checked) 0 else 2) }
                    isSuEnabled = checked
                }
            }

            var isKernelUmountEnabled by rememberSaveable { mutableStateOf(Natives.isKernelUmountEnabled()) }
            val kernelUmountSummary = when (kernelUmountStatus) {
                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                "forced" -> stringResource(id = R.string.feature_status_forced_summary)
                else -> stringResource(id = R.string.settings_enable_kernel_umount_summary)
            }
            SwitchItem(
                icon = Icons.Filled.RemoveCircle,
                title = stringResource(id = R.string.settings_enable_kernel_umount),
                summary = kernelUmountSummary,
                checked = isKernelUmountEnabled,
                enabled = kernelUmountSupported,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) { checked ->
                val prefsLocal = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                if (Natives.setKernelUmountEnabled(checked)) {
                    execKsud("feature save", true)
                    prefsLocal.edit { putInt("kernel_umount_mode", if (checked) 0 else 2) }
                    isKernelUmountEnabled = checked
                }
            }

            val sulogSummary = when (sulogStatusParam) {
                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                "forced" -> stringResource(id = R.string.feature_status_forced_summary)
                else -> stringResource(id = R.string.settings_sulog_summary)
            }
            SwitchItem(
                icon = Icons.AutoMirrored.Filled.Article,
                title = stringResource(id = R.string.settings_sulog),
                summary = sulogSummary,
                checked = isSulogEnabled,
                enabled = sulogSupported,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) { checked ->
                if (execKsud("feature set sulog ${if (checked) 1 else 0}", true)) {
                    execKsud("feature save", true)
                    onSulogEnabledChange(checked)
                }
            }

            var isAdbRootEnabled by rememberSaveable { mutableStateOf(Natives.isAdbRootEnabled()) }
            val adbRootSummary = when (adbRootStatus) {
                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                "forced" -> stringResource(id = R.string.feature_status_forced_summary)
                else -> stringResource(id = R.string.settings_adb_root_summary)
            }
            SwitchItem(
                icon = Icons.Filled.Usb,
                title = stringResource(id = R.string.settings_adb_root),
                summary = adbRootSummary,
                checked = isAdbRootEnabled,
                enabled = adbRootSupported,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) { checked ->
                val prefsLocal = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                if (Natives.setAdbRootEnabled(checked)) {
                    execKsud("feature save", true)
                    prefsLocal.edit { putInt("adb_root_mode", if (checked) 0 else 2) }
                    com.topjohnwu.superuser.ShellUtils.fastCmd("setprop ctl.restart adbd")
                    isAdbRootEnabled = checked
                }
            }

            val shellSuAdbOnlySummary = when (shellSuAdbOnlyStatusParam) {
                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                "forced" -> stringResource(id = R.string.feature_status_forced_summary)
                else -> stringResource(id = R.string.settings_shell_su_adb_only_summary)
            }
            SwitchItem(
                icon = Icons.Filled.VerifiedUser,
                title = stringResource(id = R.string.settings_shell_su_adb_only),
                summary = shellSuAdbOnlySummary,
                checked = isShellSuAdbOnlyEnabled,
                enabled = shellSuAdbOnlySupported,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) { checked ->
                if (execKsud("feature set shell_su_adb_only ${if (checked) 1 else 0}", true)) {
                    execKsud("feature save", true)
                    onShellSuAdbOnlyEnabledChange(checked)
                }
            }

            var isSelinuxHideEnabled by rememberSaveable { mutableStateOf(Natives.isSelinuxHideEnabled()) }
            val selinuxHideSummary = when (selinuxHideStatus) {
                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                "forced" -> stringResource(id = R.string.feature_status_forced_summary)
                else -> stringResource(id = R.string.settings_selinux_hide_summary)
            }
            SwitchItem(
                icon = Icons.Filled.Policy,
                title = stringResource(id = R.string.settings_selinux_hide),
                summary = selinuxHideSummary,
                checked = isSelinuxHideEnabled,
                enabled = selinuxHideSupported,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) { checked ->
                scope.launch(Dispatchers.IO) {
                    val prefsLocal = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    val status = Natives.setSelinuxHideEnabled(checked)
                    execKsud("feature save", true)
                    prefsLocal.edit { putInt("selinux_hide_mode", if (checked) 0 else 2) }
                    isSelinuxHideEnabled = checked
                    when (status) {
                        0 -> {}
                        -OsConstants.EAGAIN -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context, R.string.settings_selinux_hide_reboot_required,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        else -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context, context.getString(R.string.settings_selinux_hide_failed, status),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }

            var isAvcSpoofEnabled by rememberSaveable { mutableStateOf(Natives.isAvcSpoofEnabled()) }
            val avcSpoofSummary = when (avcSpoofStatus) {
                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                "forced" -> stringResource(id = R.string.feature_status_forced_summary)
                else -> stringResource(id = R.string.settings_enable_avc_spoof_summary)
            }
            SwitchItem(
                icon = Icons.Filled.Shield,
                title = stringResource(id = R.string.settings_enable_avc_spoof),
                summary = avcSpoofSummary,
                checked = isAvcSpoofEnabled,
                enabled = avcSpoofSupported,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) { checked ->
                val prefsLocal = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                if (Natives.setAvcSpoofEnabled(checked)) {
                    execKsud("feature save", true)
                    prefsLocal.edit { putInt("avc_spoof_mode", if (checked) 0 else 2) }
                    isAvcSpoofEnabled = checked
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextSecurityCard(
    navigator: DestinationsNavigator,
    loadingDialog: LoadingDialogHandle,
    border: BorderStroke?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isGkiKernel = remember { getKernelVersion().isGKI() }
    var susfsAlreadySupported by remember { mutableStateOf(false) }
    var susfsGuardLkmLoaded by remember { mutableStateOf(false) }
    var susfsLoadInProgress by remember { mutableStateOf(false) }
    var showSusfsLoadSheet by remember { mutableStateOf(false) }
    var autoLoadSusfs by rememberSaveable { mutableStateOf(false) }
    var autoLoadStateLoaded by remember { mutableStateOf(false) }
    var autoLoadUpdating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        autoLoadSusfs = runCatching { getSusfsAutoLoadEnabled() }.getOrDefault(false)
        autoLoadStateLoaded = true
    }
    LaunchedEffect(isGkiKernel) {
        if (isGkiKernel) {
            val state = withContext(Dispatchers.IO) {
                val supported = runCatching { getSuSFS().trim() == "Supported" }.getOrDefault(false)
                val lkmLoaded = runCatching { isSusfsGuardLkmLoaded() }.getOrDefault(false)
                supported to lkmLoaded
            }
            susfsAlreadySupported = state.first
            susfsGuardLkmLoaded = state.second
        }
    }

    val persistAutoLoad: (Boolean, String?) -> Unit = { enabled, kmi ->
        if (!autoLoadUpdating) {
            autoLoadUpdating = true
            scope.launch {
                val updated = withContext(Dispatchers.IO) {
                    runCatching { setSusfsAutoLoadEnabled(enabled, kmi) }.getOrDefault(false)
                }
                autoLoadUpdating = false
                if (updated) {
                    autoLoadSusfs = enabled
                    Toast.makeText(
                        context,
                        if (enabled) R.string.susfs_auto_load_enabled
                        else R.string.susfs_auto_load_disabled,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, R.string.susfs_auto_load_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val selectAutoLoadKmiDialog = rememberSelectKmiDialog { selectedKmi ->
        if (selectedKmi != null) persistAutoLoad(true, selectedKmi)
    }
    val onAutoLoadChanged: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            autoLoadUpdating = true
            scope.launch {
                val detectedKmi = withContext(Dispatchers.IO) {
                    runCatching { getCurrentKmi().trim() }
                        .getOrNull()
                        ?.takeIf { it.matches(Regex("[A-Za-z0-9._-]+")) }
                }
                autoLoadUpdating = false
                if (detectedKmi == null) {
                    showSusfsLoadSheet = false
                    selectAutoLoadKmiDialog.show()
                } else {
                    persistAutoLoad(true, detectedKmi)
                }
            }
        } else {
            persistAutoLoad(false, null)
        }
    }

    val startSusfsLoad: (String?) -> Unit = { selectedKmi ->
        susfsLoadInProgress = true
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { loadSusfsGuardLkm(selectedKmi) }.getOrDefault(false)
            }
            susfsLoadInProgress = false
            if (loaded) {
                susfsAlreadySupported = true
                susfsGuardLkmLoaded = true
            }
            showSusfsLoadSheet = false
            Toast.makeText(
                context,
                if (loaded) R.string.susfs_load_success else R.string.susfs_load_failed,
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val selectSusfsKmiDialog = rememberSelectKmiDialog { selectedKmi ->
        selectedKmi?.let(startSusfsLoad)
    }
    val unloadSusfs: () -> Unit = {
        if (!susfsLoadInProgress) {
            susfsLoadInProgress = true
            scope.launch {
                val unloaded = withContext(Dispatchers.IO) {
                    runCatching { unloadSusfsGuardLkm() }.getOrDefault(false)
                }
                susfsLoadInProgress = false
                if (unloaded) {
                    susfsGuardLkmLoaded = false
                    susfsAlreadySupported = withContext(Dispatchers.IO) {
                        runCatching { getSuSFS().trim() == "Supported" }.getOrDefault(false)
                    }
                }
                showSusfsLoadSheet = false
                Toast.makeText(
                    context,
                    if (unloaded) R.string.susfs_unload_success else R.string.susfs_unload_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    NextCard(
        cornerRadius = 20.dp,
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NextSettingSectionHeader("Security & Tools")

            var isSelinuxPermissive by rememberSaveable {
                mutableStateOf(getSelinuxEnforce() == false)
            }

            SwitchItem(
                icon = Icons.Filled.Security,
                title = stringResource(R.string.set_selinux),
                summary = stringResource(R.string.set_selinux_summary),
                checked = isSelinuxPermissive,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) { checked ->
                val shouldEnforce = !checked
                if (setSelinuxEnforce(shouldEnforce)) {
                    isSelinuxPermissive = !shouldEnforce
                }
            }

            if (isGkiKernel) {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !susfsLoadInProgress) { showSusfsLoadSheet = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Filled.Memory, null) },
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.susfs_load_action),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.susfs_load_summary)) }
                )
            }

            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { navigator.navigate(AppProfileTemplateScreenDestination) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Filled.Fence, null) },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.settings_profile_template),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                supportingContent = { Text(stringResource(R.string.settings_profile_template_summary)) }
            )

            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { navigator.navigate(BackupRestoreScreenDestination) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Filled.Backup, null) },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.backup_restore),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )

            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { navigator.navigate(DeveloperScreenDestination) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Filled.DeveloperBoard, null) },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.developer),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )

            if (Natives.isLkmMode && !Natives.isLateLoadMode) {
                UninstallItem(
                    navigator = navigator,
                    withLoading = { loadingDialog.withLoading(it) },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }

    if (showSusfsLoadSheet) {
        ModalBottomSheet(onDismissRequest = { showSusfsLoadSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.susfs_load_action),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.susfs_load_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .toggleable(
                            value = autoLoadSusfs,
                            enabled = !autoLoadUpdating,
                            role = Role.Switch,
                            onValueChange = onAutoLoadChanged
                        ),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.susfs_auto_load)) },
                    supportingContent = { Text(stringResource(R.string.susfs_auto_load_summary)) },
                    trailingContent = {
                        Switch(
                            checked = autoLoadSusfs,
                            enabled = !autoLoadUpdating,
                            onCheckedChange = onAutoLoadChanged
                        )
                    }
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (susfsAlreadySupported && !susfsGuardLkmLoaded) 0.55f else 1f),
                    enabled = !susfsLoadInProgress && (susfsGuardLkmLoaded || !susfsAlreadySupported),
                    onClick = {
                        if (susfsGuardLkmLoaded) {
                            unloadSusfs()
                        } else {
                            if (susfsLoadInProgress) return@Button
                            susfsLoadInProgress = true
                            scope.launch {
                                val kmi = runCatching { getCurrentKmi().trim() }.getOrDefault("")
                                if (kmi.isBlank()) {
                                    susfsLoadInProgress = false
                                    selectSusfsKmiDialog.show()
                                } else {
                                    startSusfsLoad(null)
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        stringResource(
                            when {
                                susfsLoadInProgress && susfsGuardLkmLoaded -> R.string.susfs_unload_in_progress
                                susfsLoadInProgress -> R.string.susfs_load_in_progress
                                susfsGuardLkmLoaded -> R.string.susfs_unload_button
                                susfsAlreadySupported -> R.string.susfs_builtin_loaded
                                else -> R.string.susfs_load_button
                            }
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextAppSettingsCard(
    navigator: DestinationsNavigator,
    prefs: android.content.SharedPreferences,
    aboutDialog: DialogHandle,
    exportBugreportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    loadingDialog: LoadingDialogHandle,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context,
    isUnrooted: Boolean,
    border: BorderStroke?
) {
    NextCard(
        cornerRadius = 20.dp,
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NextSettingSectionHeader("Manager")

            var checkUpdate by rememberSaveable {
                mutableStateOf(prefs.getBoolean("check_update", true))
            }

            SwitchItem(
                icon = Icons.Filled.Update,
                title = stringResource(R.string.settings_check_update),
                summary = stringResource(R.string.settings_check_update_summary),
                checked = checkUpdate,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) {
                prefs.edit { putBoolean("check_update", it) }
                checkUpdate = it
            }

            var requireBiometric by rememberSaveable {
                mutableStateOf(prefs.getBoolean("enable_biometric_lock", false))
            }
            var appLockTimeout by rememberSaveable {
                mutableStateOf(prefs.getLong("app_lock_timeout", 60000L))
            }
            var showTimeoutMenu by remember { mutableStateOf(false) }

            val timeoutOptions = remember {
                listOf(
                    0L to R.string.settings_app_lock_timeout_immediate,
                    60000L to R.string.settings_app_lock_timeout_1m,
                    300000L to R.string.settings_app_lock_timeout_5m
                )
            }

            val timeoutLabelRes = timeoutOptions.find { it.first == appLockTimeout }?.second
                ?: R.string.settings_app_lock_timeout_1m
            val timeoutLabel = stringResource(timeoutLabelRes)
            val onRequireBiometricChange: (Boolean) -> Unit = { newValue ->
                if (newValue && !(context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager).isDeviceSecure) {
                    Toast.makeText(context, context.getString(R.string.settings_app_lock_no_credential), Toast.LENGTH_SHORT).show()
                } else {
                    requireBiometric = newValue
                    prefs.edit { putBoolean("enable_biometric_lock", newValue) }
                }
            }

            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .toggleable(
                        value = requireBiometric,
                        role = Role.Switch,
                        onValueChange = onRequireBiometricChange
                    ),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Filled.Lock, null) },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.settings_app_lock),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                supportingContent = {
                    Column(modifier = Modifier.animateContentSize()) {
                        Text(stringResource(R.string.settings_app_lock_summary))
                        if (requireBiometric) {
                            Box {
                                Text(
                                    text = "${stringResource(R.string.settings_app_lock_timeout)}: $timeoutLabel",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { showTimeoutMenu = true }
                                        .padding(vertical = 4.dp)
                                )
                                DropdownMenu(
                                    expanded = showTimeoutMenu,
                                    onDismissRequest = { showTimeoutMenu = false }
                                ) {
                                    timeoutOptions.forEach { (time, stringRes) ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(stringRes)) },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = if (appLockTimeout == time) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (appLockTimeout == time) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            onClick = {
                                                appLockTimeout = time
                                                prefs.edit { putLong("app_lock_timeout", time) }
                                                showTimeoutMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                trailingContent = {
                    Switch(
                        checked = requireBiometric,
                        onCheckedChange = onRequireBiometricChange
                    )
                }
            )

            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { navigator.navigate(CustomizationScreenDestination) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Filled.Palette, null) },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.customization),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )

            var showBottomsheet by remember { mutableStateOf(false) }

            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showBottomsheet = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Filled.BugReport, null) },
                headlineContent = {
                    Text(
                        text = stringResource(
                            if (isUnrooted) R.string.export_log_unrooted else R.string.export_log
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )

            if (showBottomsheet) {
                ModalBottomSheet(onDismissRequest = { showBottomsheet = false }) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(
                                onClick = {
                                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                                    val current = LocalDateTime.now().format(formatter)
                                    exportBugreportLauncher.launch("Next_Manager_bugreport_${current}.tar.gz")
                                    showBottomsheet = false
                                },
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null)
                            }
                            Text(
                                text = stringResource(id = R.string.save_log),
                                modifier = Modifier.padding(top = 12.dp),
                                textAlign = TextAlign.Center.also {
                                    LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.None
                                    )
                                }
                            )
                        }
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val bugreport = loadingDialog.withLoading {
                                            withContext(Dispatchers.IO) {
                                                if (isUnrooted) getBugreportFileUnrooted(context)
                                                else getBugreportFile(context)
                                            }
                                        }
                                        val uri: Uri = FileProvider.getUriForFile(
                                            context,
                                            "${BuildConfig.APPLICATION_ID}.fileprovider",
                                            bugreport
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            setDataAndType(uri, "application/gzip")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, context.getString(R.string.send_log))
                                        )
                                    }
                                    showBottomsheet = false
                                },
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null)
                            }
                            Text(
                                text = stringResource(id = R.string.send_log),
                                modifier = Modifier.padding(top = 12.dp),
                                textAlign = TextAlign.Center.also {
                                    LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.None
                                    )
                                }
                            )
                        }
                    }
                }
            }

            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { aboutDialog.show() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Filled.ContactPage, null) },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    }
}
