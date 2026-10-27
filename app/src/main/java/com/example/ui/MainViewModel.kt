package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.BacaDuluDatabase
import com.example.data.model.PostWithSite
import com.example.data.model.SiteEntity
import com.example.data.repository.ArchiveRepository
import com.example.data.repository.BackupInspection
import com.example.ui.reader.ReaderFont
import com.example.ui.reader.ReaderSettings
import com.example.ui.reader.ReaderThemePreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object SearchBookmark : Screen()
    object ManageSites : Screen()
    data class Reader(val postId: Int) : Screen()
}

enum class SearchBookmarkTab {
    SEARCH,
    BOOKMARKS,
    HISTORY
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BacaDuluDatabase.getInstance(application)
    private val repository = ArchiveRepository(database, database.bacaDuluDao())

    // Navigation stack
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Random 15 posts
    private val _randomPosts = MutableStateFlow<List<PostWithSite>>(emptyList())
    val randomPosts: StateFlow<List<PostWithSite>> = _randomPosts.asStateFlow()

    private val _isLoadingRandom = MutableStateFlow(false)
    val isLoadingRandom: StateFlow<Boolean> = _isLoadingRandom.asStateFlow()

    // Search and Bookmarks and History
    val searchQuery = MutableStateFlow("")
    val searchTab = MutableStateFlow(SearchBookmarkTab.SEARCH)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<PostWithSite>> = searchQuery
        .flatMapLatest { query -> repository.searchPosts(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedPosts: StateFlow<List<PostWithSite>> = repository.bookmarkedPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingHistory: StateFlow<List<PostWithSite>> = repository.readingHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Manage Sites
    val allSites: StateFlow<List<SiteEntity>> = repository.allSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPostsCount: StateFlow<Int> = repository.totalPostsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSitesCount: StateFlow<Int> = repository.totalSitesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Import & Inspection state
    private val _pendingInspection = MutableStateFlow<BackupInspection?>(null)
    val pendingInspection: StateFlow<BackupInspection?> = _pendingInspection.asStateFlow()

    private val _isProcessingImport = MutableStateFlow(false)
    val isProcessingImport: StateFlow<Boolean> = _isProcessingImport.asStateFlow()

    private val _importProgress = MutableStateFlow<Pair<Int, Int>?>(null) // (current, total)
    val importProgress: StateFlow<Pair<Int, Int>?> = _importProgress.asStateFlow()

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    // SnackBar / Toast alerts
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Reader state
    private val _activePostId = MutableStateFlow<Int?>(null)
    val activePostId: StateFlow<Int?> = _activePostId.asStateFlow()

    private val _currentPost = MutableStateFlow<PostWithSite?>(null)
    val currentPost: StateFlow<PostWithSite?> = _currentPost.asStateFlow()

    private val _prevPostId = MutableStateFlow<Int?>(null)
    val prevPostId: StateFlow<Int?> = _prevPostId.asStateFlow()

    private val _nextPostId = MutableStateFlow<Int?>(null)
    val nextPostId: StateFlow<Int?> = _nextPostId.asStateFlow()

    private val _showRawContent = MutableStateFlow(false)
    val showRawContent: StateFlow<Boolean> = _showRawContent.asStateFlow()

    private val _readerSettings = MutableStateFlow(ReaderSettings.DEFAULT)
    val readerSettings: StateFlow<ReaderSettings> = _readerSettings.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    init {
        refreshRandomPosts()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.Reader) {
            loadPostDetails(screen.postId)
        }
    }

    fun navigateBack() {
        if (_currentScreen.value is Screen.Reader) {
            _currentScreen.value = Screen.Home
            refreshRandomPosts()
        } else if (_currentScreen.value != Screen.Home) {
            _currentScreen.value = Screen.Home
        }
    }

    fun refreshRandomPosts() {
        viewModelScope.launch {
            _isLoadingRandom.value = true
            try {
                val posts = repository.getRandom15Posts()
                _randomPosts.value = posts
            } finally {
                _isLoadingRandom.value = false
            }
        }
    }

    fun openPost(postId: Int) {
        navigateTo(Screen.Reader(postId))
    }

    private fun loadPostDetails(postId: Int) {
        _activePostId.value = postId
        _showRawContent.value = false
        viewModelScope.launch {
            repository.recordPostRead(postId)
            val post = repository.getPost(postId)
            _currentPost.value = post
            if (post != null) {
                _prevPostId.value = repository.getPreviousPostId(post.siteId, post.id)
                _nextPostId.value = repository.getNextPostId(post.siteId, post.id)
            } else {
                _prevPostId.value = null
                _nextPostId.value = null
            }
        }
    }

    fun goToPreviousPost() {
        _prevPostId.value?.let { prevId ->
            loadPostDetails(prevId)
        }
    }

    fun goToNextPost() {
        _nextPostId.value?.let { nextId ->
            loadPostDetails(nextId)
        }
    }

    fun toggleBookmark(postId: Int, currentlyBookmarked: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(postId, currentlyBookmarked)
            // Update active post if reading
            if (_activePostId.value == postId) {
                _currentPost.value = _currentPost.value?.let {
                    val newStatus = if (currentlyBookmarked) 0 else 1
                    it.copy(
                        isBookmarked = newStatus,
                        bookmarkedAt = if (newStatus == 1) System.currentTimeMillis() else null
                    )
                }
            }
            // Update random posts in place
            _randomPosts.value = _randomPosts.value.map { item ->
                if (item.id == postId) {
                    val newStatus = if (currentlyBookmarked) 0 else 1
                    item.copy(
                        isBookmarked = newStatus,
                        bookmarkedAt = if (newStatus == 1) System.currentTimeMillis() else null
                    )
                } else item
            }
            val msg = if (currentlyBookmarked) "Dihapus dari bookmark" else "Disimpan ke bookmark"
            _userMessage.emit(msg)
        }
    }

    // --- Import Flow ---

    fun onFileSelectedForImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessingImport.value = true
            _importStatusMessage.value = "Membuka dan memeriksa file backup..."
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _userMessage.emit("Gagal membaca file dari penyimpanan.")
                    return@launch
                }

                val inspection = repository.inspectBackupStream(inputStream) { count, stage ->
                    _importStatusMessage.value = "$stage ($count pos)"
                }

                if (inspection.incomingPosts.isEmpty()) {
                    _userMessage.emit("Tidak ditemukan data pos WordPress pada file ini.")
                    return@launch
                }

                if (inspection.existingSite != null) {
                    // Show comparison dialog
                    _pendingInspection.value = inspection
                } else {
                    // Direct import for new site
                    executeImportFinal(inspection, overwrite = false)
                }
            } catch (e: Exception) {
                _userMessage.emit("Gagal mengimpor file: ${e.localizedMessage ?: "Format tidak didukung"}")
            } finally {
                _isProcessingImport.value = false
                _importStatusMessage.value = null
            }
        }
    }

    fun loadDemoSite() {
        viewModelScope.launch {
            _isProcessingImport.value = true
            _importStatusMessage.value = "Menyiapkan data situs contoh..."
            try {
                val inspection = repository.inspectDemoData()
                if (inspection.existingSite != null) {
                    // Show comparison dialog
                    _pendingInspection.value = inspection
                } else {
                    executeImportFinal(inspection, overwrite = false)
                }
            } catch (e: Exception) {
                _userMessage.emit("Gagal memuat data contoh: ${e.message}")
            } finally {
                _isProcessingImport.value = false
                _importStatusMessage.value = null
            }
        }
    }

    fun confirmImportOverwrite() {
        val inspection = _pendingInspection.value ?: return
        _pendingInspection.value = null
        viewModelScope.launch {
            executeImportFinal(inspection, overwrite = true)
        }
    }

    fun dismissInspectionDialog() {
        _pendingInspection.value = null
        viewModelScope.launch {
            _userMessage.emit("Impor dibatalkan.")
        }
    }

    private suspend fun executeImportFinal(inspection: BackupInspection, overwrite: Boolean) {
        _isProcessingImport.value = true
        _importStatusMessage.value = "Menyimpan ke database lokal..."
        _importProgress.value = Pair(0, inspection.incomingPosts.size)
        try {
            val totalSaved = repository.executeImport(
                siteUrl = inspection.siteUrl,
                posts = inspection.incomingPosts,
                overwrite = overwrite
            ) { count, total ->
                _importProgress.value = Pair(count, total)
                _importStatusMessage.value = "Menyimpan pos ($count/$total)..."
            }
            _userMessage.emit("Berhasil mengimpor $totalSaved pos untuk ${inspection.siteUrl}")
            refreshRandomPosts()
        } catch (e: Exception) {
            _userMessage.emit("Gagal menyimpan ke database: ${e.localizedMessage}")
        } finally {
            _isProcessingImport.value = false
            _importProgress.value = null
            _importStatusMessage.value = null
        }
    }

    fun deleteSite(siteId: Int, siteUrl: String) {
        viewModelScope.launch {
            try {
                repository.deleteSite(siteId)
                _userMessage.emit("Situs $siteUrl dan seluruh posnya telah dihapus.")
                refreshRandomPosts()
            } catch (e: Exception) {
                _userMessage.emit("Gagal menghapus situs: ${e.localizedMessage}")
            }
        }
    }

    // --- Export Database Flow ---

    fun exportDatabase(context: Context) {
        viewModelScope.launch {
            _importStatusMessage.value = "Mengekspor file database..."
            val result = repository.exportDatabaseToDownloads(context)
            result.onSuccess { message ->
                _userMessage.emit("Ekspor berhasil! $message")
            }.onFailure { error ->
                _userMessage.emit("Gagal mengekspor: ${error.localizedMessage}")
            }
            _importStatusMessage.value = null
        }
    }

    // --- Reader Settings Controls ---

    fun toggleRawContent() {
        _showRawContent.value = !_showRawContent.value
    }

    fun toggleSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun updateFontSize(delta: Float) {
        val current = _readerSettings.value.fontSizeSp
        val newSize = (current + delta).coerceIn(14f, 32f)
        _readerSettings.value = _readerSettings.value.copy(fontSizeSp = newSize)
    }

    fun setFontSize(size: Float) {
        _readerSettings.value = _readerSettings.value.copy(fontSizeSp = size.coerceIn(14f, 32f))
    }

    fun setFont(font: ReaderFont) {
        _readerSettings.value = _readerSettings.value.copy(font = font)
    }

    fun setThemePreset(preset: ReaderThemePreset) {
        _readerSettings.value = _readerSettings.value.copy(
            themePreset = preset,
            customBackgroundColor = null,
            customTextColor = null
        )
    }

    fun clearReadingHistory() {
        viewModelScope.launch {
            repository.clearReadingHistory()
            _userMessage.emit("Histori baca berhasil dibersihkan.")
        }
    }

    fun setTextColor(color: Color) {
        _readerSettings.value = _readerSettings.value.copy(
            customTextColor = color
        )
    }

    fun setCustomColors(bg: Color?, text: Color?) {
        _readerSettings.value = _readerSettings.value.copy(
            customBackgroundColor = bg,
            customTextColor = text
        )
    }

    fun resetReaderSettings() {
        _readerSettings.value = ReaderSettings.DEFAULT
    }
}
