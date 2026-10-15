import SwiftUI
import Testing
import UIKit
import Session
import UI
import ViewingState
@testable import Account

/// The sign-in forms stay the same screen while a submission loads, fails, and is retried: the
/// text fields the gambler typed into are the same fields afterward and still hold what was
/// typed. A successful email/password sign-in is reported once, however often the screen redraws.
@MainActor
struct SignInLoadingRetentionTests {
    @Test
    func sendLinkFormKeepsTheTypedAddressThroughLoadingFailureAndRetry() throws {
        let driver = StateDriver<LoadState<String>>(.idle)
        let window = show(DrivenEmailSignIn(driver: driver))
        defer { window.isHidden = true }

        let emailField = try #require(textFields(in: window).first)
        type(typedEmail, into: emailField)

        for state in [LoadState<String>.loading, .failure(SendSignInLinkToEmailLocalizedError.tooManyRequests), .idle, .loading] {
            driver.state = state
            settle()

            #expect(textFields(in: window).first === emailField, "after \(state)")
            #expect(emailField.text == typedEmail, "after \(state)")
        }
    }

    @Test
    func emailAndPasswordFormKeepsTheTypedValuesThroughLoadingFailureAndRetry() throws {
        let driver = StateDriver<LoadState<AccountBundle>>(.idle)
        let window = show(DrivenEmailAndPasswordSignIn(driver: driver, onAuthenticate: { _ in }))
        defer { window.isHidden = true }

        let fields = textFields(in: window)
        try #require(fields.count == 2)
        type(typedEmail, into: fields[0])
        type(typedPassword, into: fields[1])

        for state in [LoadState<AccountBundle>.loading, .failure(UnknownLocalizedError()), .idle, .loading] {
            driver.state = state
            settle()

            let current = textFields(in: window)
            #expect(current.count == 2 && current[0] === fields[0] && current[1] === fields[1], "after \(state)")
            #expect(fields[0].text == typedEmail, "after \(state)")
            #expect(fields[1].text?.count == typedPassword.count, "after \(state)")
        }
    }

    @Test
    func aSuccessfulEmailAndPasswordSignInIsReportedOnce() {
        var authenticated: [AccountBundle] = []
        let driver = StateDriver<LoadState<AccountBundle>>(.idle)
        let window = show(DrivenEmailAndPasswordSignIn(driver: driver, onAuthenticate: { authenticated.append($0) }))
        defer { window.isHidden = true }

        driver.state = .loading
        settle()
        #expect(authenticated.isEmpty)

        driver.state = .loaded(signedInAccount)
        settle()
        driver.redraw()
        settle()
        driver.state = .loaded(signedInAccount)
        settle()

        #expect(authenticated == [signedInAccount])
    }
}

private let typedEmail = "loading-probe@example.com"
/// Under the 16-character cap `PasswordTextField` applies to its value.
private let typedPassword = "loading-probe-1"
private let signedInAccount = AccountBundle(accountId: "account", externalAccountId: "external", email: typedEmail)

@MainActor
private final class StateDriver<State>: ObservableObject {
    @Published var state: State

    init(_ state: State) {
        self.state = state
    }

    func redraw() {
        objectWillChange.send()
    }
}

private struct DrivenEmailSignIn: View {
    @ObservedObject var driver: StateDriver<LoadState<String>>

    var body: some View {
        NavigationStack {
            EmailSignInStatefulView(viewState: driver.state)
        }
    }
}

private struct DrivenEmailAndPasswordSignIn: View {
    @ObservedObject var driver: StateDriver<LoadState<AccountBundle>>
    let onAuthenticate: (AccountBundle) -> Void

    var body: some View {
        NavigationStack {
            EmailAndPasswordSignInStatefulView(
                viewState: driver.state,
                onSignIn: { _, _ in },
                onReset: {},
                onAuthenticate: onAuthenticate
            )
        }
    }
}

@MainActor
private func show<Content: View>(_ content: Content) -> UIWindow {
    let host = UIHostingController(rootView: content)
    let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 402, height: 874))
    window.rootViewController = host
    window.makeKeyAndVisible()
    host.view.layoutIfNeeded()
    settle()
    return window
}

@MainActor
private func settle() {
    RunLoop.main.run(until: Date().addingTimeInterval(0.3))
}

/// Appends `text` and reports the edit to the field's `.editingChanged` targets, so SwiftUI's
/// binding takes the new value. The targets are called directly: `sendActions(for:)` dispatches
/// through `UIApplication`, which a package test process does not have, and on iOS 26 that drops
/// the edit before it reaches the binding.
@MainActor
private func type(_ text: String, into field: UITextField) {
    field.text = (field.text ?? "") + text
    for target in field.allTargets {
        for action in field.actions(forTarget: target, forControlEvent: .editingChanged) ?? [] {
            _ = (target as AnyObject).perform(Selector(action), with: field)
        }
    }
    settle()
}

/// The text fields on screen, top to bottom.
@MainActor
private func textFields(in window: UIWindow) -> [UITextField] {
    func collect(_ view: UIView) -> [UITextField] {
        let own = (view as? UITextField).map { [$0] } ?? []
        return own + view.subviews.flatMap(collect)
    }
    return collect(window).sorted {
        $0.convert($0.bounds, to: window).minY < $1.convert($1.bounds, to: window).minY
    }
}
