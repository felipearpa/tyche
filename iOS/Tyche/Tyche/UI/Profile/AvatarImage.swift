import UIKit

extension UIImage {
    /// Redraws the image so EXIF orientation is baked in and one point equals one source pixel,
    /// which is what the crop geometry expects. Very large sources are downscaled to bound
    /// memory; the cap stays far above the 512-pixel output times the 5× zoom budget.
    func orientationNormalized(maxSide: CGFloat = 4096) -> UIImage {
        let pixelSize = CGSize(width: size.width * scale, height: size.height * scale)
        let downscale = min(1, maxSide / max(pixelSize.width, pixelSize.height))

        if imageOrientation == .up && scale == 1 && downscale == 1 {
            return self
        }

        let targetSize = CGSize(
            width: (pixelSize.width * downscale).rounded(),
            height: (pixelSize.height * downscale).rounded()
        )

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1

        return UIGraphicsImageRenderer(size: targetSize, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: targetSize))
        }
    }

    /// Renders the crop-rect region into a square avatar and encodes it as JPEG.
    func avatarRender(
        cropRect: CGRect,
        outputSide: CGFloat = AvatarCrop.outputSide,
        quality: CGFloat = 0.8
    ) -> (image: UIImage, jpegData: Data)? {
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1

        let outputSize = CGSize(width: outputSide, height: outputSide)
        let renderScale = outputSide / cropRect.width

        let rendered = UIGraphicsImageRenderer(size: outputSize, format: format).image { _ in
            draw(
                in: CGRect(
                    x: -cropRect.minX * renderScale,
                    y: -cropRect.minY * renderScale,
                    width: size.width * renderScale,
                    height: size.height * renderScale
                )
            )
        }

        guard let jpegData = rendered.jpegData(compressionQuality: quality) else { return nil }

        return (rendered, jpegData)
    }
}
