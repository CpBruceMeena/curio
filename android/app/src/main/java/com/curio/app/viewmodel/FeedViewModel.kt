package com.curio.app.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.CurioApp
import com.curio.app.data.model.Category
import com.curio.app.data.model.Content
import com.curio.app.data.model.CommentEntry
import com.curio.app.data.model.L1Group
import com.curio.app.data.repository.ContentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val isLoading: Boolean = false,
    val content: List<Content> = emptyList(),
    val discoverContent: List<Content> = emptyList(),
    val bookmarkedContent: List<Content> = emptyList(),
    val categories: List<Category> = emptyList(),
    val l1Groups: List<L1Group> = emptyList(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val bookmarkedIds: Set<Long> = emptySet(),
    val likedIds: Set<Long> = emptySet(),
    val feedStartIndex: Int? = null,
    val lastFeedPosition: Int = 0,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null,

    // Comment state per content item (key = contentId)
    val comments: Map<Long, List<CommentEntry>> = emptyMap(),
    val commentsLoading: Set<Long> = emptySet(),
    val submittingComment: Boolean = false
)

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContentRepository(application)
    private val prefs = (application as CurioApp).prefs

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // Survives navigation - keeps Discover/Feed tab state
    var showDiscover by mutableStateOf(false)
    var showBookmarks by mutableStateOf(false)

    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null

    init {
        loadCategories()
        loadL1Groups()
        loadFeed()
        loadDiscoverContent()
        loadBookmarkedIds()
        // Run a sync on init to backfill any bookmarks that were saved while offline
        viewModelScope.launch { syncBookmarkedContent() }
        // Populate offline content cache (25 items per L1 category) on first launch
        viewModelScope.launch { populateOfflineCacheIfNeeded() }
        // Register connectivity listener for automatic backfill when coming online
        registerConnectivityListener()
    }

    override fun onCleared() {
        super.onCleared()
        connectivityCallback?.let {
            val cm = getApplication<Application>().getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(it)
        }
    }

    /**
     * Register a network callback that triggers bookmark backfill when connectivity is restored.
     */
    private fun registerConnectivityListener() {
        val cm = getApplication<Application>().getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Device just came online — backfill any bookmarks that were cached as IDs only
                viewModelScope.launch { syncBookmarkedContent() }
            }
        }
        connectivityCallback = callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
    }

    fun selectCategory(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryIds = if (categoryId != null) setOf(categoryId) else emptySet(),
            content = emptyList(),
            feedStartIndex = null
        )
        loadFeed()
    }

    fun setSelectedCategoryIds(ids: Set<Long>) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryIds = ids.toSet(),
            content = emptyList(),
            feedStartIndex = null
        )
        loadFeed()
    }

    fun toggleCategorySelection(categoryId: Long) {
        val current = _uiState.value.selectedCategoryIds.toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        _uiState.value = _uiState.value.copy(
            selectedCategoryIds = current,
            content = emptyList(),
            feedStartIndex = null
        )
        loadFeed()
    }

    fun clearCategorySelection() {
        _uiState.value = _uiState.value.copy(
            selectedCategoryIds = emptySet(),
            content = emptyList(),
            feedStartIndex = null
        )
        loadFeed()
    }

    /**
     * Navigate to a specific content item in the feed pager.
     * Finds the content in feed or discoverContent, loads it into the feed if needed,
     * and sets feedStartIndex so the pager scrolls to it.
     */
    fun navigateToContent(contentId: Long) {
        val state = _uiState.value

        // Check if content is already in the feed list
        val existingIndex = state.content.indexOfFirst { it.id == contentId }
        if (existingIndex >= 0) {
            _uiState.value = state.copy(feedStartIndex = existingIndex)
            return
        }

        // Check if content is in discover content
        val discoverItem = state.discoverContent.find { it.id == contentId }
        if (discoverItem != null) {
            // Prepend it to the feed and start at index 0
            _uiState.value = state.copy(
                content = listOf(discoverItem) + state.content,
                feedStartIndex = 0
            )
            return
        }

        // Otherwise fetch it from the API
        viewModelScope.launch {
            repository.getContent(contentId).onSuccess { item ->
                val currentState = _uiState.value
                _uiState.value = currentState.copy(
                    content = listOf(item) + currentState.content,
                    feedStartIndex = 0
                )
            }
        }
    }

    fun clearFeedStartIndex() {
        _uiState.value = _uiState.value.copy(feedStartIndex = null)
    }

    fun loadFeed(page: Int = 1) {
        val selectedIds = _uiState.value.selectedCategoryIds

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = page == 1 && _uiState.value.content.isEmpty())

            // If multiple categories selected, load from each
            val results = if (selectedIds.isNotEmpty()) {
                val allContent = mutableListOf<Content>()
                var latestPage = 1
                var latestHasMore = true
                var latestTotal = 0L

                for (catId in selectedIds) {
                    repository.getFeed(
                        page = page,
                        pageSize = 50,
                        categoryId = catId,
                        random = true
                    ).let { result ->
                        result.onSuccess { response ->
                            allContent.addAll(response.content)
                            latestPage = response.page
                            latestHasMore = response.hasMore
                            latestTotal = response.total
                        }
                    }
                }

                if (allContent.isNotEmpty()) {
                    Result.success(
                        com.curio.app.data.model.FeedResponse(
                            content = allContent.shuffled(),
                            page = latestPage,
                            pageSize = 50,
                            total = latestTotal,
                            hasMore = latestHasMore
                        )
                    )
                } else {
                    repository.getFeed(page = page, pageSize = 100, random = true)
                }
            } else {
                repository.getFeed(
                    page = page,
                    pageSize = 100,
                    random = true
                )
            }

            results.onSuccess { feedResponse ->
                val userInterests = prefs.selectedCategories
                val filteredContent = if (selectedIds.isNotEmpty()) {
                    // Explicit category selection via Discover - trust the API filter
                    feedResponse.content
                } else if (userInterests.isNotEmpty()) {
                    // No explicit category selection - filter by onboarding interests
                    feedResponse.content.filter { item ->
                        userInterests.any { interest ->
                            item.categoryName.equals(interest, ignoreCase = true)
                        }
                    }
                } else {
                    feedResponse.content
                }

                val currentContent = if (page == 1) {
                    filteredContent
                } else {
                    _uiState.value.content + filteredContent
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    content = currentContent,
                    currentPage = feedResponse.page,
                    hasMore = feedResponse.hasMore,
                    error = null
                )
            }.onFailure { error ->
                // On failure, fall back to offline cache
                val cachedContent = repository.loadOfflineCache().shuffled()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    content = if (cachedContent.isNotEmpty()) cachedContent else _uiState.value.content,
                    error = if (cachedContent.isNotEmpty()) null else (error.message ?: "Failed to load feed")
                )
            }
        }
    }

    fun loadMore() {
        if (!_uiState.value.hasMore || _uiState.value.isLoading) return
        loadFeed(page = _uiState.value.currentPage + 1)
    }

    fun loadDiscoverContent() {
        viewModelScope.launch {
            repository.getFeed(
                page = 1,
                pageSize = 4,
                random = true
            ).onSuccess { response ->
                _uiState.value = _uiState.value.copy(discoverContent = response.content.take(4))
            }
        }
    }

    /**
     * Refresh discover content and categories — called every time user opens Discover.
     */
    fun refreshDiscover() {
        loadDiscoverContent()
        loadCategories()
        loadL1Groups()
    }

    // ── Feed Position Caching ──────────────────────────────

    /**
     * Save the current feed pager position so it can be restored when
     * switching back from discover/bookmarks.
     */
    fun saveFeedPosition(page: Int) {
        if (_uiState.value.content.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(lastFeedPosition = page.coerceIn(0, _uiState.value.content.size - 1))
        }
    }

    /**
     * Restore the last feed position when returning to the feed.
     */
    fun restoreFeedPosition() {
        val state = _uiState.value
        if (state.lastFeedPosition > 0 && state.lastFeedPosition < state.content.size) {
            _uiState.value = state.copy(feedStartIndex = state.lastFeedPosition)
        }
    }

    // ── Bookmarks ─────────────────────────────────────────────

    /**
     * Toggle bookmark for a content item. Returns true if bookmarked, false if removed.
     * The full content is saved locally so it can be viewed offline.
     */
    fun toggleBookmark(contentId: Long): Boolean {
        val wasBookmarked = prefs.isBookmarked(contentId)
        val isNowBookmarked = prefs.toggleBookmark(contentId)
        val updatedIds = prefs.bookmarkedContentIds
        _uiState.value = _uiState.value.copy(bookmarkedIds = updatedIds)

        // Save or remove the full content locally for offline access
        viewModelScope.launch {
            if (isNowBookmarked) {
                // Find the Content object from the current state lists
                val state = _uiState.value
                val content = state.content.find { it.id == contentId }
                    ?: state.discoverContent.find { it.id == contentId }
                    ?: state.bookmarkedContent.find { it.id == contentId }
                if (content != null) {
                    repository.saveBookmarkLocally(content)
                } else {
                    // Fallback: fetch from API
                    repository.getContent(contentId).onSuccess { item ->
                        repository.saveBookmarkLocally(item)
                    }
                }
            } else {
                repository.removeBookmarkLocally(contentId)
                // Also remove from local bookmarkedContent state list
                val state = _uiState.value
                _uiState.value = state.copy(
                    bookmarkedContent = state.bookmarkedContent.filter { it.id != contentId }
                )
            }
        }
        return isNowBookmarked
    }

    fun isBookmarked(contentId: Long): Boolean {
        // Prefer the live state set over SharedPreferences for reactive UI
        return _uiState.value.bookmarkedIds.contains(contentId)
    }

    fun loadBookmarkedIds() {
        _uiState.value = _uiState.value.copy(bookmarkedIds = prefs.bookmarkedContentIds)
    }

    fun getBookmarkedIds(): Set<Long> {
        return prefs.bookmarkedContentIds
    }

    /**
     * Sync: fetch all bookmarked IDs that are NOT yet cached in the local database.
     * Runs silently (no loading spinner) so it can be called from connectivity listener.
     */
    suspend fun syncBookmarkedContent() {
        val bookmarkedIds = prefs.bookmarkedContentIds
        if (bookmarkedIds.isEmpty()) return

        val localContent = repository.getBookmarkedContentLocally()
        val localIds = localContent.map { it.id }.toSet()
        val missingIds = bookmarkedIds - localIds

        if (missingIds.isEmpty()) return

        coroutineScope {
            // Fetch missing content from API
            val deferredResults = missingIds.map { id ->
                async { repository.getContent(id) }
            }
            val apiContent = deferredResults.mapNotNull { deferred ->
                deferred.await().getOrNull()
            }

            if (apiContent.isEmpty()) return@coroutineScope

            // Cache newly fetched content locally
            apiContent.forEach { item ->
                repository.saveBookmarkLocally(item)
            }

            // Update the bookmarkedContent state if the Bookmarks screen is visible
            val current = _uiState.value.bookmarkedContent
            _uiState.value = _uiState.value.copy(
                bookmarkedContent = current + apiContent
            )
        }
    }

    /**
     * Load all bookmarked content from local storage (offline-first).
     * Falls back to API for content not yet cached locally.
     */
    fun loadBookmarkedContent() {
        val bookmarkedIds = prefs.bookmarkedContentIds
        if (bookmarkedIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(bookmarkedContent = emptyList())
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // First, load from local database (fully offline)
            val localContent = repository.getBookmarkedContentLocally()
            val localIds = localContent.map { it.id }.toSet()

            // Find which bookmarked IDs are NOT yet cached locally
            val missingIds = bookmarkedIds - localIds

            if (missingIds.isNotEmpty()) {
                // Fetch missing content from API
                val deferredResults = missingIds.map { id ->
                    async { repository.getContent(id) }
                }
                val apiContent = deferredResults.mapNotNull { deferred ->
                    deferred.await().getOrNull()
                }

                // Cache newly fetched content locally for future offline access
                apiContent.forEach { item ->
                    repository.saveBookmarkLocally(item)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    bookmarkedContent = localContent + apiContent
                )
            } else {
                // All content is available locally — no API needed
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    bookmarkedContent = localContent
                )
            }
        }
    }

    /**
     * Reload bookmarked content (used after toggle to refresh the bookmarks view).
     */
    fun refreshBookmarks() {
        loadBookmarkedContent()
    }

    // ── Shuffle ────────────────────────────────────────────────

    /**
     * Shuffle: fetch fresh random content from ALL categories.
     * Clears any category filter so the user sees variety across topics.
     */
    fun shuffleFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                content = emptyList(),
                selectedCategoryIds = emptySet(),
                feedStartIndex = 0,
                lastFeedPosition = 0
            )

            repository.getFeed(
                page = 1,
                pageSize = 100,
                categoryId = null,
                random = true
            ).onSuccess { feedResponse ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    content = feedResponse.content,
                    currentPage = feedResponse.page,
                    hasMore = feedResponse.hasMore,
                    feedStartIndex = 0,
                    error = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to shuffle"
                )
            }
        }
    }

    /**
     * Populate the offline cache on first launch — fetches ~25 items per L1 category
     * and stores them locally. Only runs once.
     */
    suspend fun populateOfflineCacheIfNeeded() {
        if (repository.isCachePopulated()) return

        val l1Names = listOf("Facts", "Poems", "Short Stories", "Puzzles")
        val categoryMap = _uiState.value.l1Groups.associateBy { it.name }

        coroutineScope {
            val deferred = l1Names.map { l1Name ->
                async {
                    // Get subcategory IDs for this L1 group
                    val group = categoryMap[l1Name]
                    if (group == null || group.categories.isEmpty()) return@async

                    // Fetch ~25 items across all subcategories
                    val allContent = mutableListOf<Content>()
                    val catIds = group.categories.map { it.id }.shuffled().take(5)

                    for (catId in catIds) {
                        val remaining = 25 - allContent.size
                        if (remaining <= 0) break
                        repository.getFeed(page = 1, pageSize = remaining, categoryId = catId, random = true)
                            .onSuccess { response ->
                                allContent.addAll(response.content.take(remaining))
                            }
                    }

                    // Shuffle and save to cache
                    val items = allContent.shuffled().take(25)
                    if (items.isNotEmpty()) {
                        repository.saveToOfflineCache(items, l1Name)
                    }
                }
            }
            deferred.forEach { it.await() }
        }
    }

    fun loadL1Feed(categoryIds: Set<Long>?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, content = emptyList(), lastFeedPosition = 0)

            val results = if (!categoryIds.isNullOrEmpty()) {
                val allContent = mutableListOf<Content>()
                for (catId in categoryIds) {
                    repository.getFeed(page = 1, pageSize = 100, categoryId = catId, random = true)
                        .onSuccess { allContent.addAll(it.content) }
                }
                if (allContent.isNotEmpty()) {
                    Result.success<com.curio.app.data.model.FeedResponse>(
                        com.curio.app.data.model.FeedResponse(
                            content = allContent.shuffled(), page = 1, pageSize = 100,
                            total = allContent.size.toLong(), hasMore = false
                        )
                    )
                } else {
                    repository.getFeed(page = 1, pageSize = 100, random = true)
                }
            } else {
                repository.getFeed(page = 1, pageSize = 100, random = true)
            }

            results.onSuccess { feedResponse ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false, content = feedResponse.content,
                    hasMore = feedResponse.hasMore, error = null,
                    feedStartIndex = 0
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = error.message ?: "Failed to load"
                )
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().onSuccess { response ->
                _uiState.value = _uiState.value.copy(categories = response.categories)
            }
        }
    }

    private fun loadL1Groups() {
        viewModelScope.launch {
            repository.getL1Categories().onSuccess { response ->
                _uiState.value = _uiState.value.copy(l1Groups = response.groups)
            }
        }
    }

    // ── Likes ──────────────────────────────────────────────────

    /**
     * Toggle like/unlike for a content item.
     * Updates likedIds set locally and calls the backend to adjust the count.
     */
    fun toggleLike(contentId: Long) {
        val currentLiked = _uiState.value.likedIds.contains(contentId)
        val action = if (currentLiked) "unlike" else "like"

        // Optimistic update: like state flips immediately
        val updatedLikedIds = if (currentLiked) {
            _uiState.value.likedIds - contentId
        } else {
            _uiState.value.likedIds + contentId
        }

        // Optimistically update the likes count in the content list too
        val updatedContent = _uiState.value.content.map { item ->
            if (item.id == contentId) {
                val delta = if (currentLiked) -1 else 1
                item.copy(likes = (item.likes + delta).coerceAtLeast(0))
            } else item
        }

        _uiState.value = _uiState.value.copy(
            likedIds = updatedLikedIds,
            content = updatedContent
        )

        // Backend call
        viewModelScope.launch {
            repository.likeContent(contentId, action).onSuccess { newLikes ->
                // Sync with actual backend count
                val syncedContent = _uiState.value.content.map { item ->
                    if (item.id == contentId) item.copy(likes = newLikes) else item
                }
                _uiState.value = _uiState.value.copy(content = syncedContent)
            }.onFailure {
                // Revert on failure
                val revertedLiked = if (currentLiked) {
                    _uiState.value.likedIds + contentId
                } else {
                    _uiState.value.likedIds - contentId
                }
                val revertedContent = _uiState.value.content.map { item ->
                    if (item.id == contentId) {
                        val delta = if (currentLiked) 1 else -1
                        item.copy(likes = (item.likes + delta).coerceAtLeast(0))
                    } else item
                }
                _uiState.value = _uiState.value.copy(
                    likedIds = revertedLiked,
                    content = revertedContent
                )
            }
        }
    }

    fun isLiked(contentId: Long): Boolean {
        return _uiState.value.likedIds.contains(contentId)
    }

    // ── Audio / TTS ────────────────────────────────────────────
    var playingContentId by mutableStateOf<Long?>(null)
    var audioFilePath by mutableStateOf<String?>(null)
    var isAudioLoading by mutableStateOf(false)
    var autoPlayEnabled by mutableStateOf(false)

    fun toggleAutoPlay() {
        autoPlayEnabled = !autoPlayEnabled
        if (!autoPlayEnabled) {
            // Stop any playing audio when disabling auto-play
            playingContentId = null
            audioFilePath = null
        }
    }

    /**
     * Generate and cache audio for a content item.
     * Sets audioFilePath on success so the UI can play it.
     */
    fun playAudio(contentId: Long) {
        // If already playing this content, stop
        if (playingContentId == contentId) {
            playingContentId = null
            audioFilePath = null
            return
        }

        isAudioLoading = true
        playingContentId = contentId

        viewModelScope.launch {
            repository.generateSpeech(contentId).onSuccess { file ->
                audioFilePath = file.absolutePath
                isAudioLoading = false
            }.onFailure {
                audioFilePath = null
                playingContentId = null
                isAudioLoading = false
            }
        }
    }

    // ── Comments ───────────────────────────────────────────────

    /**
     * Load comments for a content item.
     */
    fun loadComments(contentId: Long) {
        // Mark as loading
        _uiState.value = _uiState.value.copy(
            commentsLoading = _uiState.value.commentsLoading + contentId
        )

        viewModelScope.launch {
            repository.getComments(contentId).onSuccess { response ->
                val updatedComments = _uiState.value.comments.toMutableMap()
                updatedComments[contentId] = response.comments
                _uiState.value = _uiState.value.copy(
                    comments = updatedComments,
                    commentsLoading = _uiState.value.commentsLoading - contentId
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    commentsLoading = _uiState.value.commentsLoading - contentId
                )
            }
        }
    }

    /**
     * Add a comment to a content item.
     */
    fun addComment(contentId: Long, text: String, email: String = "") {
        _uiState.value = _uiState.value.copy(submittingComment = true)

        viewModelScope.launch {
            repository.addComment(contentId, text, email, prefs.deviceUuid).onSuccess { response ->
                if (response.success && response.comment != null) {
                    // Append locally for instant UI update
                    val currentComments = _uiState.value.comments.toMutableMap()
                    val existing = currentComments[contentId]?.toMutableList() ?: mutableListOf()
                    existing.add(response.comment)
                    currentComments[contentId] = existing
                    _uiState.value = _uiState.value.copy(
                        comments = currentComments,
                        submittingComment = false
                    )
                } else {
                    // Re-fetch to sync
                    loadComments(contentId)
                    _uiState.value = _uiState.value.copy(submittingComment = false)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(submittingComment = false)
            }
        }
    }

    /**
     * Get comments for a specific content item from the local state.
     */
    fun getCommentsFor(contentId: Long): List<CommentEntry> {
        return _uiState.value.comments[contentId] ?: emptyList()
    }
}
