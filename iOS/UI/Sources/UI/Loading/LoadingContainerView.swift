import SwiftUI

/// Full-screen loading host for a blocking operation.
///
/// The host always renders `content` in the same place, so toggling `isLoading` keeps the
/// screen's view identity and with it its text, selection, focus history, step, and scroll
/// position. While loading:
/// - the content is softly blurred; no material, dimming, or tint is drawn over it;
/// - a clear barrier takes every touch and the content is disabled, so it cannot be tapped,
///   scrolled, typed into, or activated;
/// - the content is hidden from assistive technologies, which find a single localized loading
///   status instead, and VoiceOver focus moves to that status;
/// - the Tyche ball is centered in the host's safe area, which excludes system bars and a
///   visible keyboard.
///
/// When loading ends, all of the above is removed and the content is interactive again.
///
/// Place the host around the whole screen, outside form padding, so it fills the area the screen
/// is given. A host without content paints the normal system background and shows only the ball.
public struct LoadingContainerView<Content: View>: View {
    private let isLoading: Bool
    private let content: Content

    public init(isLoading: Bool, @ViewBuilder content: () -> Content) {
        self.isLoading = isLoading
        self.content = content()
    }

    public var body: some View {
        ZStack {
            canvas
            content
                .blur(radius: isLoading ? blurRadius : 0)
                .disabled(isLoading)
                .accessibilityHidden(isLoading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay {
            if isLoading {
                LoadingIndicatorLayer()
            }
        }
    }

    /// The normal system background when there is no screen to show, and nothing otherwise, so a
    /// screen keeps the background its container gives it.
    @ViewBuilder
    private var canvas: some View {
        if Content.self == EmptyView.self {
            Color(uiColor: .systemBackground)
                .ignoresSafeArea()
        } else {
            Color.clear
        }
    }
}

public extension LoadingContainerView where Content == EmptyView {
    /// A host with no screen behind it: the ball on the normal system background.
    init(isLoading: Bool) {
        self.init(isLoading: isLoading) { EmptyView() }
    }
}

/// The barrier that keeps touches away from the covered content, and the ball with its status.
private struct LoadingIndicatorLayer: View {
    @AccessibilityFocusState private var isStatusFocused: Bool

    var body: some View {
        ZStack {
            Color.clear
                .contentShape(Rectangle())
                .ignoresSafeArea()
                .accessibilityHidden(true)

            BallSpinner()
                .frame(width: ballSize, height: ballSize)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text(.loadingStatus))
                .accessibilityFocused($isStatusFocused)
        }
        .onAppear { isStatusFocused = true }
    }
}

private let blurRadius: CGFloat = 6
private let ballSize: CGFloat = 64

private struct LoadingPreviewForm: View {
    @State private var name = "Gambler"

    var body: some View {
        Form {
            TextField("Name", text: $name)
            Toggle("Remember me", isOn: .constant(true))
            Button("Continue") {}
        }
    }
}

#Preview("Content, light") {
    NavigationStack {
        LoadingContainerView(isLoading: true) { LoadingPreviewForm() }
            .navigationTitle("Sign in")
    }
    .preferredColorScheme(.light)
}

#Preview("Content, dark") {
    NavigationStack {
        LoadingContainerView(isLoading: true) { LoadingPreviewForm() }
            .navigationTitle("Sign in")
    }
    .preferredColorScheme(.dark)
}

#Preview("No content, light") {
    LoadingContainerView(isLoading: true)
        .preferredColorScheme(.light)
}

#Preview("No content, dark") {
    LoadingContainerView(isLoading: true)
        .preferredColorScheme(.dark)
}
