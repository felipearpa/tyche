import SwiftUI
import UI

struct MatchHeader: View {
    let bet: PoolGamblerBetModel

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(alignment: .center, spacing: boxSpacing.medium) {
            if bet.isLive {
                LiveIndicator()
            }

            HStack(spacing: boxSpacing.medium) {
                HStack(spacing: boxSpacing.small) {
                    FlagImage(teamCode: bet.homeTeamId)
                        .frame(width: flagSize, height: flagSize)

                    Text(bet.homeTeamName)
                        .font(.headline)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .truncationMode(.tail)
                        .frame(maxWidth: .infinity)
                }

                if bet.isComputed {
                    Text(bet.matchScore.map { String($0.homeTeamValue) } ?? "")
                        .font(.title2)

                    Text("-")

                    Text(bet.matchScore.map { String($0.awayTeamValue) } ?? "")
                        .font(.title2)
                }

                HStack(spacing: boxSpacing.small) {
                    Text(bet.awayTeamName)
                        .font(.headline)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .truncationMode(.tail)
                        .frame(maxWidth: .infinity)

                    FlagImage(teamCode: bet.awayTeamId)
                        .frame(width: flagSize, height: flagSize)
                }
            }

            Text(bet.matchDateTime.toShortDateTimeString())
                .font(.caption)
        }
        .frame(maxWidth: .infinity)
    }
}

private let flagSize: CGFloat = 48

private struct LiveIndicator: View {
    @State private var isPulsing = false

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        HStack(spacing: boxSpacing.small) {
            Circle()
                .fill(Color.accentColor)
                .frame(width: 8, height: 8)
                .opacity(isPulsing ? 0.3 : 1.0)
                .animation(
                    .easeInOut(duration: 0.8).repeatForever(autoreverses: true),
                    value: isPulsing
                )

            Text(.liveLabel)
                .font(.caption2)
                .foregroundStyle(Color.accentColor)
        }
        .onAppear { isPulsing = true }
    }
}

#Preview {
    MatchHeader(bet: poolGamblerBetDummyModel())
}

#Preview("Placeholder") {
    MatchHeader(bet: poolGamblerBetPlaceholderModel(isLocked: false, isComputed: false))
        .shimmer()
}
