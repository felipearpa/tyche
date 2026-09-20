package com.felipearpa.tyche.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.account.AvatarUrl
import com.felipearpa.tyche.session.CurrentAccountCoordinator
import com.felipearpa.tyche.session.CurrentAccountRefreshTrigger
import com.felipearpa.tyche.session.avatar.application.UploadAvatar
import com.felipearpa.ui.state.LoadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ProfileAvatarSource {
    data class Local(val bitmap: Bitmap) : ProfileAvatarSource()
    data class Remote(val url: String) : ProfileAvatarSource()
    data object Letter : ProfileAvatarSource()
}

class ProfileViewModel(
    private val uploadAvatar: UploadAvatar,
    private val currentAccountCoordinator: CurrentAccountCoordinator,
) : ViewModel() {
    val accountId: StateFlow<String> = currentAccountCoordinator.state
        .map { bundle -> bundle?.accountId.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = currentAccountCoordinator.state.value?.accountId.orEmpty(),
        )

    val username: StateFlow<String> = currentAccountCoordinator.state
        .map { bundle -> bundle?.username.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = currentAccountCoordinator.state.value?.username.orEmpty(),
        )

    val email: StateFlow<String> = currentAccountCoordinator.state
        .map { bundle -> bundle?.email.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = currentAccountCoordinator.state.value?.email.orEmpty(),
        )

    private val _uploadState = MutableStateFlow<LoadState<Unit>>(LoadState.Idle)
    val uploadState: StateFlow<LoadState<Unit>> = _uploadState.asStateFlow()

    private var pendingUpload: Pair<Bitmap, ByteArray>? = null

    /**
     * The uploading photo shows optimistically; a failed upload falls back to what was there
     * before, and an account without a photo falls back to the letter avatar. After a
     * successful upload the remote branch is already served from the seeded shared store, so
     * no view-local copy is retained.
     */
    val avatarSource: StateFlow<ProfileAvatarSource> = combine(
        currentAccountCoordinator.state,
        _uploadState,
    ) { bundle, uploadState ->
        val pending = pendingUpload

        when {
            uploadState is LoadState.Loading && pending != null ->
                ProfileAvatarSource.Local(pending.first)

            !bundle?.accountId.isNullOrEmpty() ->
                ProfileAvatarSource.Remote(AvatarUrl.of(bundle!!.accountId))

            else -> ProfileAvatarSource.Letter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProfileAvatarSource.Letter,
    )

    init {
        viewModelScope.launch {
            currentAccountCoordinator.refresh(CurrentAccountRefreshTrigger.PROFILE_OPENED)
        }
    }

    fun beginUpload(bitmap: Bitmap, imageData: ByteArray): Job {
        pendingUpload = bitmap to imageData
        return performUpload()
    }

    fun retryUpload(): Job = performUpload()

    fun dismissUploadError() {
        pendingUpload = null
        _uploadState.value = LoadState.Idle
    }

    private fun performUpload(): Job {
        _uploadState.value = LoadState.Loading

        return viewModelScope.launch {
            val pending = pendingUpload ?: run {
                _uploadState.value = LoadState.Idle
                return@launch
            }

            uploadAvatar.execute(imageData = pending.second)
                .onSuccess {
                    // The upload flow already seeded the shared avatar store, so success only
                    // clears the pending photo and reports loaded.
                    pendingUpload = null
                    _uploadState.value = LoadState.Loaded(Unit)
                }
                .onFailure { exception ->
                    _uploadState.value = LoadState.Failure(exception)
                }
        }
    }
}
