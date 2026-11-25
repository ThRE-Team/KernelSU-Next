package com.rifsxd.ksunext.ui.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.LabelItemDefaults
import com.ramcosta.composedestinations.generated.destinations.AppProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SulogScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ksuApp
import com.rifsxd.ksunext.ui.LocalNavBarEnabled
import com.rifsxd.ksunext.ui.LocalScrollState
import com.rifsxd.ksunext.ui.rememberScrollConnection
import com.rifsxd.ksunext.ui.viewmodel.SuperUserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextSuperUserScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<SuperUserViewModel>()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val cardBorder = rememberNextBorder()

    val bottomBarScrollState = LocalScrollState.current
    val bottomBarScrollConnection = if (bottomBarScrollState != null) {
        rememberScrollConnection(
            isScrollingDown = bottomBarScrollState.isScrollingDown,
            scrollOffset = bottomBarScrollState.scrollOffset,
            previousScrollOffset = bottomBarScrollState.previousScrollOffset,
            threshold = 30f
        )
    } else null

    LaunchedEffect(navigator) {
        viewModel.search = ""
        if (viewModel.appList.isEmpty()) {
            viewModel.fetchAppList()
        }
    }

    Scaffold(
        topBar = {
            NextSearchTopBar(
                title = stringResource(R.string.superuser),
                searchText = viewModel.search,
                onSearchTextChange = { viewModel.search = it },
                onClearClick = { viewModel.search = "" },
                pillBorder = cardBorder,
                actions = {
                    IconButton(
                        onClick = { navigator.navigate(SulogScreenDestination) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = stringResource(R.string.settings_sulog),
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
                                text = { Text(stringResource(R.string.refresh)) },
                                onClick = {
                                    scope.launch { viewModel.fetchAppList() }
                                    showDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (viewModel.showSystemApps) {
                                            stringResource(R.string.hide_system_apps)
                                        } else {
                                            stringResource(R.string.show_system_apps)
                                        }
                                    )
                                },
                                onClick = {
                                    viewModel.updateShowSystemApps(!viewModel.showSystemApps)
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
            modifier = Modifier.padding(innerPadding),
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch { viewModel.fetchAppList() }
            },
            isRefreshing = viewModel.isRefreshing
        ) {
            val scrollState = LocalScrollState.current
            val navBarEnabled = LocalNavBarEnabled.current
            val isNavBarHidden = (scrollState?.isScrollingDown?.value ?: false) || (navBarEnabled?.value == false)
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                    if (isNavBarHidden) 0.dp else 112.dp

            LazyColumn(
                state = listState,
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 16.dp + navBarPadding
                )
            ) {
                items(
                    viewModel.appList.filter { it.displayIdentifier != ksuApp.packageName },
                    key = { it.displayIdentifier + it.uid }
                ) { app ->
                    NextAppItem(
                        app = app,
                        border = cardBorder,
                        onClickListener = {
                            navigator.navigate(AppProfileScreenDestination(app))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NextAppItem(
    app: SuperUserViewModel.AppInfo,
    border: BorderStroke?,
    onClickListener: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)

    NextCard(
        cornerRadius = 20.dp,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClickListener)
    ) {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Column {
                    Text(
                        text = app.displayIdentifier,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (app.allowSu) {
                            val rootLabel = if (developerOptionsEnabled) {
                                "ROOT | UID: ${app.uid}"
                            } else {
                                "ROOT"
                            }
                            LabelItem(text = rootLabel)
                        } else {
                            if (Natives.uidShouldUmount(app.uid)) {
                                LabelItem(
                                    text = "UMOUNT",
                                    style = LabelItemDefaults.style.copy(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                        if (app.hasCustomProfile) {
                            LabelItem(
                                text = "CUSTOM",
                                style = LabelItemDefaults.style.copy(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            )
                        } else if (!app.allowSu && !Natives.uidShouldUmount(app.uid)) {
                            LabelItem(
                                text = "DEFAULT",
                                style = LabelItemDefaults.style.copy(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            )
                        }
                    }
                }
            },
            leadingContent = {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(app.packageInfo)
                        .crossfade(true)
                        .build(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .padding(2.dp)
                        .size(46.dp)
                )
            },
        )
    }
}
