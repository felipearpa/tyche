import SwiftUI
import UI
import ViewingState

/// The scrolling "Gamblers" heading with the pool's gambler count beneath it. The count line is a
/// loading placeholder until the count arrives and is left out when it could not be loaded or the
/// pool has no count.
struct ManageGamblersHeader: View {
    let count: ViewingState.LoadState<Int?>

    var body: some View {
        VStack(alignment: .leading, spacing: headingSpacing) {
            Text(.manageGamblersTitle)
                .font(.largeTitle.bold())
                .foregroundStyle(Color(sharedResource: .onSurface))
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)

            countLine
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, rowHorizontalPadding)
        .padding(.bottom, headingBottomPadding)
    }

    @ViewBuilder
    private var countLine: some View {
        switch count {
        case .loaded(.some(let gamblerCount)):
            ManageGamblersCountLine(count: gamblerCount)
        case .idle, .loading:
            ManageGamblersCountLine(count: placeholderGamblerCount, isPlaceholder: true)
        case .loaded(.none), .failure:
            EmptyView()
        }
    }
}

/// "N gamblers in this pool". With `isPlaceholder` the line is concealed by the shared loading
/// pulse and hidden from VoiceOver; `count` is then filler.
struct ManageGamblersCountLine: View {
    let count: Int
    let isPlaceholder: Bool

    init(count: Int, isPlaceholder: Bool = false) {
        self.count = count
        self.isPlaceholder = isPlaceholder
    }

    var body: some View {
        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { line }
            } else {
                line.foregroundStyle(Color(sharedResource: .onSurface).opacity(countLineOpacity))
            }
        }
        .accessibilityHidden(isPlaceholder)
    }

    private var line: some View {
        Text(.manageGamblersCount(gamblers: count))
            .font(.subheadline)
            .fixedSize(horizontal: false, vertical: true)
    }
}

private let headingSpacing: CGFloat = 2
private let headingBottomPadding: CGFloat = 16
private let placeholderGamblerCount = 12
/// `onSurface` at 70 %: 5.9:1 on the light canvas and 7.7:1 on the dark one.
private let countLineOpacity = 0.7

#Preview("Loaded") {
    ManageGamblersHeader(count: .loaded(3))
}

#Preview("One gambler") {
    ManageGamblersHeader(count: .loaded(1))
}

#Preview("Loading") {
    ManageGamblersHeader(count: .loading)
}

#Preview("Count failed") {
    ManageGamblersHeader(count: .failure(CancellationError()))
}

#Preview("Large text, Spanish") {
    ManageGamblersHeader(count: .loaded(3))
        .environment(\.dynamicTypeSize, .accessibility3)
        .environment(\.locale, Locale(identifier: "es-CO"))
}
