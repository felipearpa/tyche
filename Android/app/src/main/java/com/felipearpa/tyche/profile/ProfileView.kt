package com.felipearpa.tyche.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.tyche.R
import com.felipearpa.tyche.account.AccountAvatar
import com.felipearpa.tyche.account.EmailAvatar
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.ui.state.LoadState
import java.io.File
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun ProfileView(
    viewModel: ProfileViewModel,
    onEditUsername: () -> Unit,
    onBack: () -> Unit,
) {
    val accountId by viewModel.accountId.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val avatarSource by viewModel.avatarSource.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val libraryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            cropBitmap = decodeOrientedBitmap(context.contentResolver, it)
        }
    }

    var captureUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { captured ->
        if (captured) {
            captureUri?.let { uri ->
                cropBitmap = decodeOrientedBitmap(context.contentResolver, uri)
            }
        }
    }

    BackHandler(enabled = cropBitmap != null) {
        cropBitmap = null
    }

    // The confirm transition is a crossfade: Compose's shared-element transition left the crop
    // screen's pointer input consumed by its overlay on device, so the morph ships on iOS only.
    AnimatedContent(targetState = cropBitmap, label = "avatarCrop") { bitmap ->
        if (bitmap == null) {
            ProfileContent(
                accountId = accountId,
                username = username,
                email = email,
                avatarSource = avatarSource,
                isUploading = uploadState is LoadState.Loading,
                onChooseFromLibrary = {
                    libraryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onTakePhoto = {
                    val captureDirectory = File(context.cacheDir, "avatar").apply { mkdirs() }
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(captureDirectory, "capture.jpg"),
                    )
                    captureUri = uri
                    cameraLauncher.launch(uri)
                },
                onEditUsername = onEditUsername,
                onBack = onBack,
            )
        } else {
            AvatarCropView(
                bitmap = bitmap,
                username = username,
                onConfirm = { cropRect ->
                    val (rendered, imageData) = bitmap.avatarRender(cropRect = cropRect)
                    viewModel.beginUpload(bitmap = rendered, imageData = imageData)
                    cropBitmap = null
                },
                onCancel = { cropBitmap = null },
            )
        }
    }

    if (uploadState is LoadState.Failure) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUploadError() },
            title = { Text(text = stringResource(id = R.string.avatar_upload_error_title)) },
            text = { Text(text = stringResource(id = R.string.avatar_upload_error_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.retryUpload() }) {
                    Text(text = stringResource(id = SharedR.string.retry_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUploadError() }) {
                    Text(text = stringResource(id = SharedR.string.cancel_action))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    accountId: String,
    username: String,
    email: String,
    avatarSource: ProfileAvatarSource,
    isUploading: Boolean,
    onChooseFromLibrary: () -> Unit,
    onTakePhoto: () -> Unit,
    onEditUsername: () -> Unit,
    onBack: () -> Unit,
) {
    var sourceChooserVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = SharedR.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = LocalBoxSpacing.current.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarBlock(
                accountId = accountId,
                avatarSource = avatarSource,
                email = email,
                isUploading = isUploading,
                modifier = Modifier.padding(top = LocalBoxSpacing.current.xxLarge),
            )

            TextButton(
                onClick = { sourceChooserVisible = true },
                enabled = !isUploading,
                modifier = Modifier.padding(top = LocalBoxSpacing.current.medium),
            ) {
                Text(
                    text = stringResource(id = R.string.change_photo_action),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            UsernameSection(
                username = username,
                onEditUsername = onEditUsername,
                modifier = Modifier.padding(top = LocalBoxSpacing.current.xxLarge),
            )
        }
    }

    if (sourceChooserVisible) {
        PhotoSourceChooser(
            onChooseFromLibrary = {
                sourceChooserVisible = false
                onChooseFromLibrary()
            },
            onTakePhoto = {
                sourceChooserVisible = false
                onTakePhoto()
            },
            onDismiss = { sourceChooserVisible = false },
        )
    }
}

@Composable
private fun AvatarBlock(
    accountId: String,
    avatarSource: ProfileAvatarSource,
    email: String,
    isUploading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(AVATAR_SIZE.dp)
                .clip(CircleShape),
        ) {
            AvatarImage(
                accountId = accountId,
                avatarSource = avatarSource,
                email = email,
                modifier = Modifier.fillMaxSize(),
            )

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(LocalBoxSpacing.current.extraLarge),
                        color = Color.White,
                    )
                }
            }
        }

        CameraBadge(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun AvatarImage(
    accountId: String,
    avatarSource: ProfileAvatarSource,
    email: String,
    modifier: Modifier = Modifier,
) {
    when (avatarSource) {
        is ProfileAvatarSource.Local ->
            Image(
                bitmap = avatarSource.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )

        is ProfileAvatarSource.Remote ->
            AccountAvatar(accountId = accountId, email = email, modifier = modifier)

        ProfileAvatarSource.Letter ->
            EmailAvatar(email = email, modifier = modifier)
    }
}

@Composable
private fun CameraBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BADGE_SIZE.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.background,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = SharedR.drawable.photo_camera),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 7.dp),
            )
        }
    }
}

@Composable
private fun UsernameSection(
    username: String,
    onEditUsername: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        modifier = modifier.fillMaxWidth(),
    ) {
        Card(
            onClick = onEditUsername,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = LocalBoxSpacing.current.large),
            ) {
                Text(
                    text = stringResource(id = R.string.username_label),
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Icon(
                    painter = painterResource(id = SharedR.drawable.arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Text(
            text = stringResource(id = R.string.profile_footer_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = LocalBoxSpacing.current.large),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceChooser(
    onChooseFromLibrary: () -> Unit,
    onTakePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ListItem(
            headlineContent = { Text(text = stringResource(id = R.string.choose_from_library_action)) },
            leadingContent = {
                Icon(
                    painter = painterResource(id = SharedR.drawable.photo_library),
                    contentDescription = null,
                )
            },
            modifier = Modifier.clickableRow(onChooseFromLibrary),
        )

        ListItem(
            headlineContent = { Text(text = stringResource(id = R.string.take_photo_action)) },
            leadingContent = {
                Icon(
                    painter = painterResource(id = SharedR.drawable.photo_camera),
                    contentDescription = null,
                )
            },
            modifier = Modifier
                .clickableRow(onTakePhoto)
                .padding(bottom = LocalBoxSpacing.current.large),
        )
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this
        .fillMaxWidth()
        .clickable(onClick = onClick)

private const val AVATAR_SIZE = 96
private const val BADGE_SIZE = 30
