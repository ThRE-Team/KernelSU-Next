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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import coil.compose.AsyncImage
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MetaModuleScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.BuildConfig
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.LocalNavBarEnabled
import com.rifsxd.ksunext.ui.LocalScrollState
import com.rifsxd.ksunext.ui.rememberScrollConnection
import com.rifsxd.ksunext.ui.util.DownloadListener
import com.rifsxd.ksunext.ui.util.LocalSnackbarHost
import com.rifsxd.ksunext.ui.util.download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val NEXT_REPO_PREFS = "module_repo_prefs"
private const val NEXT_KEY_JSON_URLS = "json_urls"
private const val NEXT_KEY_NON_FREE = "non_free_enabled"
private val NEXT_DEFAULT_JSON_URLS = listOf(
    "https://raw.githubusercontent.com/KernelSU-Next/KernelSU-Next-Modules-Repo/refs/heads/main/modules.json",
    "https://raw.githubusercontent.com/ThRE-Team/Next-Manager-Modules-Repo/refs/heads/main/modules.json"
)
private const val NEXT_NON_FREE_JSON_URL = "https://raw.githubusercontent.com/KernelSU-Next/KernelSU-Next-Modules-Repo/refs/heads/main/non_free_modules.json"
private const val NEXT_URL_SEP = "|||"
private const val NEXT_REPO_TIMEOUT_MS = 15_000

private fun nextLoadJsonUrls(context: Context): List<String> {
    val raw = context.getSharedPreferences(NEXT_REPO_PREFS, Context.MODE_PRIVATE)
        .getString(NEXT_KEY_JSON_URLS, null)
    return if (raw.isNullOrBlank()) NEXT_DEFAULT_JSON_URLS
    else raw.split(NEXT_URL_SEP).filter { it.isNotBlank() }
}

private fun nextSaveJsonUrls(context: Context, urls: List<String>) {
    context.getSharedPreferences(NEXT_REPO_PREFS, Context.MODE_PRIVATE)
        .edit().putString(NEXT_KEY_JSON_URLS, urls.joinToString(NEXT_URL_SEP)).apply()
}

private fun nextLoadNonFree(context: Context): Boolean =
    context.getSharedPreferences(NEXT_REPO_PREFS, Context.MODE_PRIVATE)
        .getBoolean(NEXT_KEY_NON_FREE, false)

private fun nextSaveNonFree(context: Context, enabled: Boolean) {
    context.getSharedPreferences(NEXT_REPO_PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(NEXT_KEY_NON_FREE, enabled).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextModuleRepoScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val cardBorder = rememberNextBorder()

    val navBarEnabled = LocalNavBarEnabled.current
    val isNavBarHidden = (bottomBarScrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            if (isNavBarHidden) 0.dp else 112.dp

    var jsonUrls by remember { mutableStateOf(nextLoadJsonUrls(context)) }
    var nonFreeEnabled by remember { mutableStateOf(nextLoadNonFree(context)) }

    var showRepoManagerDialog by remember { mutableStateOf(false) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var pendingUrl by remember { mutableStateOf("") }

    var searchText by remember { mutableStateOf("") }
    var moduleState by remember { mutableStateOf<ModuleRepoState>(ModuleRepoState.Loading) }
    var downloadingModuleName by remember { mutableStateOf<String?>(null) }

    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var sortAToZ by remember { mutableStateOf(prefs.getBoolean("module_sort_a_to_z", true)) }
    var sortZToA by remember { mutableStateOf(prefs.getBoolean("module_sort_z_to_a", false)) }
    var sortStarsLowToHigh by remember { mutableStateOf(prefs.getBoolean("module_sort_stars_low_to_high", false)) }
    var sortStarsHighToLow by remember { mutableStateOf(prefs.getBoolean("module_sort_stars_high_to_low", false)) }

    DownloadListener(context) { uri ->
        downloadingModuleName = null
        navigator.navigate(
            FlashScreenDestination(FlashIt.FlashModules(listOf(uri)))
        )
    }

    suspend fun fetchModuleReposFromJson(jsonUrl: String): List<ModuleRepo> {
        return withContext(Dispatchers.IO) {
            try {
                val conn = URL(jsonUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = NEXT_REPO_TIMEOUT_MS
                conn.readTimeout = NEXT_REPO_TIMEOUT_MS
                conn.setRequestProperty("User-Agent", "KernelSU-Next/${BuildConfig.VERSION_CODE}")
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                conn.disconnect()

                val arr = JSONArray(text)
                val out = mutableListOf<ModuleRepo>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    out += ModuleRepo(
                        name = obj.optString("name"),
                        description = obj.optString("description"),
                        author = obj.optString("author"),
                        repoUrl = obj.optString("repoUrl"),
                        bannerLink = obj.optString("bannerUrl", ""),
                        license = obj.optString("license", ""),
                        visibility = obj.optInt("visibility", 1)
                    )
                }
                out
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun fetchNextRepoReleaseInfo(repoUrl: String): ReleaseInfoModuleRepo? {
        return withContext(Dispatchers.IO) {
            try {
                val stars = try {
                    val htmlConn = URL(repoUrl).openConnection() as HttpURLConnection
                    htmlConn.connectTimeout = NEXT_REPO_TIMEOUT_MS
                    htmlConn.readTimeout = NEXT_REPO_TIMEOUT_MS
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
                connection.connectTimeout = NEXT_REPO_TIMEOUT_MS
                connection.readTimeout = NEXT_REPO_TIMEOUT_MS
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "KernelSU-Next/${BuildConfig.VERSION_CODE}")

                val redirectUrl = connection.getHeaderField("Location")
                connection.disconnect()

                if (redirectUrl == null) return@withContext ReleaseInfoModuleRepo("null", "", stars)

                val tagMatch = """/tag/([^/?\s]+)""".toRegex()
                    .find(redirectUrl)?.groupValues?.get(1)
                    ?: return@withContext ReleaseInfoModuleRepo("null", "", stars)

                val releasePageUrl = "$repoUrl/releases/expanded_assets/$tagMatch"
                val pageConnection = URL(releasePageUrl).openConnection()
                pageConnection.connectTimeout = NEXT_REPO_TIMEOUT_MS
                pageConnection.readTimeout = NEXT_REPO_TIMEOUT_MS
                pageConnection.setRequestProperty("User-Agent", "KernelSU-Next/${BuildConfig.VERSION_CODE}")

                val pageHtml = BufferedReader(InputStreamReader(pageConnection.getInputStream())).use {
                    it.readText()
                }

                val zipUrlMatch = """href="(/[^"]+/releases/download/[^"]+\.zip)"""".toRegex()
                    .find(pageHtml)?.groupValues?.get(1)

                ReleaseInfoModuleRepo(
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
            moduleState = ModuleRepoState.Loading
            val effectiveUrls = if (nonFreeEnabled) jsonUrls + NEXT_NON_FREE_JSON_URL else jsonUrls
            val allModules = withContext(Dispatchers.IO) {
                effectiveUrls.map { url -> async { fetchModuleReposFromJson(url) } }
                    .awaitAll()
                    .flatten()
            }

            val seen = mutableSetOf<String>()
            val visibleModules = allModules
                .filter { it.visibility == 1 }
                .filter { module -> seen.add("${module.name}||${module.repoUrl}") }

            if (visibleModules.isEmpty()) {
                moduleState = ModuleRepoState.Error(context.getString(R.string.repo_no_modules))
                return
            }

            moduleState = ModuleRepoState.Success(
                visibleModules.map { it.copy(latestVersion = "", downloadUrl = "", stars = -1, isLoading = true) }
            )

            coroutineScope {
                visibleModules.forEach { baseModule ->
                    launch {
                        val releaseInfo = runCatching { fetchNextRepoReleaseInfo(baseModule.repoUrl) }.getOrNull()
                        val updated = baseModule.copy(
                            latestVersion = releaseInfo?.version ?: "null",
                            downloadUrl = releaseInfo?.zipUrl ?: "",
                            stars = releaseInfo?.stars ?: -1,
                            isLoading = false
                        )
                        val current = moduleState
                        if (current is ModuleRepoState.Success) {
                            val mutable = current.modules.toMutableList()
                            val idx = mutable.indexOfFirst { it.name == baseModule.name && it.repoUrl == baseModule.repoUrl }
                            if (idx >= 0) mutable[idx] = updated else mutable.add(updated)
                            moduleState = ModuleRepoState.Success(mutable)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            moduleState = ModuleRepoState.Error(e.message ?: context.getString(R.string.unknown_error))
        }
    }

    LaunchedEffect(jsonUrls, nonFreeEnabled) {
        scope.launch { loadModules() }
    }

    if (showRepoManagerDialog) {
        AlertDialog(
            onDismissRequest = { showRepoManagerDialog = false },
            title = { Text(stringResource(R.string.manage_repositories)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.non_free_repo),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.non_free_repo_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = nonFreeEnabled,
                            onCheckedChange = { enabled ->
                                nonFreeEnabled = enabled
                                nextSaveNonFree(context, enabled)
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (jsonUrls.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_repositories_configured),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        jsonUrls.forEachIndexed { index, url ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = {
                                        editingIndex = index
                                        pendingUrl = url
                                        showAddEditDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                                IconButton(
                                    onClick = {
                                        val updated = jsonUrls.toMutableList().also { it.removeAt(index) }
                                        jsonUrls = updated
                                        nextSaveJsonUrls(context, updated)
                                    },
                                    enabled = jsonUrls.size > 1
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            }
                            if (index < jsonUrls.lastIndex) HorizontalDivider()
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            editingIndex = null
                            pendingUrl = ""
                            showAddEditDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.add_repository))
                    }

                    TextButton(
                        onClick = {
                            jsonUrls = NEXT_DEFAULT_JSON_URLS
                            nextSaveJsonUrls(context, NEXT_DEFAULT_JSON_URLS)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.reset_to_default))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepoManagerDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddEditDialog = false
                pendingUrl = ""
            },
            title = { Text(stringResource(if (editingIndex == null) R.string.add_repository else R.string.repo_edit_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.repo_enter_json_url),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = pendingUrl,
                        onValueChange = { pendingUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        placeholder = { Text("https://...") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = pendingUrl.trim()
                        if (trimmed.isNotBlank()) {
                            val updated = jsonUrls.toMutableList()
                            val idx = editingIndex
                            if (idx == null) updated.add(trimmed) else updated[idx] = trimmed
                            jsonUrls = updated
                            nextSaveJsonUrls(context, updated)
                            showAddEditDialog = false
                            pendingUrl = ""
                        }
                    }
                ) {
                    Text(stringResource(if (editingIndex == null) R.string.add_repository else R.string.repo_apply))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddEditDialog = false
                        pendingUrl = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            NextSearchTopBar(
                title = stringResource(R.string.module_repo_screen),
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onClearClick = { searchText = "" },
                pillBorder = cardBorder,
                onBack = dropUnlessResumed { navigator.popBackStack() },
                actions = {
                    IconButton(
                        onClick = { showRepoManagerDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { navigator.navigate(MetaModuleScreenDestination) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SettingsSuggest,
                            contentDescription = stringResource(id = R.string.module_repo_screen),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                dropdownContent = {
                    var showDropdown by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showDropdown = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(id = R.string.settings),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.module_sort_a_to_z)) },
                                trailingIcon = { Checkbox(checked = sortAToZ, onCheckedChange = null) },
                                onClick = {
                                    sortAToZ = !sortAToZ
                                    sortZToA = false
                                    sortStarsLowToHigh = false
                                    sortStarsHighToLow = false
                                    prefs.edit()
                                        .putBoolean("module_sort_a_to_z", sortAToZ)
                                        .putBoolean("module_sort_z_to_a", false)
                                        .putBoolean("module_sort_stars_low_to_high", false)
                                        .putBoolean("module_sort_stars_high_to_low", false)
                                        .apply()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.module_sort_z_to_a)) },
                                trailingIcon = { Checkbox(checked = sortZToA, onCheckedChange = null) },
                                onClick = {
                                    sortZToA = !sortZToA
                                    sortAToZ = false
                                    sortStarsLowToHigh = false
                                    sortStarsHighToLow = false
                                    prefs.edit()
                                        .putBoolean("module_sort_z_to_a", sortZToA)
                                        .putBoolean("module_sort_a_to_z", false)
                                        .putBoolean("module_sort_stars_low_to_high", false)
                                        .putBoolean("module_sort_stars_high_to_low", false)
                                        .apply()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.module_sort_stars_low_to_high)) },
                                trailingIcon = { Checkbox(checked = sortStarsLowToHigh, onCheckedChange = null) },
                                onClick = {
                                    sortStarsLowToHigh = !sortStarsLowToHigh
                                    sortStarsHighToLow = false
                                    sortAToZ = false
                                    sortZToA = false
                                    prefs.edit()
                                        .putBoolean("module_sort_stars_low_to_high", sortStarsLowToHigh)
                                        .putBoolean("module_sort_stars_high_to_low", false)
                                        .putBoolean("module_sort_a_to_z", false)
                                        .putBoolean("module_sort_z_to_a", false)
                                        .apply()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.module_sort_stars_high_to_low)) },
                                trailingIcon = { Checkbox(checked = sortStarsHighToLow, onCheckedChange = null) },
                                onClick = {
                                    sortStarsHighToLow = !sortStarsHighToLow
                                    sortStarsLowToHigh = false
                                    sortAToZ = false
                                    sortZToA = false
                                    prefs.edit()
                                        .putBoolean("module_sort_stars_high_to_low", sortStarsHighToLow)
                                        .putBoolean("module_sort_stars_low_to_high", false)
                                        .putBoolean("module_sort_a_to_z", false)
                                        .putBoolean("module_sort_z_to_a", false)
                                        .apply()
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost, modifier = Modifier.padding(bottom = navBarPadding)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        when (val state = moduleState) {
            is ModuleRepoState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ModuleRepoState.Success -> {
                val filteredModules = if (searchText.isBlank()) {
                    state.modules
                } else {
                    state.modules.filter { module ->
                        module.name.contains(searchText, ignoreCase = true) ||
                                module.description.contains(searchText, ignoreCase = true) ||
                                module.author.contains(searchText, ignoreCase = true)
                    }
                }

                val sortedModules = when {
                    sortAToZ -> filteredModules.sortedBy { it.name.lowercase() }
                    sortZToA -> filteredModules.sortedByDescending { it.name.lowercase() }
                    sortStarsLowToHigh -> filteredModules.sortedWith(compareBy { if (it.stars < 0) Int.MAX_VALUE else it.stars })
                    sortStarsHighToLow -> filteredModules.sortedWith(compareByDescending { if (it.stars < 0) Int.MIN_VALUE else it.stars })
                    else -> filteredModules
                }

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
                            .let { modifier ->
                                if (bottomBarScrollConnection != null) {
                                    modifier
                                        .nestedScroll(bottomBarScrollConnection)
                                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                                } else {
                                    modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                                }
                            },
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 16.dp + navBarPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedModules) { module ->
                            val isThisModuleDownloading = downloadingModuleName == module.name
                            val isInstallEnabled = downloadingModuleName == null || isThisModuleDownloading

                            NextModuleRepoCard(
                                module = module,
                                isInstallEnabled = isInstallEnabled,
                                isDownloading = isThisModuleDownloading,
                                border = cardBorder,
                                onCardClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(module.repoUrl)))
                                },
                                onDownload = { selectedModule ->
                                    downloadingModuleName = selectedModule.name
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
                                            downloadingModuleName = null
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            is ModuleRepoState.Error -> {
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
private fun NextModuleRepoCard(
    module: ModuleRepo,
    isInstallEnabled: Boolean,
    isDownloading: Boolean,
    border: BorderStroke?,
    onCardClick: () -> Unit,
    onDownload: (ModuleRepo) -> Unit
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
                if (hasLicense || hasStars) {
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
                            text = stringResource(R.string.install),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
