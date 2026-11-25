package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.BuildConfig
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.util.DownloadListener
import com.rifsxd.ksunext.ui.util.LocalSnackbarHost
import com.rifsxd.ksunext.ui.util.download
import com.rifsxd.ksunext.ui.viewmodel.ModuleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val NEXT_META_TIMEOUT_MS = 15_000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextMetaModuleScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val cardBorder = rememberNextBorder()

    val modulesJsonUrl = "https://raw.githubusercontent.com/KernelSU-Next/KernelSU-Next-Modules-Repo/refs/heads/main/meta_modules.json"

    var moduleState by remember { mutableStateOf<MetaModuleState>(MetaModuleState.Loading) }
    var downloadingModuleId by remember { mutableStateOf<String?>(null) }

    DownloadListener(context) { uri ->
        downloadingModuleId = null
        navigator.navigate(
            FlashScreenDestination(FlashIt.FlashModules(listOf(uri)))
        )
    }

    val moduleViewModel = viewModel<ModuleViewModel>()

    suspend fun fetchMetaModulesFromJson(jsonUrl: String): List<MetaModule>? {
        return withContext(Dispatchers.IO) {
            try {
                val conn = URL(jsonUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = NEXT_META_TIMEOUT_MS
                conn.readTimeout = NEXT_META_TIMEOUT_MS
                conn.setRequestProperty("User-Agent", "KernelSU-Next/${BuildConfig.VERSION_CODE}")
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                conn.disconnect()

                val arr = JSONArray(text)
                val out = mutableListOf<MetaModule>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    out += MetaModule(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        description = obj.optString("description"),
                        author = obj.optString("author"),
                        repoUrl = obj.optString("repoUrl"),
                        bannerLink = obj.optString("bannerUrl"),
                        license = obj.optString("license", ""),
                        visibility = obj.optInt("visibility", 1)
                    )
                }
                out
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun fetchNextMetaReleaseInfo(repoUrl: String): ReleaseInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val stars = try {
                    val htmlConn = URL(repoUrl).openConnection() as HttpURLConnection
                    htmlConn.connectTimeout = NEXT_META_TIMEOUT_MS
                    htmlConn.readTimeout = NEXT_META_TIMEOUT_MS
                    htmlConn.setRequestProperty("User-Agent", "KernelSU-Next/${BuildConfig.VERSION_CODE}")
                    htmlConn.setRequestProperty("Accept", "text/html")
                    val html = BufferedReader(InputStreamReader(htmlConn.inputStream)).use { it.readText() }
                    htmlConn.disconnect()
                    val titleMatch = """id="repo-stars-counter-star"[^>]*title="([^"]+)"""".toRegex()
                        .find(html)?.groupValues?.get(1)
                    titleMatch?.replace(",", "")?.trim()?.toIntOrNull() ?: -1
                } catch (_: Exception) {
                    -1
                }

                val latestUrl = "$repoUrl/releases/latest"
                val connection = URL(latestUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = NEXT_META_TIMEOUT_MS
                connection.readTimeout = NEXT_META_TIMEOUT_MS
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "KernelSU-Next/${BuildConfig.VERSION_CODE}")

                val redirectUrl = connection.getHeaderField("Location")
                connection.disconnect()

                if (redirectUrl == null) return@withContext ReleaseInfo("null", "", stars)

                val tagMatch = """/tag/([^/?\s]+)""".toRegex()
                    .find(redirectUrl)?.groupValues?.get(1)
                    ?: return@withContext ReleaseInfo("null", "", stars)

                val releasePageUrl = "$repoUrl/releases/expanded_assets/$tagMatch"
                val pageConnection = URL(releasePageUrl).openConnection()
                pageConnection.connectTimeout = NEXT_META_TIMEOUT_MS
                pageConnection.readTimeout = NEXT_META_TIMEOUT_MS
                pageConnection.setRequestProperty("User-Agent", "KernelSU-Next/${BuildConfig.VERSION_CODE}")

                val pageHtml = BufferedReader(InputStreamReader(pageConnection.getInputStream())).use {
                    it.readText()
                }

                val zipUrlMatch = """href="(/[^"]+/releases/download/[^"]+\.zip)"""".toRegex()
                    .find(pageHtml)?.groupValues?.get(1)

                ReleaseInfo(
                    version = tagMatch,
                    zipUrl = if (zipUrlMatch != null) "https://github.com$zipUrlMatch" else "",
                    stars = stars
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun loadModules() {
        try {
            moduleState = MetaModuleState.Loading
            val baseList = withContext(Dispatchers.IO) { fetchMetaModulesFromJson(modulesJsonUrl) }

            if (baseList == null) {
                moduleState = MetaModuleState.Error(context.getString(R.string.metamodule_load_failed))
                return
            }

            val visibleModules = baseList.filter { it.visibility == 1 }
            moduleState = MetaModuleState.Success(
                visibleModules.map { it.copy(latestVersion = "", downloadUrl = "", stars = -1, isLoading = true) }
            )

            coroutineScope {
                visibleModules.forEach { baseModule ->
                    launch {
                        val releaseInfo = runCatching { fetchNextMetaReleaseInfo(baseModule.repoUrl) }.getOrNull()
                        val updated = baseModule.copy(
                            latestVersion = releaseInfo?.version ?: "null",
                            downloadUrl = releaseInfo?.zipUrl ?: "",
                            stars = releaseInfo?.stars ?: -1,
                            isLoading = false
                        )
                        val current = moduleState
                        if (current is MetaModuleState.Success) {
                            val mutable = current.modules.toMutableList()
                            val idx = mutable.indexOfFirst { it.id == baseModule.id }
                            if (idx >= 0) mutable[idx] = updated else mutable.add(updated)
                            moduleState = MetaModuleState.Success(mutable)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            moduleState = MetaModuleState.Error(e.message ?: context.getString(R.string.unknown_error))
        }
    }

    LaunchedEffect(Unit) {
        scope.launch { loadModules() }
    }

    Scaffold(
        topBar = {
            NextMetaModuleTopBar(
                title = stringResource(R.string.meta_module_screen),
                pillBorder = cardBorder,
                onBack = dropUnlessResumed { navigator.popBackStack() },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        when (val state = moduleState) {
            is MetaModuleState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is MetaModuleState.Success -> {
                val installedIds = moduleViewModel.moduleList.map { it.id }
                val hasInstalledMetaModule = state.modules.any { installedIds.contains(it.id) }
                val sortedModules = state.modules.sortedBy { it.name.lowercase() }

                PullToRefreshBox(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    isRefreshing = false,
                    onRefresh = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { loadModules() }
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedModules) { module ->
                            val isInstalled = installedIds.contains(module.id)
                            val isThisModuleDownloading = downloadingModuleId == module.id
                            val isInstallEnabled = when {
                                downloadingModuleId != null && !isThisModuleDownloading -> false
                                isInstalled -> true
                                hasInstalledMetaModule -> false
                                else -> true
                            }

                            NextMetaModuleCard(
                                module = module,
                                isInstalled = isInstalled,
                                isInstallEnabled = isInstallEnabled,
                                isDownloading = isThisModuleDownloading,
                                border = cardBorder,
                                onCardClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(module.repoUrl)))
                                },
                                onDownload = { selectedModule ->
                                    downloadingModuleId = selectedModule.id
                                    scope.launch {
                                        try {
                                            val fileName = "${selectedModule.name.replace(" ", "_")}_${selectedModule.latestVersion.replace("/", "_")}.zip"
                                            download(
                                                context,
                                                selectedModule.downloadUrl,
                                                fileName,
                                                context.getString(R.string.repo_download_progress, selectedModule.name)
                                            )
                                        } catch (e: Exception) {
                                            snackBarHost.showSnackbar(context.getString(R.string.repo_download_error, e.message.orEmpty()))
                                            downloadingModuleId = null
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            is MetaModuleState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.error_loading_modules),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = { scope.launch { loadModules() } },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextMetaModuleCard(
    module: MetaModule,
    isInstalled: Boolean,
    isInstallEnabled: Boolean,
    isDownloading: Boolean,
    border: BorderStroke?,
    onCardClick: () -> Unit,
    onDownload: (MetaModule) -> Unit
) {
    NextCard(
        cornerRadius = 22.dp,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onCardClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val useBanner = prefs.getBoolean("use_banner", true)

            if (useBanner && module.bannerLink.isNotEmpty()) {
                val isDark = isSystemInDarkTheme()
                val colorScheme = MaterialTheme.colorScheme
                val amoledMode = prefs.getBoolean("amoled_mode", false)
                val isDynamic = colorScheme.primary != colorScheme.secondary
                val fadeColor = when {
                    amoledMode && isDark -> Color.Black
                    isDynamic -> colorScheme.surface
                    isDark -> Color(0xFF222222)
                    else -> Color.White
                }

                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (module.bannerLink.startsWith("http", true)) {
                        AsyncImage(
                            model = module.bannerLink,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.18f
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fadeColor.copy(alpha = 0.0f),
                                        fadeColor.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "by ${module.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )

                val hasLicense = module.license.isNotEmpty()
                val hasStars = !module.isLoading && module.stars >= 0
                if (hasLicense || hasStars || isInstalled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasLicense) {
                            LabelItem(
                                icon = {
                                    Icon(
                                        tint = LabelItemDefaults.style.contentColor,
                                        imageVector = Icons.Filled.LocalOffer,
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(
                                        text = module.license,
                                        style = LabelItemDefaults.style.textStyle.copy(
                                            color = LabelItemDefaults.style.contentColor
                                        )
                                    )
                                }
                            )
                        }
                        if (isInstalled) {
                            LabelItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(
                                        text = stringResource(R.string.installed),
                                        style = LabelItemDefaults.style.textStyle.copy(
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                },
                                style = LabelItemDefaults.style.copy(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                        if (hasStars) {
                            val starsText = if (module.stars >= 1_000) "${"%.1f".format(module.stars / 1000.0)}k" else "${module.stars}"
                            LabelItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(
                                        text = starsText,
                                        style = LabelItemDefaults.style.textStyle.copy(
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                },
                                style = LabelItemDefaults.style.copy(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.latest_version),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = if (module.latestVersion.isNotEmpty()) module.latestVersion else stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { onDownload(module) },
                        modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                        enabled = module.downloadUrl.isNotEmpty() && isInstallEnabled,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = if (isInstalled) stringResource(R.string.reinstall) else stringResource(R.string.install),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextMetaModuleTopBar(
    title: String,
    pillBorder: BorderStroke?,
    onBack: () -> Unit,
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
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}
