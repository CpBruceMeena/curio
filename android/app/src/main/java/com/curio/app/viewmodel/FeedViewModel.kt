package com.curio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null
)

class FeedViewModel : ViewModel() {

    private val repository = ContentRepository()

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadFeed()
    }

    fun loadFeed(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = page == 1)

            val result = repository.getFeed(
                page = page,
                pageSize = 10,
                categoryId = _uiState.value.selectedCategoryId
            )

            result.onSuccess { feedResponse ->
                val currentContent = if (page == 1) {
                    feedResponse.content
                } else {
                    _uiState.value.content + feedResponse.content
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

    fun selectCategory(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, content = emptyList())
        loadFeed()
    }

    fun likeContent(contentId: Long) {
        viewModelScope.launch {
            repository.likeContent(contentId).onSuccess { likes ->
                _uiState.value = _uiState.value.copy(
                    content = _uiState.value.content.map {
                        if (it.id == contentId) it.copy(likes = likes) else it
                    }
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
}
