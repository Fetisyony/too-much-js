package com.ba.randomtraining.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ba.randomtraining.data.model.JasonSearchResultItem
import com.ba.randomtraining.data.repository.FetchError
import com.ba.randomtraining.data.repository.TenorRequestResult
import com.ba.randomtraining.data.utils.RetrofitTenorInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class ErrorStatus(
    val seen: Boolean,  // whether an alert was already shown to the user
    val fetchError: FetchError
)

class MainViewModel : ViewModel() {
    private val tenorRepository = RetrofitTenorInstance.tenorRepository

    private val _jasonItems = mutableStateListOf<JasonSearchResultItem>()
    val jasonItems: SnapshotStateList<JasonSearchResultItem> get() = _jasonItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorStatus = MutableStateFlow(ErrorStatus(true, FetchError.Ok))
    val errorStatus: StateFlow<ErrorStatus> = _errorStatus.asStateFlow()

    init {
        fetchJason()
    }

    fun markErrorAsSeen() {
        _errorStatus.value = _errorStatus.value.copy(seen = true)
    }

    fun fetchJason(refresh: Boolean = false) {
        viewModelScope.launch {
            if (isLoading.value || errorStatus.value.fetchError == FetchError.NoDataLeftError) return@launch

            _isLoading.value = true

            val newJasonItems: TenorRequestResult
            if (refresh) {
                newJasonItems = tenorRepository.getJasonsInitial()
                _jasonItems.clear()
                _isRefreshing.value = true
            } else
                newJasonItems = tenorRepository.getJasonsNext()

            when (newJasonItems) {
                is TenorRequestResult.Success -> {
                    _jasonItems.addAll(newJasonItems.gifs)
                    _errorStatus.value = ErrorStatus(false, FetchError.Ok)
                }
                is TenorRequestResult.Error -> {
                    _errorStatus.value = ErrorStatus(false, newJasonItems.fetchError)
                }
                is TenorRequestResult.Empty -> {
                    _errorStatus.value = ErrorStatus(true, FetchError.NoDataLeftError)
                }
            }
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }
}
