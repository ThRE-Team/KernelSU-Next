package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rifsxd.ksunext.ui.LocalNavBarEnabled
import com.rifsxd.ksunext.ui.LocalScrollState
import com.rifsxd.ksunext.ui.rememberScrollConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.dropUnlessResumed
import com.rifsxd.ksunext.ui.MainActivity
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.SwitchItem
import com.rifsxd.ksunext.ui.util.refreshActivity
import com.rifsxd.ksunext.ui.util.LocalSnackbarHost
import com.rifsxd.ksunext.ui.util.LocaleHelper
import java.io.File
import kotlin.math.roundToInt

/**
 * @author rifsxd
 * @date 2025/6/1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CustomizationScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    // Bottom bar scroll tracking
    val bottomBarScrollState = LocalScrollState.current
    val bottomBarScrollConnection = if (bottomBarScrollState != null) {
        rememberScrollConnection(
            isScrollingDown = bottomBarScrollState.isScrollingDown,
            scrollOffset = bottomBarScrollState.scrollOffset,
            previousScrollOffset = bottomBarScrollState.previousScrollOffset,
            threshold = 30f
        )
    } else null
    val snackBarHost = LocalSnackbarHost.current

    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null

    val scrollState = LocalScrollState.current
    val navBarEnabled = LocalNavBarEnabled.current
    val isNavBarHidden = (scrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp

    val context = LocalContext.current
    val activity = context as? MainActivity
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var currentAppLocale by remember {
        mutableStateOf(LocaleHelper.getCurrentAppLocale(context))
    }
    var showLanguageSheet by remember { mutableStateOf(false) }

    var backgroundMode by rememberSaveable {
        mutableStateOf(prefs.getString("background_mode", "default") ?: "default")
    }
    var showBackgroundSheet by remember { mutableStateOf(false) }

    var wallpaperOpacity by rememberSaveable {
        mutableFloatStateOf(prefs.getFloat("wallpaper_opacity", 0.60f))
    }
    var cardOpacity by rememberSaveable {
        mutableFloatStateOf(prefs.getFloat("card_opacity", 0.75f))
    }
    var topBarOpacity by rememberSaveable {
        mutableFloatStateOf(prefs.getFloat("topbar_opacity", 0.80f))
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val destFile = File(context.filesDir, "custom_wallpaper")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val version = System.currentTimeMillis()
                prefs.edit {
                    putString("background_mode", "image")
                    putLong("wallpaper_version", version)
                }
                backgroundMode = "image"
                activity?.setBackgroundMode("image", version)
            } catch (_: Exception) {}
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        currentAppLocale = LocaleHelper.getCurrentAppLocale(context)
    }

    val languageOptions = remember(context) {
        listOf(
            LocaleHelper.SYSTEM_LANGUAGE_TAG to context.getString(R.string.system_default)
        ) + LocaleHelper.getSupportedLocales(context)
            .map { it.toLanguageTag() to it.getDisplayName(it) }
            .sortedBy { (_, displayName) -> displayName }
    }
    val currentLanguageTag = currentAppLocale?.toLanguageTag()
        ?: LocaleHelper.SYSTEM_LANGUAGE_TAG

    Scaffold(
        topBar = {
            TopBar(
                onBack = dropUnlessResumed {
                    navigator.popBackStack()
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost, modifier = Modifier.padding(bottom = navBarPadding)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
    
        Column(
            modifier = Modifier
                .padding(paddingValues)
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
        ) {

            val language = stringResource(id = R.string.settings_language)
            val currentLanguageDisplay = currentAppLocale?.let { it.getDisplayName(it) }
                ?: stringResource(R.string.system_default)

            ListItem(
                leadingContent = { Icon(Icons.Filled.Translate, language) },
                headlineContent = { Text(
                    text = language,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                ) },
                supportingContent = { Text(currentLanguageDisplay) },
                modifier = Modifier.clickable {
                    showLanguageSheet = true
                }
            )

            var useBanner by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("use_banner", true)
                )
            }
            if (ksuVersion != null) {
                SwitchItem(
                    icon = Icons.Filled.ViewCarousel,
                    title = stringResource(id = R.string.settings_banner),
                    summary = stringResource(id = R.string.settings_banner_summary),
                    checked = useBanner
                ) {
                    prefs.edit { putBoolean("use_banner", it) }
                    useBanner = it
                }
            }

            var enableAmoled by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_amoled", false)
                )
            }
            if (isSystemInDarkTheme()) {
                SwitchItem(
                    icon = Icons.Filled.Contrast,
                    title = stringResource(id = R.string.settings_amoled_mode),
                    summary = stringResource(id = R.string.settings_amoled_mode_summary),
                    checked = enableAmoled
                ) { checked ->
                    activity?.setAmoledMode(checked)
                    enableAmoled = checked
                }
            }

            val backgroundDisplay = when (backgroundMode) {
                "wallpaper" -> "Wallpaper"
                "image" -> "Image"
                else -> "Default"
            }
            ListItem(
                leadingContent = { Icon(Icons.Filled.Wallpaper, contentDescription = "Background") },
                headlineContent = {
                    Text(
                        text = "Background",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                supportingContent = { Text(backgroundDisplay) },
                modifier = Modifier.clickable {
                    showBackgroundSheet = true
                }
            )

            if (backgroundMode == "image") {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Image, contentDescription = "Pilih gambar") },
                    headlineContent = {
                        Text(
                            text = "Pilih gambar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = { Text("Ganti gambar latar belakang dari galeri") },
                    modifier = Modifier.clickable {
                        imagePickerLauncher.launch("image/*")
                    }
                )
            }

            if (backgroundMode == "wallpaper" || backgroundMode == "image") {
                OpacitySliderItem(
                    icon = Icons.Filled.Opacity,
                    title = if (backgroundMode == "wallpaper") "Opacity Wallpaper" else "Opacity Gambar",
                    value = wallpaperOpacity,
                    onValueChange = { value ->
                        wallpaperOpacity = value
                        activity?.setWallpaperOpacity(value)
                    }
                )

                OpacitySliderItem(
                    icon = Icons.Filled.Opacity,
                    title = "Opacity Card",
                    value = cardOpacity,
                    onValueChange = { value ->
                        cardOpacity = value
                        activity?.setCardOpacity(value)
                    }
                )

                OpacitySliderItem(
                    icon = Icons.Filled.Opacity,
                    title = "Opacity Title Bar",
                    value = topBarOpacity,
                    onValueChange = { value ->
                        topBarOpacity = value
                        activity?.setTopBarOpacity(value)
                    }
                )
            }

            var enableNavBar by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_navbar", true)
                )
            }
            SwitchItem(
                icon = Icons.Filled.Dock,
                title = stringResource(id = R.string.settings_navbar),
                summary = stringResource(id = R.string.settings_navbar_summary),
                checked = enableNavBar
            ) { checked ->
                activity?.setNavBarEnabled(checked)
                enableNavBar = checked
            }

            var enableStartupAnimation by rememberSaveable {
                mutableStateOf(prefs.getBoolean("enable_startup_animation", true))
            }
            SwitchItem(
                icon = Icons.Filled.Animation,
                title = stringResource(id = R.string.settings_startup_animation),
                summary = stringResource(id = R.string.settings_startup_animation_summary),
                checked = enableStartupAnimation
            ) { checked ->
                prefs.edit { putBoolean("enable_startup_animation", checked) }
                enableStartupAnimation = checked
            }
        }
    }

    if (showLanguageSheet) {
        LanguageBottomSheet(
            options = languageOptions,
            selectedLanguageTag = currentLanguageTag,
            onDismiss = { showLanguageSheet = false },
            onLanguageSelected = { languageTag ->
                showLanguageSheet = false
                if (languageTag != currentLanguageTag) {
                    LocaleHelper.setAppLocale(context, languageTag)
                    currentAppLocale = LocaleHelper.getCurrentAppLocale(context)
                    if (!LocaleHelper.usesFrameworkLocaleManager) {
                        refreshActivity(context)
                    }
                }
            }
        )
    }

    if (showBackgroundSheet) {
        BackgroundBottomSheet(
            selectedMode = backgroundMode,
            onDismiss = { showBackgroundSheet = false },
            onModeSelected = { mode ->
                showBackgroundSheet = false
                when (mode) {
                    "default" -> {
                        File(context.filesDir, "custom_wallpaper").delete()
                        prefs.edit { putString("background_mode", "default") }
                        backgroundMode = "default"
                        activity?.setBackgroundMode("default")
                    }
                    "wallpaper" -> {
                        prefs.edit { putString("background_mode", "wallpaper") }
                        backgroundMode = "wallpaper"
                        activity?.setBackgroundMode("wallpaper")
                    }
                    "image" -> {
                        if (File(context.filesDir, "custom_wallpaper").exists()) {
                            prefs.edit { putString("background_mode", "image") }
                            backgroundMode = "image"
                            activity?.setBackgroundMode("image")
                        } else {
                            imagePickerLauncher.launch("image/*")
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun OpacitySliderItem(
    icon: ImageVector,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = title) },
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${(value * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        supportingContent = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundBottomSheet(
    selectedMode: String,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val options = listOf(
        "default" to "Default",
        "wallpaper" to "Wallpaper",
        "image" to "Image"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f)
    ) {
        Text(
            text = "Background",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .semantics { heading() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(bottom = 24.dp)
        ) {
            options.forEach { (mode, label) ->
                val selected = mode == selectedMode
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        RadioButton(selected = selected, onClick = null)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onModeSelected(mode) }
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageBottomSheet(
    options: List<Pair<String, String>>,
    selectedLanguageTag: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f)
    ) {
        Text(
            text = stringResource(R.string.select_language),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .semantics { heading() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .selectableGroup(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(options, key = { (tag, _) -> tag }) { (tag, displayName) ->
                val selected = tag == selectedLanguageTag
                ListItem(
                    headlineContent = { Text(displayName) },
                    trailingContent = {
                        RadioButton(selected = selected, onClick = null)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onLanguageSelected(tag) }
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { Text(
                text = stringResource(R.string.customization),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            ) }, navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

@Preview
@Composable
private fun CustomizationPreview() {
    CustomizationScreen(EmptyDestinationsNavigator)
}
