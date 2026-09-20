import CoreGraphics
import Testing
@testable import Tyche

@Suite("AvatarCrop")
struct AvatarCropTests {

    @Test("given a portrait source when the minimum scale is computed then the image exactly covers the window")
    func coverMinZoomForPortraitSource() {
        let crop = AvatarCrop(imageSize: CGSize(width: 1000, height: 2000), windowSide: 300)

        #expect(approx(crop.minScale, 0.3))
    }

    @Test("given a landscape source when the minimum scale is computed then the image exactly covers the window")
    func coverMinZoomForLandscapeSource() {
        let crop = AvatarCrop(imageSize: CGSize(width: 2000, height: 1000), windowSide: 300)

        #expect(approx(crop.minScale, 0.3))
    }

    @Test("given a square source when the minimum scale is computed then the image exactly covers the window")
    func coverMinZoomForSquareSource() {
        let crop = AvatarCrop(imageSize: CGSize(width: 1000, height: 1000), windowSide: 300)

        #expect(approx(crop.minScale, 0.3))
    }

    @Test("given a very large source when the maximum scale is computed then it caps at five times the cover zoom")
    func maxZoomCapsAtFiveTimesCover() {
        let crop = AvatarCrop(imageSize: CGSize(width: 4000, height: 4000), windowSide: 300)

        #expect(approx(crop.maxScale, crop.minScale * 5))
    }

    @Test("given a mid-size source when the maximum scale is computed then quality bounds it below the cap")
    func maxZoomBoundedByOutputQuality() {
        let crop = AvatarCrop(imageSize: CGSize(width: 1024, height: 2048), windowSide: 300)

        #expect(approx(crop.maxScale, crop.minScale * 2))
    }

    @Test("given a source smaller than the output when the maximum scale is computed then the minimum wins")
    func maxZoomNeverFallsBelowCoverMinimum() {
        let crop = AvatarCrop(imageSize: CGSize(width: 400, height: 600), windowSide: 300)

        #expect(approx(crop.maxScale, crop.minScale))
        #expect(approx(crop.clampScale(10), crop.minScale))
        #expect(approx(crop.clampScale(0.001), crop.minScale))
    }

    @Test("given offsets past each edge when clamped then the window stays covered on all four sides")
    func panClampsAtAllFourEdges() {
        let crop = AvatarCrop(imageSize: CGSize(width: 1000, height: 1000), windowSide: 300)
        let scale: CGFloat = 0.5

        let pastTrailingBottom = crop.clampOffset(CGSize(width: 200, height: 200), scale: scale)
        let pastLeadingTop = crop.clampOffset(CGSize(width: -200, height: -200), scale: scale)
        let within = crop.clampOffset(CGSize(width: 80, height: -90), scale: scale)

        #expect(approx(pastTrailingBottom, CGSize(width: 100, height: 100)))
        #expect(approx(pastLeadingTop, CGSize(width: -100, height: -100)))
        #expect(approx(within, CGSize(width: 80, height: -90)))
    }

    @Test("given the image exactly covers the window when any offset is clamped then it collapses to zero")
    func panCollapsesAtExactCover() {
        let crop = AvatarCrop(imageSize: CGSize(width: 1000, height: 1000), windowSide: 300)

        let clamped = crop.clampOffset(CGSize(width: 10, height: -10), scale: crop.minScale)

        #expect(approx(clamped, .zero))
    }

    @Test("given a zoom about an anchor when the offset is recomputed then the anchored image point stays put")
    func zoomKeepsAnchoredPointStationary() {
        let crop = AvatarCrop(imageSize: CGSize(width: 4000, height: 4000), windowSide: 300)

        let offset = crop.offset(
            zoomingTo: 0.3,
            from: 0.15,
            offset: CGSize(width: 10, height: 20),
            anchor: CGPoint(x: 50, y: -30)
        )

        #expect(approx(offset, CGSize(width: -30, height: 70)))
    }

    @Test("given a zoom out to the cover minimum when the offset is recomputed then it re-clamps to keep coverage")
    func zoomOutReclampsOffset() {
        let crop = AvatarCrop(imageSize: CGSize(width: 1000, height: 1000), windowSide: 300)

        let offset = crop.offset(
            zoomingTo: crop.minScale,
            from: 0.6,
            offset: CGSize(width: 150, height: 150),
            anchor: .zero
        )

        #expect(approx(offset, .zero))
    }

    @Test("given a source matching the output at scale one when the crop rect is computed then it is the identity")
    func cropRectIdentityCase() {
        let crop = AvatarCrop(imageSize: CGSize(width: 512, height: 512), windowSide: 512)

        let rect = crop.cropRect(scale: 1, offset: .zero)

        #expect(approx(rect, CGRect(x: 0, y: 0, width: 512, height: 512)))
    }

    @Test("given a pan and zoom when the crop rect is computed then it maps the visible square to source pixels")
    func cropRectMapsVisibleRegion() {
        let crop = AvatarCrop(imageSize: CGSize(width: 2000, height: 1000), windowSide: 300)

        let rect = crop.cropRect(scale: 0.5, offset: CGSize(width: 25, height: -35))

        #expect(approx(rect, CGRect(x: 650, y: 270, width: 600, height: 600)))
    }
}

private func approx(_ lhs: CGFloat, _ rhs: CGFloat) -> Bool {
    abs(lhs - rhs) < 1e-9
}

private func approx(_ lhs: CGSize, _ rhs: CGSize) -> Bool {
    approx(lhs.width, rhs.width) && approx(lhs.height, rhs.height)
}

private func approx(_ lhs: CGRect, _ rhs: CGRect) -> Bool {
    approx(lhs.minX, rhs.minX) && approx(lhs.minY, rhs.minY)
        && approx(lhs.width, rhs.width) && approx(lhs.height, rhs.height)
}
