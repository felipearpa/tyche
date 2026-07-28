import Account
import PhotosUI
import Session
import SwiftUI
import UI
import ViewingState

struct ProfileView: View {
    @StateObject private var viewModel: ProfileViewModel
    private let onEditUsername: () -> Void

    @State private var isShowingSourceDialog = false
    @State private var isShowingLibrary = false
    @State private var isShowingCamera = false
    @State private var photoItem: PhotosPickerItem?
    @State private var cropImage: UIImage?

    @Namespace private var avatarMorph
    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    init(
        viewModel: @autoclosure @escaping () -> ProfileViewModel,
        onEditUsername: @escaping () -> Void
    ) {
        self._viewModel = StateObject(wrappedValue: viewModel())
        self.onEditUsername = onEditUsername
    }

    var body: some View {
        ZStack {
            profileContent

            if let cropImage {
                AvatarCropView(
                    image: cropImage,
                    username: viewModel.username,
                    morphNamespace: avatarMorph,
                    onConfirm: { cropRect in confirmCrop(image: cropImage, cropRect: cropRect) },
                    onCancel: { self.cropImage = nil }
                )
                .zIndex(1)
                .transition(.opacity)
                .toolbar(.hidden, for: .navigationBar)
            }
        }
        .navigationTitle(String(localized: .profileTitle))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadAccount()
        }
    }

    private var profileContent: some View {
        ScrollView {
            VStack(spacing: 0) {
                avatarBlock
                    .padding(.top, boxSpacing.xxLarge)

                changePhotoButton
                    .padding(.top, boxSpacing.large)

                usernameSection
                    .padding(.top, boxSpacing.xxLarge)
                    .padding(.horizontal, boxSpacing.large)
            }
            .frame(maxWidth: contentMaxWidth)
            .frame(maxWidth: .infinity)
        }
        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
        .confirmationDialog(
            String(localized: .changePhotoAction),
            isPresented: $isShowingSourceDialog,
            titleVisibility: .hidden
        ) {
            Button(String(localized: .chooseFromLibraryAction)) {
                isShowingLibrary = true
            }

            if UIImagePickerController.isSourceTypeAvailable(.camera) {
                Button(String(localized: .takePhotoAction)) {
                    isShowingCamera = true
                }
            }
        }
        .photosPicker(isPresented: $isShowingLibrary, selection: $photoItem, matching: .images)
        .onChange(of: photoItem) { newItem in
            guard let newItem else { return }
            photoItem = nil

            Task {
                guard
                    let imageData = try? await newItem.loadTransferable(type: Data.self),
                    let pickedImage = UIImage(data: imageData)
                else { return }

                cropImage = pickedImage.orientationNormalized()
            }
        }
        .fullScreenCover(isPresented: $isShowingCamera) {
            CameraPicker(
                onCapture: { capturedImage in
                    isShowingCamera = false
                    cropImage = capturedImage.orientationNormalized()
                },
                onCancel: { isShowingCamera = false }
            )
            .ignoresSafeArea()
        }
        .alert(
            String(localized: .avatarUploadErrorTitle),
            isPresented: uploadFailureBinding
        ) {
            Button(String(sharedResource: .retryAction)) {
                viewModel.retryUpload()
            }

            Button(String(sharedResource: .cancelAction), role: .cancel) {
                viewModel.dismissUploadError()
            }
        } message: {
            Text(.avatarUploadErrorMessage)
        }
    }

    private var avatarBlock: some View {
        ZStack(alignment: .bottomTrailing) {
            avatarImage
                .frame(width: avatarSize, height: avatarSize)
                .clipShape(Circle())
                .matchedGeometryEffect(id: AVATAR_MORPH_ID, in: avatarMorph, isSource: false)
                .overlay {
                    if viewModel.uploadState.isLoading() {
                        Circle().fill(.black.opacity(0.3))

                        BallSpinner()
                            .frame(width: boxSpacing.extraLarge, height: boxSpacing.extraLarge)
                    }
                }

            cameraBadge
        }
    }

    @ViewBuilder
    private var avatarImage: some View {
        switch viewModel.avatarSource {
        case .local(let image):
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
        case .remote:
            AccountAvatar(accountId: viewModel.accountId, email: viewModel.email)
        case .letter:
            EmailAvatar(email: viewModel.email)
        }
    }

    private var cameraBadge: some View {
        Circle()
            .fill(Color.accentColor)
            .frame(width: badgeSize, height: badgeSize)
            .overlay {
                Image(systemName: "camera.fill")
                    .font(.system(size: badgeIconSize, weight: .semibold))
                    .foregroundStyle(.white)
            }
            .overlay {
                Circle().strokeBorder(Color(uiColor: .systemGroupedBackground), lineWidth: 2)
            }
    }

    private var changePhotoButton: some View {
        Button {
            isShowingSourceDialog = true
        } label: {
            Text(.changePhotoAction)
                .fontWeight(.semibold)
        }
        .disabled(viewModel.uploadState.isLoading())
    }

    private var usernameSection: some View {
        VStack(alignment: .leading, spacing: boxSpacing.small) {
            Button(action: onEditUsername) {
                HStack(spacing: boxSpacing.medium) {
                    Text(.usernameLabel)
                        .foregroundStyle(.primary)

                    Spacer()

                    Text(viewModel.username)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.tail)

                    Image(sharedResource: .arrowForwardIos)
                        .foregroundStyle(Color.secondary)
                }
                .padding(boxSpacing.large)
            }
            .background(Color(uiColor: .secondarySystemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12))

            Text(.profileFooterText)
                .font(.footnote)
                .foregroundStyle(Color.secondary)
                .padding(.horizontal, boxSpacing.large)
        }
    }

    // Dismissal is decided by the alert buttons alone; a setter that reacted to the
    // presentation flag could discard the pending photo before the Retry action runs.
    private var uploadFailureBinding: Binding<Bool> {
        Binding(
            get: { viewModel.uploadState.isFailure() },
            set: { _ in }
        )
    }

    private func confirmCrop(image: UIImage, cropRect: CGRect) {
        guard let render = image.avatarRender(cropRect: cropRect) else {
            cropImage = nil
            return
        }

        viewModel.beginUpload(image: render.image, data: render.jpegData)

        withAnimation(reduceMotion ? nil : .spring(response: 0.45, dampingFraction: 0.8)) {
            cropImage = nil
        }
    }
}

private let avatarSize: CGFloat = 96
private let badgeSize: CGFloat = 30
private let badgeIconSize: CGFloat = 13
private let contentMaxWidth: CGFloat = 560

#Preview("light") {
    NavigationStack {
        ProfileView(
            viewModel: ProfileViewModel(
                accountStorage: PreviewAccountStorage(),
                onUploadAvatar: { _ in .success(()) }
            ),
            onEditUsername: {}
        )
    }
}

#Preview("dark") {
    NavigationStack {
        ProfileView(
            viewModel: ProfileViewModel(
                accountStorage: PreviewAccountStorage(),
                onUploadAvatar: { _ in .success(()) }
            ),
            onEditUsername: {}
        )
    }
    .preferredColorScheme(.dark)
}
