package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.getKernelType
import com.rifsxd.ksunext.getKernelVersion
import com.rifsxd.ksunext.ui.LocalNavBarEnabled
import com.rifsxd.ksunext.ui.LocalScrollState
import com.rifsxd.ksunext.ui.rememberScrollConnection
import com.rifsxd.ksunext.ui.util.getModuleCount
import com.rifsxd.ksunext.ui.util.getSuperuserCount
import com.rifsxd.ksunext.ui.util.isSuCompatDisabled
import com.rifsxd.ksunext.ui.util.restartActivity
import com.rifsxd.ksunext.ui.util.rootAvailable
import com.rifsxd.ksunext.ui.viewmodel.ModuleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val GeminiBlue = Color(0xFF4285F4)
private val GeminiPurple = Color(0xFF9B72CB)
private val GeminiPink = Color(0xFFD96570)

@Composable
private fun rememberNextCardBorder(isCustomBg: Boolean): BorderStroke? {
    if (!isCustomBg) return null
    val primaryColor = MaterialTheme.colorScheme.primary
    val brush = remember(primaryColor) {
        Brush.linearGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.75f),
                GeminiPurple.copy(alpha = 0.75f),
                GeminiPink.copy(alpha = 0.65f)
            )
        )
    }
    return remember(brush) { BorderStroke(1.dp, brush) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextScreen(navigator: DestinationsNavigator) {
    val kernelVersion = getKernelVersion()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val isManager = Natives.isManager
    val fullFeatured = Natives.isFullFeatured()
    val ksuVersion = if (isManager) Natives.version else null
    val ksuVersionTag = if (isManager) Natives.getVersionTag() else null
    val kernelUAPIVersion = if (isManager) Natives.kernelUAPIVersion else null
    val managerUAPIVersion = Natives.managerUAPIVersion

    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)

    var backgroundMode by remember {
        mutableStateOf(prefs.getString("background_mode", "default") ?: "default")
    }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "background_mode") {
                backgroundMode = sp.getString("background_mode", "default") ?: "default"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    val cardBorder = rememberNextCardBorder(isCustomBg = backgroundMode != "default")

    val bottomBarScrollState = LocalScrollState.current
    val navBarEnabled = LocalNavBarEnabled.current
    val isNavBarHidden = (bottomBarScrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            if (isNavBarHidden) 0.dp else 112.dp

    val bottomBarScrollConnection = if (bottomBarScrollState != null) {
        rememberScrollConnection(
            isScrollingDown = bottomBarScrollState.isScrollingDown,
            scrollOffset = bottomBarScrollState.scrollOffset,
            previousScrollOffset = bottomBarScrollState.previousScrollOffset,
            threshold = 30f
        )
    } else null

    val moduleViewModel: ModuleViewModel = viewModel()
    var homeInfo by remember { mutableStateOf(HomeInfoSnapshot()) }

    LaunchedEffect(ksuVersion, moduleViewModel.moduleList) {
        homeInfo = withContext(Dispatchers.IO) {
            buildHomeInfoSnapshot(context, ksuVersion, moduleViewModel.moduleList)
        }
    }

    val managerVersionSubtitle = remember(homeInfo.managerVersion, homeInfo.managerUAPIVersion) {
        val verName = homeInfo.managerVersion.first
        val verCode = homeInfo.managerVersion.second
        if (verName.isNotEmpty()) {
            "$verName ($verCode-${homeInfo.managerUAPIVersion})"
        } else {
            val pkg = getManagerVersion(context)
            "${pkg.first} (${pkg.second}-$managerUAPIVersion)"
        }
    }

    Scaffold(
        topBar = {
            NextTopBar(
                versionSubtitle = managerVersionSubtitle,
                ksuVersion = ksuVersion,
                pillBorder = cardBorder,
                onInstallClick = {
                    navigator.navigate(InstallScreenDestination)
                },
                onSettingsClick = {
                    navigator.navigate(SettingScreenDestination) {
                        popUpTo(NavGraphs.root.startRoute) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
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
            val lkmMode = ksuVersion?.let { Natives.isLkmMode }

            // 1. Hero Status Card
            NextHeroStatusCard(
                kernelVersion = kernelVersion,
                ksuVersion = ksuVersion,
                uapiVer = kernelUAPIVersion,
                lkmMode = lkmMode,
                ksuVersionTag = ksuVersionTag,
                hookMode = homeInfo.hookMode,
                border = cardBorder,
                onClickInstall = {
                    navigator.navigate(InstallScreenDestination)
                }
            )

            // 2. Warning & Update Banners (if any)
            val currentVersionCode = getManagerVersion(context).second
            val requiresNewKernel = isManager && kernelUAPIVersion != null && managerUAPIVersion > kernelUAPIVersion
            val requiresNewManager = isManager && kernelUAPIVersion != null && managerUAPIVersion < kernelUAPIVersion
            val canInstallKernelUpdate = lkmMode == true && !Natives.isLateLoadMode

            if (requiresNewKernel) {
                WarningCard(
                    stringResource(
                        id = if (canInstallKernelUpdate) R.string.require_kernel_version else R.string.require_kernel_version_gki,
                        kernelUAPIVersion!!,
                        managerUAPIVersion
                    ),
                    onClick = if (canInstallKernelUpdate) {
                        { navigator.navigate(InstallScreenDestination) }
                    } else null
                )
            }

            if (requiresNewManager) {
                WarningCard(
                    stringResource(
                        id = R.string.require_manager_version,
                        managerUAPIVersion,
                        kernelUAPIVersion!!
                    )
                )
            }

            val showLkmUpdate = isManager && lkmMode == true && Natives.isLkmBundled &&
                    ksuVersion?.toLong() != currentVersionCode && !requiresNewKernel && !requiresNewManager

            if (showLkmUpdate) {
                WarningCard(
                    message = stringResource(R.string.home_lkm_update_available),
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { navigator.navigate(InstallScreenDestination) }
                )
            }

            if (ksuVersion != null && !rootAvailable()) {
                WarningCard(
                    stringResource(id = R.string.grant_root_failed),
                    onClick = {
                        restartActivity(context)
                    }
                )
            }

            val checkUpdate = prefs.getBoolean("check_update", true)
            if (checkUpdate) {
                UpdateCard()
            }

            // 3. Bento 2-Column Stats (Superuser & Modules)
            if (fullFeatured) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        NextSuperuserTile(
                            border = cardBorder,
                            onClick = {
                                navigator.navigate(SuperUserScreenDestination) {
                                    popUpTo(NavGraphs.root.startRoute) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NextModuleTile(
                            moduleViewModel = moduleViewModel,
                            border = cardBorder,
                            onClick = {
                                navigator.navigate(ModuleScreenDestination) {
                                    popUpTo(NavGraphs.root.startRoute) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }

            // 4. Full-Width Kernel & ABI Card
            NextKernelWideCard(
                kernelRelease = "${homeInfo.unameRelease} (${homeInfo.unameMachine})",
                abiList = Build.SUPPORTED_ABIS.joinToString(", "),
                uidInfo = if (developerOptionsEnabled) "Manager UID: ${homeInfo.managerAppId}" else null,
                border = cardBorder
            )

            // 5. Hook Mode & Metamodule (when KSU active)
            if (ksuVersion != null) {
                val metaInfo = homeInfo.metaInfo
                val metaText = when {
                    homeInfo.metaModule == "Installed" && metaInfo != null && !metaInfo.enabled ->
                        "${stringResource(R.string.disabled)} • ${metaInfo.name}"
                    homeInfo.metaModule == "Installed" && metaInfo != null ->
                        "${metaInfo.name} (${metaInfo.version})"
                    homeInfo.metaModule == "Installed" ->
                        stringResource(R.string.installed)
                    else ->
                        stringResource(R.string.home_not_installed)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NextSpecTile(
                        label = stringResource(R.string.hook_mode),
                        value = homeInfo.hookMode ?: stringResource(R.string.unavailable),
                        border = cardBorder,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    NextSpecTile(
                        label = stringResource(R.string.home_metamodule_status),
                        value = metaText,
                        border = cardBorder,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // 6. SuSFS & Zygisk (when supported/enabled)
                if (homeInfo.suSFS == "Supported" || homeInfo.zygiskEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (homeInfo.suSFS == "Supported") {
                            NextSpecTile(
                                label = stringResource(R.string.home_susfs_version),
                                value = "${stringResource(R.string.supported)} | ${homeInfo.suSFSVersion ?: stringResource(R.string.unavailable)} (${homeInfo.suSFSVariant ?: stringResource(R.string.unavailable)})",
                                border = cardBorder,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                        if (homeInfo.zygiskEnabled) {
                            val zygiskInfo = homeInfo.zygiskInfo
                            val zygiskText = if (zygiskInfo != null) {
                                "${stringResource(R.string.enabled)} • ${zygiskInfo.name}"
                            } else {
                                stringResource(R.string.enabled)
                            }
                            NextSpecTile(
                                label = stringResource(R.string.zygisk_status),
                                value = zygiskText,
                                border = cardBorder,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }

            // 7. Android Version & SELinux / Seccomp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NextSpecTile(
                    label = stringResource(R.string.home_android),
                    value = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                    border = cardBorder,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                NextSpecTile(
                    label = "SELinux • Seccomp",
                    value = "${stringResource(homeInfo.selinuxStatusRes)} • ${homeInfo.seccompStatus}",
                    border = cardBorder,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // 8. Interactive Footer Signature Card (Opens .Term)
            GeminiSignatureCard(
                border = cardBorder,
                onClick = {
                    try {
                        val termClass = Class.forName("${context.packageName}.Term")
                        context.startActivity(Intent(context, termClass))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Terminal activity not found", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextTopBar(
    versionSubtitle: String,
    ksuVersion: Int?,
    pillBorder: BorderStroke?,
    onInstallClick: () -> Unit,
    onSettingsClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    var isSpinning by remember { mutableStateOf(false) }
    var rotationTarget by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = rotationTarget,
        animationSpec = tween(
            durationMillis = 1400,
            easing = FastOutSlowInEasing
        ),
        finishedListener = {
            isSpinning = false
        },
        label = "nextLogoSpin"
    )

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isSpinning) {
                        isSpinning = true
                        rotationTarget += 360f * 6
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ksu_next),
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = rotation
                    }
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Next Manager",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (versionSubtitle.isNotEmpty()) {
                        Text(
                            text = versionSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        actions = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = pillBorder,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(id = R.string.settings),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (ksuVersion != null) {
                        IconButton(
                            onClick = onInstallClick,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Archive,
                                contentDescription = stringResource(id = R.string.install),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        var showDropdown by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showDropdown = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PowerSettingsNew,
                                    contentDescription = stringResource(id = R.string.reboot),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false }
                            ) {
                                RebootDropdownItem(id = R.string.reboot)
                                RebootDropdownItem(id = R.string.reboot_userspace, reason = "soft-reboot")

                                val pm = LocalContext.current.getSystemService(Context.POWER_SERVICE) as PowerManager?
                                @Suppress("DEPRECATION")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
                                    RebootDropdownItem(id = R.string.reboot_userspace, reason = "userspace")
                                }
                                RebootDropdownItem(id = R.string.reboot_recovery, reason = "recovery")
                                RebootDropdownItem(id = R.string.reboot_bootloader, reason = "bootloader")
                                RebootDropdownItem(id = R.string.reboot_download, reason = "download")
                                RebootDropdownItem(id = R.string.reboot_edl, reason = "edl")
                            }
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun NextHeroStatusCard(
    kernelVersion: com.rifsxd.ksunext.KernelVersion,
    ksuVersion: Int?,
    uapiVer: Int?,
    lkmMode: Boolean?,
    ksuVersionTag: String?,
    hookMode: String?,
    border: BorderStroke?,
    onClickInstall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (ksuVersion == null) Modifier.clickable { onClickInstall() }
                else Modifier
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle Gemini Sparkle Watermark
            GeminiSparkleIcon(
                animate = false,
                alpha = 0.14f,
                modifier = Modifier
                    .size(104.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 18.dp, y = (-18).dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Chip Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val chipText = when {
                        ksuVersion != null -> {
                            val mode = if (lkmMode == true) "LKM" else "BUILT-IN"
                            "WORKING • $mode (${kernelVersion.getKernelType()})"
                        }
                        kernelVersion.isGKI() -> "GKI • READY"
                        else -> "UNSUPPORTED"
                    }

                    val chipContainerColor = when {
                        ksuVersion != null -> MaterialTheme.colorScheme.primaryContainer
                        kernelVersion.isGKI() -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                    val chipContentColor = when {
                        ksuVersion != null -> MaterialTheme.colorScheme.onPrimaryContainer
                        kernelVersion.isGKI() -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = chipContainerColor
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    ksuVersion != null && Natives.isSafeMode -> Icons.Filled.Security
                                    ksuVersion != null -> Icons.Filled.VerifiedUser
                                    kernelVersion.isGKI() -> Icons.Filled.AutoFixHigh
                                    else -> Icons.Filled.MoodBad
                                },
                                contentDescription = null,
                                tint = chipContentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = chipContentColor
                            )
                        }
                    }

                    // Codename Badge: HydraGen
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            0.5.dp,
                            GeminiPurple.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            GeminiSparkleIcon(
                                animate = true,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "HydraGen",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Main Status Headline & Driver Details
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when {
                            ksuVersion != null -> stringResource(id = R.string.home_working)
                            kernelVersion.isGKI() -> stringResource(R.string.home_not_installed)
                            else -> stringResource(R.string.home_failure)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val subText = when {
                        ksuVersion != null -> {
                            val tag = if (!ksuVersionTag.isNullOrEmpty()) ksuVersionTag else "v0.0.0"
                            val verLine = stringResource(
                                R.string.home_working_version,
                                tag,
                                "$ksuVersion-${uapiVer ?: 0}"
                            )
                            if (!hookMode.isNullOrBlank() && hookMode != "Unavailable") {
                                "$verLine • $hookMode"
                            } else {
                                verLine
                            }
                        }
                        kernelVersion.isGKI() -> stringResource(R.string.home_click_to_install)
                        else -> stringResource(R.string.home_failure_tip)
                    }

                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Extra Warning Pills (SuCompat / Jailbreak / Custom LKM)
                val hasExtraBadges = ksuVersion != null && (
                        isSuCompatDisabled() ||
                        Natives.isLateLoadMode ||
                        (lkmMode == true && !Natives.isLkmBundled)
                )
                if (hasExtraBadges) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSuCompatDisabled()) {
                            NextMiniWarningChip(
                                text = stringResource(R.string.sucompat_disabled),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        if (Natives.isLateLoadMode) {
                            NextMiniWarningChip(
                                text = stringResource(R.string.jailbreak_mode),
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (lkmMode == true && !Natives.isLkmBundled) {
                            NextMiniWarningChip(
                                text = stringResource(R.string.home_lkm_custom),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextMiniWarningChip(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun NextSuperuserTile(
    border: BorderStroke?,
    onClick: () -> Unit
) {
    val count = getSuperuserCount()

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = border,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.superuser),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = if (count <= 1) {
                    stringResource(R.string.home_superuser_count_singular)
                } else {
                    stringResource(R.string.home_superuser_count_plural)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NextModuleTile(
    moduleViewModel: ModuleViewModel,
    border: BorderStroke?,
    onClick: () -> Unit
) {
    val count = getModuleCount()
    val moduleUpdateCount = remember(moduleViewModel.moduleList) {
        moduleViewModel.moduleList.count { module ->
            module.enabled && (moduleViewModel.checkUpdate(module).first.isNotEmpty())
        }
    }

    var step by remember { mutableStateOf(0) }
    LaunchedEffect(moduleUpdateCount) {
        if (moduleUpdateCount > 0) {
            step = 1
            delay(1200)
            step = 2
        } else {
            step = 0
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = border,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GeminiPurple)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.module),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (moduleUpdateCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "|",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        },
                        label = "moduleUpdateStep"
                    ) { target ->
                        when (target) {
                            1 -> Text(
                                text = stringResource(id = R.string.home_module_update_available),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            2 -> Text(
                                text = buildAnnotatedString {
                                    append(moduleUpdateCount.toString())
                                    append("*")
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = if (count <= 1) {
                    stringResource(R.string.home_module_count_singular)
                } else {
                    stringResource(R.string.home_module_count_plural)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NextSpecTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = border,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NextKernelWideCard(
    kernelRelease: String,
    abiList: String,
    uidInfo: String? = null,
    border: BorderStroke? = null
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.home_kernel),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = kernelRelease,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${stringResource(R.string.home_abi)}: $abiList",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uidInfo != null) {
                Text(
                    text = uidInfo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GeminiSignatureCard(
    border: BorderStroke?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GeminiSparkleIcon(
                animate = true,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "linex Project",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enjoy Your Life...",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GeminiSparkleIcon(
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    alpha: Float = 1f
) {
    val rotationAngle = if (animate) {
        val infiniteTransition = rememberInfiniteTransition(label = "geminiSparkleTransition")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "geminiSparkleRotation"
        )
        angle
    } else {
        0f
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(cx, cy) * 0.92f

        val gradient = Brush.linearGradient(
            colors = listOf(
                GeminiBlue.copy(alpha = alpha),
                GeminiPurple.copy(alpha = alpha),
                GeminiPink.copy(alpha = alpha)
            ),
            start = Offset.Zero,
            end = Offset(w, h)
        )

        val starPath = Path().apply {
            moveTo(cx, cy - r)
            quadraticTo(cx, cy, cx + r, cy)
            quadraticTo(cx, cy, cx, cy + r)
            quadraticTo(cx, cy, cx - r, cy)
            quadraticTo(cx, cy, cx, cy - r)
            close()
        }

        rotate(degrees = rotationAngle, pivot = Offset(cx, cy)) {
            drawPath(path = starPath, brush = gradient)
        }
    }
}
