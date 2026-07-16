package com.free2party.ui.screens.premium

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.free2party.data.billing.BillingManager
import com.free2party.data.billing.DurationType
import com.free2party.data.billing.PremiumPackage
import com.free2party.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PremiumUiState {
    object Loading : PremiumUiState
    data class Success(
        val packages: List<PremiumPackage>,
        val isPremium: Boolean,
        val isSandboxMode: Boolean,
        val isPurchasing: Boolean = false,
        val message: String? = null
    ) : PremiumUiState
    data class Error(val message: String) : PremiumUiState
}

sealed class PremiumUiEvent {
    object PurchaseSuccess : PremiumUiEvent()
    data class ShowError(val message: String) : PremiumUiEvent()
}

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val userRepository: UserRepository
) : ViewModel() {

    var gradientBackground by mutableStateOf(true)
        private set

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val uid = userRepository.currentUserId
            if (uid.isNotBlank()) {
                userRepository.observeUser(uid)
                    .catch { e ->
                        android.util.Log.e("PremiumViewModel", "Error observing user settings", e)
                    }
                    .collect { user ->
                        gradientBackground = user.settings.gradientBackground
                    }
            }
        }
    }

    private val _isPurchasing = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    private val _uiEvent = MutableSharedFlow<PremiumUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val uiState: StateFlow<PremiumUiState> = combine(
        billingManager.premiumPackages,
        billingManager.isPremium,
        billingManager.loadError,
        _isPurchasing,
        _message
    ) { packages, isPremium, loadError, isPurchasing, msg ->
        if (loadError != null) {
            PremiumUiState.Error(loadError)
        } else {
            // Filter out Lifetime packages from selection options shown to the user
            val visiblePackages = packages.filter { it.durationType != DurationType.LIFETIME }
            PremiumUiState.Success(
                packages = visiblePackages,
                isPremium = isPremium,
                isSandboxMode = billingManager.isSandboxMode,
                isPurchasing = isPurchasing,
                message = msg
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PremiumUiState.Loading
    )

    fun purchase(activity: Activity, pkg: PremiumPackage) {
        viewModelScope.launch {
            _isPurchasing.value = true
            _message.value = null
            billingManager.purchasePackage(activity, pkg)
                .onSuccess {
                    _uiEvent.emit(PremiumUiEvent.PurchaseSuccess)
                }
                .onFailure { error ->
                    _uiEvent.emit(PremiumUiEvent.ShowError(error.message ?: "Purchase failed"))
                }
            _isPurchasing.value = false
        }
    }

    fun restore() {
        viewModelScope.launch {
            _isPurchasing.value = true
            _message.value = null
            billingManager.restorePurchases()
                .onSuccess {
                    _uiEvent.emit(PremiumUiEvent.PurchaseSuccess)
                }
                .onFailure { error ->
                    _uiEvent.emit(PremiumUiEvent.ShowError(error.message ?: "Restore failed"))
                }
            _isPurchasing.value = false
        }
    }
}
