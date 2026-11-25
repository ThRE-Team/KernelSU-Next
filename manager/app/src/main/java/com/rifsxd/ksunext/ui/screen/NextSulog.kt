package com.rifsxd.ksunext.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.screen.sulog.*
import com.rifsxd.ksunext.ui.util.SulogEntry
import com.rifsxd.ksunext.ui.util.SulogEventFilter
import com.rifsxd.ksunext.ui.viewmodel.SulogViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NextSulogScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<SulogViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val cardBorder = rememberNextBorder()

    LaunchedEffect(Unit) {
        viewModel.refreshLatest()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    var selectedEntry by remember { mutableStateOf<SulogEntry?>(null) }
    var showFileMenu by remember { mutableStateOf(false) }
    val fileSelector = buildSulogFileSelector(uiState.files, uiState.selectedFilePath)

    if (selectedEntry != null) {
        Dialog(onDismissRequest = { selectedEntry = null }) {
            NextCard(
                cornerRadius = 24.dp,
                border = cardBorder,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = sulogEntryTitle(selectedEntry!!),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SelectionContainer {
                            Text(
                                text = sulogEntryDetailText(selectedEntry!!),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedEntry = null }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            NextSearchTopBar(
                title = stringResource(R.string.settings_sulog),
                searchText = uiState.searchText,
                onSearchTextChange = viewModel::setSearchText,
                onClearClick = { viewModel.setSearchText("") },
                pillBorder = cardBorder,
                onBack = dropUnlessResumed { navigator.popBackStack() },
                actions = {
                    IconButton(
                        onClick = viewModel::cleanFile,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = stringResource(R.string.sulog_clean_title),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                dropdownContent = {
                    var showFilterMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showFilterMenu = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(id = R.string.settings),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                        ) {
                            SulogEventFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(sulogFilterLabel(filter)) },
                                    trailingIcon = {
                                        Checkbox(
                                            checked = filter in uiState.selectedFilters,
                                            onCheckedChange = null,
                                        )
                                    },
                                    onClick = { viewModel.toggleFilter(filter) },
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.refreshLatest(refreshing = true)
            },
            isRefreshing = uiState.isRefreshing
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                    val statusWarning: Pair<String, Boolean>? = when (uiState.sulogStatus) {
                        "unsupported" -> stringResource(R.string.sulog_unsupported_title) to false
                        "managed" -> stringResource(R.string.feature_status_managed_summary) to false
                        "supported" if !uiState.isSulogEnabled -> stringResource(R.string.sulog_disabled_title) to true
                        else -> null
                    }

                    if (statusWarning != null) {
                        item {
                            NextCard(
                                cornerRadius = 20.dp,
                                border = cardBorder,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = statusWarning.first,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    if (statusWarning.second) {
                                        Button(
                                            onClick = viewModel::enableSulog,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            )
                                        ) {
                                            Text(stringResource(R.string.sulog_enable_action))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.files.isNotEmpty()) {
                        item {
                            NextCard(
                                cornerRadius = 20.dp,
                                border = cardBorder,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showFileMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.sulog_log_files),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = fileSelector.items.getOrNull(fileSelector.selectedIndex) ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                        DropdownMenu(
                                            expanded = showFileMenu,
                                            onDismissRequest = { showFileMenu = false }
                                        ) {
                                            uiState.files.forEachIndexed { index, file ->
                                                DropdownMenuItem(
                                                    text = { Text(fileSelector.items[index]) },
                                                    onClick = {
                                                        viewModel.refresh(file.path)
                                                        showFileMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    when {
                        uiState.errorMessage != null -> item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(R.string.sulog_failed_to_load),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = uiState.errorMessage!!,
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        uiState.visibleEntries.isEmpty() -> item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.sulog_no_log_entries),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        else -> {
                            itemsIndexed(
                                items = uiState.visibleEntries,
                                key = { index, entry -> "$index-${entry.key}" }
                            ) { _, entry ->
                                NextSulogEntryCard(
                                    entry = entry,
                                    border = cardBorder,
                                    onClick = { selectedEntry = entry }
                                )
                            }
                        }
                    }
                }

                if (uiState.isLoading || uiState.isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NextSulogEntryCard(
    entry: SulogEntry,
    border: BorderStroke?,
    onClick: () -> Unit
) {
    NextCard(
        cornerRadius = 20.dp,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = sulogEntryTitle(entry),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            sulogEntryDescription(entry)?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entry.timestampText?.let { ts ->
                    LabelItem(
                        text = ts,
                        style = LabelItemDefaults.style.copy(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                sulogEntryStatus(entry)?.let { status ->
                    val isSuccess = status == "Success"
                    LabelItem(
                        text = status,
                        style = LabelItemDefaults.style.copy(
                            containerColor = if (isSuccess)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (isSuccess)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
                sulogEntrySummaryTags(entry).forEach { tag ->
                    LabelItem(
                        text = tag,
                        style = LabelItemDefaults.style.copy(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }
}
