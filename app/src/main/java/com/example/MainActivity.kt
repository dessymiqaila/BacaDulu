package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageHistory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.DuplicateCompareDialog
import com.example.ui.home.HomeScreen
import com.example.ui.manage.ManageSitesScreen
import com.example.ui.reader.PostReaderScreen
import com.example.ui.search.SearchBookmarkScreen
import com.example.ui.theme.BacaDuluTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemDark) }

            BacaDuluTheme(darkTheme = isDarkMode) {
                BacaDuluApp(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacaDuluApp(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val randomPosts by viewModel.randomPosts.collectAsStateWithLifecycle()
    val isLoadingRandom by viewModel.isLoadingRandom.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchTab by viewModel.searchTab.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val bookmarkedPosts by viewModel.bookmarkedPosts.collectAsStateWithLifecycle()
    val readingHistory by viewModel.readingHistory.collectAsStateWithLifecycle()

    val allSites by viewModel.allSites.collectAsStateWithLifecycle()
    val totalPostsCount by viewModel.totalPostsCount.collectAsStateWithLifecycle()

    val pendingInspection by viewModel.pendingInspection.collectAsStateWithLifecycle()
    val isProcessingImport by viewModel.isProcessingImport.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val importStatusMessage by viewModel.importStatusMessage.collectAsStateWithLifecycle()

    val currentPost by viewModel.currentPost.collectAsStateWithLifecycle()
    val prevPostId by viewModel.prevPostId.collectAsStateWithLifecycle()
    val nextPostId by viewModel.nextPostId.collectAsStateWithLifecycle()
    val showRawContent by viewModel.showRawContent.collectAsStateWithLifecycle()
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()

    // File picker launcher for .db.gz / .sql.gz / .sql
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onFileSelectedForImport(context, uri)
        }
    }

    // Handle user snackbar messages
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Hardware/System back navigation
    BackHandler(enabled = currentScreen != Screen.Home) {
        viewModel.navigateBack()
    }

    // Duplicate detection comparison dialog
    pendingInspection?.let { inspection ->
        DuplicateCompareDialog(
            inspection = inspection,
            onConfirmOverwrite = { viewModel.confirmImportOverwrite(context) },
            onDismiss = { viewModel.dismissInspectionDialog() }
        )
    }

    if (currentScreen is Screen.Reader) {
        // Full screen immersive reader view
        PostReaderScreen(
            post = currentPost,
            prevPostId = prevPostId,
            nextPostId = nextPostId,
            readerSettings = readerSettings,
            showRawContent = showRawContent,
            showSettingsDialog = showSettingsDialog,
            onBackClick = { viewModel.navigateBack() },
            onPrevClick = { viewModel.goToPreviousPost() },
            onNextClick = { viewModel.goToNextPost() },
            onBookmarkToggle = { id, bookmarked -> viewModel.toggleBookmark(id, bookmarked) },
            onToggleRawContent = { viewModel.toggleRawContent() },
            onToggleSettings = { show -> viewModel.toggleSettingsDialog(show) },
            onFontSelected = { font -> viewModel.setFont(font) },
            onFontSizeDelta = { delta -> viewModel.updateFontSize(delta) },
            onFontSizeSet = { size -> viewModel.setFontSize(size) },
            onThemeSelected = { preset -> viewModel.setThemePreset(preset) },
            onTextColorSelected = { color -> viewModel.setTextColor(color) },
            onResetSettings = { viewModel.resetReaderSettings() }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                is Screen.Home -> "BacaDulu"
                                is Screen.SearchBookmark -> "Cari & Bookmark"
                                is Screen.ManageSites -> "Kelola Arsip"
                                else -> "BacaDulu"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier.testTag("theme_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                                contentDescription = "Ubah Mode Gelap/Terang"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home,
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                        label = { Text("Beranda") },
                        modifier = Modifier.testTag("nav_item_home")
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.SearchBookmark,
                        onClick = { viewModel.navigateTo(Screen.SearchBookmark) },
                        icon = { Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null) },
                        label = { Text("Cari & Simpan") },
                        modifier = Modifier.testTag("nav_item_search")
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.ManageSites,
                        onClick = { viewModel.navigateTo(Screen.ManageSites) },
                        icon = { Icon(imageVector = Icons.Default.ManageHistory, contentDescription = null) },
                        label = { Text("Kelola Arsip") },
                        modifier = Modifier.testTag("nav_item_manage")
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    is Screen.Home -> {
                        HomeScreen(
                            randomPosts = randomPosts,
                            isLoading = isLoadingRandom,
                            onRefreshRandom = { viewModel.refreshRandomPosts() },
                            onPostClick = { postId -> viewModel.openPost(postId) },
                            onBookmarkToggle = { id, bookmarked -> viewModel.toggleBookmark(id, bookmarked) },
                            onNavigateToManage = { viewModel.navigateTo(Screen.ManageSites) },
                            onLoadDemoData = { viewModel.loadDemoSite() }
                        )
                    }
                    is Screen.SearchBookmark -> {
                        SearchBookmarkScreen(
                            currentTab = searchTab,
                            onTabSelected = { tab -> viewModel.searchTab.value = tab },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { q -> viewModel.searchQuery.value = q },
                            searchResults = searchResults,
                            bookmarkedPosts = bookmarkedPosts,
                            readingHistory = readingHistory,
                            onClearHistory = { viewModel.clearReadingHistory() },
                            onPostClick = { postId -> viewModel.openPost(postId) },
                            onBookmarkToggle = { id, bookmarked -> viewModel.toggleBookmark(id, bookmarked) }
                        )
                    }
                    is Screen.ManageSites -> {
                        ManageSitesScreen(
                            sites = allSites,
                            totalPosts = totalPostsCount,
                            isProcessing = isProcessingImport,
                            progressPair = importProgress,
                            statusMessage = importStatusMessage,
                            onPickFileToImport = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/gzip",
                                        "application/x-gzip",
                                        "application/octet-stream",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                            },
                            onLoadDemoData = { viewModel.loadDemoSite() },
                            onExportDatabase = { viewModel.exportDatabase(context) },
                            onDeleteSite = { id, url -> viewModel.deleteSite(id, url) }
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}
