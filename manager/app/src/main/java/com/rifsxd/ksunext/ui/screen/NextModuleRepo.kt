package com.rifsxd.ksunext.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.ConfirmResult
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.component.rememberLoadingDialog
import com.rifsxd.ksunext.ui.util.DownloadListener
import com.rifsxd.ksunext.ui.util.download
import com.rifsxd.ksunext.ui.util.formatSize
import com.rifsxd.ksunext.ui.viewmodel.ModuleRepoViewModel
import com.rifsxd.ksunext.ui.viewmodel.ModuleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextModuleRepoScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<ModuleRepoViewModel>()
    val installedViewModel = viewModel<ModuleViewModel>()
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val cardBorder = rememberNextBorder()

    LaunchedEffect(Unit) {
        viewModel.sortByName = prefs.getBoolean("module_repo_sort_name", true)
        viewModel.sortByUpdated = prefs.getBoolean("module_repo_sort_updated", false)
        viewModel.sortByStars = prefs.getBoolean("module_repo_sort_stars", false)

        if (viewModel.modules.isEmpty()) {
            viewModel.fetchModules()
        }
        if (installedViewModel.moduleList.isEmpty()) {
            installedViewModel.fetchModuleList()
        }
    }

    val installedIds = remember(installedViewModel.moduleList) {
        installedViewModel.moduleList.map { it.id }.toSet()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog()

    val onInstallModule: (Uri) -> Unit = { uri ->
        navigator.navigate(FlashScreenDestination(FlashIt.FlashModules(listOf(uri))))
        installedViewModel.markNeedRefresh()
    }

    DownloadListener(context, onInstallModule)

    Scaffold(
        topBar = {
            NextSearchTopBar(
                title = stringResource(R.string.module_repo_screen),
                searchText = viewModel.search,
                onSearchTextChange = { viewModel.search = it },
                onClearClick = { viewModel.search = "" },
                pillBorder = cardBorder,
                onBack = dropUnlessResumed { navigator.popBackStack() },
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
                                text = { Text(stringResource(R.string.module_repo_sort_name)) },
                                trailingIcon = { Checkbox(checked = viewModel.sortByName, onCheckedChange = null) },
                                onClick = {
                                    viewModel.sortByName = true
                                    viewModel.sortByUpdated = false
                                    viewModel.sortByStars = false
                                    prefs.edit {
                                        putBoolean("module_repo_sort_name", true)
                                        putBoolean("module_repo_sort_updated", false)
                                        putBoolean("module_repo_sort_stars", false)
                                    }
                                    showDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.module_repo_sort_updated)) },
                                trailingIcon = { Checkbox(checked = viewModel.sortByUpdated, onCheckedChange = null) },
                                onClick = {
                                    viewModel.sortByName = false
                                    viewModel.sortByUpdated = true
                                    viewModel.sortByStars = false
                                    prefs.edit {
                                        putBoolean("module_repo_sort_name", false)
                                        putBoolean("module_repo_sort_updated", true)
                                        putBoolean("module_repo_sort_stars", false)
                                    }
                                    showDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.module_repo_sort_stars)) },
                                trailingIcon = { Checkbox(checked = viewModel.sortByStars, onCheckedChange = null) },
                                onClick = {
                                    viewModel.sortByName = false
                                    viewModel.sortByUpdated = false
                                    viewModel.sortByStars = true
                                    prefs.edit {
                                        putBoolean("module_repo_sort_name", false)
                                        putBoolean("module_repo_sort_updated", false)
                                        putBoolean("module_repo_sort_stars", true)
                                    }
                                    showDropdown = false
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        val haptic = LocalHapticFeedback.current
        PullToRefreshBox(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            isRefreshing = viewModel.isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.fetchModules()
            }
        ) {
            val modules = viewModel.modules
            when {
                viewModel.error != null && modules.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.module_repo_load_failed),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            FilledTonalButton(onClick = { viewModel.fetchModules() }) {
                                Text(stringResource(R.string.module_repo_retry))
                            }
                        }
                    }
                }
                modules.isEmpty() && !viewModel.isRefreshing -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.module_repo_empty),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 12.dp,
                            end = 16.dp,
                            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                    ) {
                        items(modules, key = { it.id }) { module ->
                            NextRepoModuleItem(
                                module = module,
                                isInstalled = installedIds.contains(module.id),
                                border = cardBorder,
                                onInstall = {
                                    scope.launch {
                                        val releaseInfo = loadingDialog.withLoading {
                                            withContext(Dispatchers.IO) {
                                                viewModel.fetchLatestRelease(module)
                                            }
                                        }

                                        if (releaseInfo == null) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.module_repo_no_release),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@launch
                                        }

                                        val sizeStr = if (releaseInfo.assetSize > 0) " (${formatSize(releaseInfo.assetSize)})" else ""
                                        val confirmContent = buildString {
                                            append("**${module.name}** — `${releaseInfo.version}`$sizeStr\n\n")
                                            if (releaseInfo.changelog.isNotBlank()) {
                                                append(releaseInfo.changelog)
                                            }
                                        }

                                        val result = confirmDialog.awaitConfirm(
                                            title = stringResource(R.string.module_install),
                                            content = confirmContent,
                                            markdown = true,
                                            confirm = stringResource(R.string.install),
                                            dismiss = stringResource(android.R.string.cancel)
                                        )

                                        if (result != ConfirmResult.Confirmed) return@launch

                                        val fileName = releaseInfo.assetName.ifBlank {
                                            "${module.id}-${releaseInfo.version}.zip"
                                        }
                                        val downloadingText = context.getString(R.string.module_downloading, module.name)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.module_start_downloading, module.name),
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        withContext(Dispatchers.IO) {
                                            download(
                                                context = context,
                                                url = releaseInfo.downloadUrl,
                                                fileName = fileName,
                                                description = downloadingText,
                                                onDownloaded = onInstallModule,
                                                onDownloading = {
                                                    launch(Dispatchers.Main) {
                                                        Toast.makeText(context, downloadingText, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextRepoModuleItem(
    module: ModuleRepoViewModel.RepoModule,
    isInstalled: Boolean,
    border: BorderStroke?,
    onInstall: () -> Unit,
) {
    val updatedDate = remember(module.updatedAt) {
        if (module.updatedAt.isNotBlank()) {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(module.updatedAt)
                if (date != null) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                } else ""
            } catch (_: Exception) {
                module.updatedAt.take(10)
            }
        } else ""
    }

    NextCard(
        cornerRadius = 20.dp,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onInstall() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (module.author.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.module_author)}: ${module.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (updatedDate.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.module_repo_updated)}: $updatedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    modifier = Modifier.defaultMinSize(52.dp, 36.dp),
                    onClick = onInstall,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = if (isInstalled) Icons.Filled.CloudDownload else Icons.Outlined.Download,
                        contentDescription = stringResource(R.string.install)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInstalled) stringResource(R.string.module_reinstall) else stringResource(R.string.install),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (module.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 4
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LabelItem(text = "★ ${module.stars}")

                if (module.latestVersion.isNotBlank()) {
                    LabelItem(
                        text = module.latestVersion,
                        style = LabelItemDefaults.style.copy(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }

                if (isInstalled) {
                    LabelItem(
                        text = stringResource(R.string.installed),
                        style = LabelItemDefaults.style.copy(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }
        }
    }
}
