package jobs.procrush.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = createAuthRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun refreshSession() {
        viewModelScope.launch {
            _isBusy.value = true
            _errorMessage.value = null
            try {
                val user = repository.fetchMe()
                _state.value =
                    when {
                        user == null -> AuthState.Unauthenticated
                        user.role == null -> AuthState.NeedsRegistration(user)
                        else -> AuthState.Authenticated(user)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось загрузить сессию"
                _state.value = AuthState.Unauthenticated
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun signInDev(email: String) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || !normalizedEmail.contains('@')) {
            _errorMessage.value = "Введите корректный адрес электронной почты"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _errorMessage.value = null
            try {
                val user = repository.devLogin(normalizedEmail)
                _state.value =
                    when {
                        user.role == null -> AuthState.NeedsRegistration(user)
                        else -> AuthState.Authenticated(user)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось войти"
                _state.value = AuthState.Unauthenticated
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun completeRegistration(role: UserRole) {
        val pending = _state.value as? AuthState.NeedsRegistration ?: return
        viewModelScope.launch {
            _isBusy.value = true
            _errorMessage.value = null
            try {
                val user = repository.completeRegistration(pending.user.email, role)
                _state.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось завершить регистрацию"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.logout()
            } catch (_: Exception) {
                // ignore
            } finally {
                _state.value = AuthState.Unauthenticated
                _isBusy.value = false
            }
        }
    }
}
