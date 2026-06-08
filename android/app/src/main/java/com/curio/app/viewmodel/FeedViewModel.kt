package com.curio.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.CurioApp
import com.curio.app.data.model.Category
import com.curio.app.data.model.Content
import com.curio.app.data.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val isLoading: Boolean = true,
    val content: List<Content> = emptyList(),
    val discoverContent: List<Content> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val feedStartIndex: Int? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null
)

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContentRepository()
    private val prefs = (application as CurioApp).prefs

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadFeed()
        loadDiscoverContent()
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
            _uiState.value = _uiState.value.copy(isLoading = page == 1)

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
                pageSize = 8,
                random = true
            ).onSuccess { response ->
                _uiState.value = _uiState.value.copy(discoverContent = response.content.take(8))
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
}
