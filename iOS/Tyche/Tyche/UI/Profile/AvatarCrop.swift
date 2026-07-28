import CoreGraphics

/// Pure geometry for the avatar crop screen, shared as a spec with the Android implementation:
/// `scale` maps source pixels to view points, the crop window is a centered square of `windowSide`
/// points, and `offset` is the translation of the image center from the window center.
struct AvatarCrop: Equatable {
    let imageSize: CGSize
    let windowSide: CGFloat

    static let outputSide: CGFloat = 512
    static let maxZoomFactor: CGFloat = 5

    /// The image always covers the window (aspect fill), never less.
    var minScale: CGFloat {
        windowSide / min(imageSize.width, imageSize.height)
    }

    /// The visible region never holds fewer source pixels than the output needs,
    /// capped at 5× the cover zoom; tiny sources collapse the range to `minScale`.
    var maxScale: CGFloat {
        let qualityZoomFactor = min(imageSize.width, imageSize.height) / Self.outputSide
        return minScale * min(max(qualityZoomFactor, 1), Self.maxZoomFactor)
    }

    func clampScale(_ scale: CGFloat) -> CGFloat {
        min(max(scale, minScale), maxScale)
    }

    func clampOffset(_ offset: CGSize, scale: CGFloat) -> CGSize {
        let clampedScale = clampScale(scale)
        let horizontalSlack = (imageSize.width * clampedScale - windowSide) / 2
        let verticalSlack = (imageSize.height * clampedScale - windowSide) / 2

        return CGSize(
            width: min(max(offset.width, -horizontalSlack), horizontalSlack),
            height: min(max(offset.height, -verticalSlack), verticalSlack)
        )
    }

    /// Offset that keeps the image point under `anchor` (window coordinates, relative to the
    /// window center) stationary while zooming, re-clamped so the window stays covered.
    func offset(
        zoomingTo newScale: CGFloat,
        from oldScale: CGFloat,
        offset: CGSize,
        anchor: CGPoint
    ) -> CGSize {
        let clampedScale = clampScale(newScale)
        let zoomRatio = clampedScale / oldScale

        let zoomedOffset = CGSize(
            width: anchor.x - (anchor.x - offset.width) * zoomRatio,
            height: anchor.y - (anchor.y - offset.height) * zoomRatio
        )

        return clampOffset(zoomedOffset, scale: clampedScale)
    }

    /// The visible square under the window, in source pixels.
    func cropRect(scale: CGFloat, offset: CGSize) -> CGRect {
        let side = windowSide / scale

        return CGRect(
            x: imageSize.width / 2 - (windowSide / 2 + offset.width) / scale,
            y: imageSize.height / 2 - (windowSide / 2 + offset.height) / scale,
            width: side,
            height: side
        )
    }
}
