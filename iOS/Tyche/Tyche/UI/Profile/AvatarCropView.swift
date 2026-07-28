import SwiftUI
import UI

let AVATAR_MORPH_ID = "profileAvatar"

/// Pan + pinch-zoom crop over a fixed circular window on a near-black surface.
/// The image always covers the window; zoom is bounded by `AvatarCrop`. A dual-scale
/// preview strip below the window shows the crop at Profile size and at row size.
struct AvatarCropView: View {
    let image: UIImage
    let username: String
    let morphNamespace: Namespace.ID
    let onConfirm: (CGRect) -> Void
    let onCancel: () -> Void

    @State private var steadyScale: CGFloat?
    @State private var steadyOffset: CGSize = .zero
    @GestureState private var pinch: CGFloat = 1
    @GestureState private var drag: CGSize = .zero
    @State private var isAtBoundary = false

    @Environment(\.boxSpacing) private var boxSpacing

    private let boundaryHaptic = UIImpactFeedbackGenerator(style: .light)

    private var imagePixelSize: CGSize {
        CGSize(width: image.size.width * image.scale, height: image.size.height * image.scale)
    }

    var body: some View {
        GeometryReader { proxy in
            let windowSide = min(proxy.size.width - boxSpacing.huge, proxy.size.height * 0.5)
            let crop = AvatarCrop(imageSize: imagePixelSize, windowSide: windowSide)
            let baseScale = steadyScale ?? crop.minScale
            let liveScale = crop.clampScale(baseScale * pinch)
            let liveOffset = liveOffset(crop: crop, baseScale: baseScale, liveScale: liveScale)

            VStack(spacing: 0) {
                cropWindow(crop: crop, scale: liveScale, offset: liveOffset)
                    .frame(maxHeight: .infinity)

                previewStrip(scale: liveScale, offset: liveOffset, windowSide: windowSide)
                    .padding(.vertical, boxSpacing.extraLarge)

                actionBar(crop: crop, scale: liveScale, offset: liveOffset)
            }
            .background(surfaceColor.ignoresSafeArea())
            .contentShape(Rectangle())
            .gesture(cropGesture(crop: crop, baseScale: baseScale))
        }
    }

    private func liveOffset(crop: AvatarCrop, baseScale: CGFloat, liveScale: CGFloat) -> CGSize {
        let zoomedOffset = crop.offset(
            zoomingTo: liveScale,
            from: baseScale,
            offset: steadyOffset,
            anchor: .zero
        )

        return crop.clampOffset(
            CGSize(width: zoomedOffset.width + drag.width, height: zoomedOffset.height + drag.height),
            scale: liveScale
        )
    }

    // Built as overlays on Color.clear so the zoomed image can never inflate the layout —
    // a frame carrying the child's own size would blow the screen apart at high zoom.
    private func cropWindow(crop: AvatarCrop, scale: CGFloat, offset: CGSize) -> some View {
        Color.clear
            .overlay(transformedImage(scale: scale, offset: offset))
            .overlay(
                scrimColor
                    .mask {
                        Rectangle()
                            .overlay(
                                Circle()
                                    .frame(width: crop.windowSide, height: crop.windowSide)
                                    .blendMode(.destinationOut)
                            )
                            .compositingGroup()
                    }
            )
            .overlay(
                transformedImage(scale: scale, offset: offset)
                    .frame(width: crop.windowSide, height: crop.windowSide)
                    .clipShape(Circle())
                    .matchedGeometryEffect(id: AVATAR_MORPH_ID, in: morphNamespace)
            )
            .overlay(
                Circle()
                    .strokeBorder(hairlineColor, lineWidth: 1)
                    .frame(width: crop.windowSide, height: crop.windowSide)
            )
            .clipped()
    }

    private func transformedImage(scale: CGFloat, offset: CGSize) -> some View {
        Image(uiImage: image)
            .resizable()
            .frame(width: imagePixelSize.width * scale, height: imagePixelSize.height * scale)
            .offset(offset)
    }

    private func previewStrip(scale: CGFloat, offset: CGSize, windowSide: CGFloat) -> some View {
        HStack(spacing: boxSpacing.extraLarge) {
            cropPreview(scale: scale, offset: offset, windowSide: windowSide, diameter: profilePreviewSize)

            HStack(spacing: boxSpacing.medium) {
                cropPreview(scale: scale, offset: offset, windowSide: windowSide, diameter: rowPreviewSize)

                Text(username)
                    .font(.subheadline)
                    .foregroundStyle(previewLabelColor)
                    .lineLimit(1)
            }
        }
        .padding(.horizontal, boxSpacing.large)
    }

    private func cropPreview(scale: CGFloat, offset: CGSize, windowSide: CGFloat, diameter: CGFloat) -> some View {
        let previewFactor = diameter / windowSide

        return Image(uiImage: image)
            .resizable()
            .frame(
                width: imagePixelSize.width * scale * previewFactor,
                height: imagePixelSize.height * scale * previewFactor
            )
            .offset(CGSize(width: offset.width * previewFactor, height: offset.height * previewFactor))
            .frame(width: diameter, height: diameter)
            .clipShape(Circle())
    }

    private func actionBar(crop: AvatarCrop, scale: CGFloat, offset: CGSize) -> some View {
        HStack {
            Button(action: onCancel) {
                Text(sharedResource: .cancelAction)
                    .foregroundStyle(.white)
            }

            Spacer()

            Button {
                onConfirm(crop.cropRect(scale: scale, offset: offset))
            } label: {
                Text(.usePhotoAction)
                    .fontWeight(.semibold)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(.horizontal, boxSpacing.large)
        .padding(.bottom, boxSpacing.large)
    }

    private func cropGesture(crop: AvatarCrop, baseScale: CGFloat) -> some Gesture {
        let dragGesture = DragGesture()
            .updating($drag) { value, state, _ in
                state = value.translation
            }
            .onChanged { value in
                let liveScale = crop.clampScale(baseScale * pinch)
                let zoomedOffset = crop.offset(
                    zoomingTo: liveScale,
                    from: baseScale,
                    offset: steadyOffset,
                    anchor: .zero
                )
                let rawOffset = CGSize(
                    width: zoomedOffset.width + value.translation.width,
                    height: zoomedOffset.height + value.translation.height
                )
                tickOnClamp(raw: rawOffset, clamped: crop.clampOffset(rawOffset, scale: liveScale))
            }

        let pinchGesture = MagnificationGesture()
            .updating($pinch) { value, state, _ in
                state = value
            }
            .onChanged { value in
                let rawScale = baseScale * value
                tickOnClamp(raw: rawScale, clamped: crop.clampScale(rawScale))
            }

        // One commit for both gestures, mirroring the live-display math exactly —
        // per-gesture commits would re-apply the zoom ratio to an already-committed pan.
        return dragGesture
            .simultaneously(with: pinchGesture)
            .onEnded { value in
                let translation = value.first?.translation ?? .zero
                let newScale = crop.clampScale(baseScale * (value.second ?? 1))
                let zoomedOffset = crop.offset(
                    zoomingTo: newScale,
                    from: baseScale,
                    offset: steadyOffset,
                    anchor: .zero
                )

                steadyOffset = crop.clampOffset(
                    CGSize(
                        width: zoomedOffset.width + translation.width,
                        height: zoomedOffset.height + translation.height
                    ),
                    scale: newScale
                )
                steadyScale = newScale
                isAtBoundary = false
            }
    }

    private func tickOnClamp(raw: CGSize, clamped: CGSize) {
        tick(clamping: raw != clamped)
    }

    private func tickOnClamp(raw: CGFloat, clamped: CGFloat) {
        tick(clamping: raw != clamped)
    }

    private func tick(clamping: Bool) {
        if clamping && !isAtBoundary {
            boundaryHaptic.impactOccurred()
        }
        isAtBoundary = clamping
    }
}

private let profilePreviewSize: CGFloat = 96
private let rowPreviewSize: CGFloat = 32

private let surfaceColor = Color(red: 0.04, green: 0.04, blue: 0.04)
private let scrimColor = Color.black.opacity(0.62)
private let hairlineColor = Color.white.opacity(0.24)
private let previewLabelColor = Color.white.opacity(0.85)

#Preview {
    struct CropPreviewHost: View {
        @Namespace private var namespace

        var body: some View {
            AvatarCropView(
                image: UIImage(systemName: "photo")!,
                username: "ElGoleador",
                morphNamespace: namespace,
                onConfirm: { _ in },
                onCancel: {}
            )
        }
    }

    return CropPreviewHost()
}
