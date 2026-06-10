package com.curio.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.CurioApp
import com.curio.app.data.model.Category
import com.curio.app.data.model.Content
import com.curio.app.data.model.L1Group
import com.curio.app.data.repository.ContentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class FeedUiState(
    val isLoading: Boolean = false,
    val content: List<Content> = emptyList(),
    val discoverContent: List<Content> = emptyList(),
    val bookmarkedContent: List<Content> = emptyList(),
    val categories: List<Category> = emptyList(),
    val l1Groups: List<L1Group> = emptyList(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val bookmarkedIds: Set<Long> = emptySet(),
    val feedStartIndex: Int? = null,
    val lastFeedPosition: Int = 0,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null
)

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContentRepository()
    private val prefs = (application as CurioApp).prefs

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // Survives navigation - keeps Discover/Feed tab state
    var showDiscover by mutableStateOf(false)
    var showBookmarks by mutableStateOf(false)

    init {
        loadCategories()
        loadL1Groups()
        loadFeed()
        loadDiscoverContent()
        loadBookmarkedIds()
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to load feed"
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
     */
    fun toggleBookmark(contentId: Long): Boolean {
        val isNowBookmarked = prefs.toggleBookmark(contentId)
        // Update the live bookmarkedIds set in state to trigger recomposition
        val updatedIds = prefs.bookmarkedContentIds
        _uiState.value = _uiState.value.copy(bookmarkedIds = updatedIds)
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
     * Load all bookmarked content from the API.
     */
    fun loadBookmarkedContent() {
        val bookmarkedIds = prefs.bookmarkedContentIds
        if (bookmarkedIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(bookmarkedContent = emptyList())
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val deferredResults = bookmarkedIds.map { id ->
                async { repository.getContent(id) }
            }

            val loadedContent = deferredResults.mapNotNull { deferred ->
                deferred.await().getOrNull()
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                bookmarkedContent = loadedContent
            )
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
     * Shuffle: fetch fresh random content for the currently selected category.
     */
    fun shuffleFeed() {
        val selectedIds = _uiState.value.selectedCategoryIds
        val currentCategoryName = _uiState.value.content.firstOrNull()?.categoryName

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, content = emptyList(), feedStartIndex = 0, lastFeedPosition = 0)

            // Find the category ID for the current category name
            val targetCategoryId = if (selectedIds.isNotEmpty()) {
                selectedIds.first()
            } else if (currentCategoryName != null) {
                val cat = _uiState.value.categories.find {
                    it.name.equals(currentCategoryName, ignoreCase = true)
                }
                cat?.id
            } else {
                null
            }

            repository.getFeed(
                page = 1,
                pageSize = 100,
                categoryId = targetCategoryId,
                random = true
            ).onSuccess { feedResponse ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    content = feedResponse.content,
                    currentPage = feedResponse.page,
                    hasMore = feedResponse.hasMore,
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
}
