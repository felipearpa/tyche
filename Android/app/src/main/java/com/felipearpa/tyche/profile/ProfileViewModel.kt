package com.felipearpa.tyche.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.account.AvatarUrl
import com.felipearpa.tyche.account.AvatarVersion
import com.felipearpa.tyche.session.AccountStorage
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
    accountStorage: AccountStorage,
) : ViewModel() {
    val accountId: StateFlow<String> = accountStorage.state
        .map { bundle -> bundle?.accountId.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = accountStorage.state.value?.accountId.orEmpty(),
        )

    val username: StateFlow<String> = accountStorage.state
        .map { bundle -> bundle?.username.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = accountStorage.state.value?.username.orEmpty(),
        )

    val email: StateFlow<String> = accountStorage.state
        .map { bundle -> bundle?.email.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = accountStorage.state.value?.email.orEmpty(),
        )

    private val _uploadState = MutableStateFlow<LoadState<Unit>>(LoadState.Idle)
    val uploadState: StateFlow<LoadState<Unit>> = _uploadState.asStateFlow()

    private val _localAvatar = MutableStateFlow<Bitmap?>(null)

    private var pendingUpload: Pair<Bitmap, ByteArray>? = null

    /**
     * The uploading photo shows optimistically; a failed upload falls back to what was
     * there before, and an account without a photo falls back to the letter avatar.
     */
    val avatarSource: StateFlow<ProfileAvatarSource> = combine(
        accountStorage.state,
        _localAvatar,
        _uploadState,
    ) { bundle, localAvatar, uploadState ->
        val pending = pendingUpload

        when {
            uploadState is LoadState.Loading && pending != null ->
                ProfileAvatarSource.Local(pending.first)

            localAvatar != null -> ProfileAvatarSource.Local(localAvatar)

            !bundle?.accountId.isNullOrEmpty() ->
                ProfileAvatarSource.Remote(AvatarUrl.of(bundle!!.accountId))

            else -> ProfileAvatarSource.Letter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProfileAvatarSource.Letter,
    )

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
                    _localAvatar.value = pending.first
                    pendingUpload = null
                    _uploadState.value = LoadState.Loaded(Unit)
                    AvatarVersion.bump()
                }
                .onFailure { exception ->
                    _uploadState.value = LoadState.Failure(exception)
                }
        }
    }
}
