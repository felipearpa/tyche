import CoreGraphics
import Testing
@testable import UI

struct SwipeToDismissThresholdTests {
    @Test(arguments: [320.0, 430.0, 1024.0])
    func fixedDistanceConfirmsAt56Points(rowWidth: CGFloat) {
        let threshold = SwipeToDismissThreshold.distance(56)

        #expect(threshold.isCrossed(byDragDistance: 56, rowWidth: rowWidth))
    }

    @Test(arguments: [320.0, 430.0, 1024.0])
    func fixedDistanceSnapsBackAt40Points(rowWidth: CGFloat) {
        let threshold = SwipeToDismissThreshold.distance(56)

        #expect(!threshold.isCrossed(byDragDistance: 40, rowWidth: rowWidth))
    }

    @Test
    func fractionScalesWithRowWidth() {
        let threshold = SwipeToDismissThreshold.fraction(0.35)

        #expect(!threshold.isCrossed(byDragDistance: 56, rowWidth: 400))
        #expect(threshold.isCrossed(byDragDistance: 140, rowWidth: 400))
    }
}
