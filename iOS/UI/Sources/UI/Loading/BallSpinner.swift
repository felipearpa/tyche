import SwiftUI

/// The Tyche ball, turning once every two seconds at a constant speed.
///
/// With Reduce Motion on, the ball stays upright and still; the setting is read live, so turning
/// it on or off while the ball is visible takes effect immediately. The rotation is driven by the
/// frame clock while the ball is on screen and stops when it leaves.
public struct BallSpinner: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init() {}

    public var body: some View {
        TimelineView(.animation(minimumInterval: nil, paused: reduceMotion)) { context in
            Image(.tycheLogo)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .foregroundStyle(Color.accentColor)
                .rotationEffect(reduceMotion ? .zero : Self.rotation(at: context.date))
        }
    }

    /// The ball's angle at `date`: a linear, clockwise turn every ``period`` that never reverses.
    static func rotation(at date: Date) -> Angle {
        let elapsed = date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: period)
        return .degrees(elapsed / period * 360)
    }

    static let period: TimeInterval = 2
}

#Preview {
    BallSpinner()
        .frame(width: 64, height: 64)
}
