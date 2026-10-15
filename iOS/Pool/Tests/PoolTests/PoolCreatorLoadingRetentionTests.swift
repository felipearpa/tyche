import SwiftUI
import Testing
import UIKit
import Swinject
import Core
import DataPool
import UI
import ViewingState
@testable import Pool

/// The pool creator stays the same stepper while a pool is created: it is built once, and the
/// second step's name field is the same field, with the same name, after loading, a failure, a
/// retry, and success. The created pool is opened once, however often the screen redraws.
@MainActor
struct PoolCreatorLoadingRetentionTests {
    @Test
    func theStepperAndItsDraftSurviveLoadingFailureRetryAndSuccess() throws {
        var opened: [String] = []
        let driver = CreationDriver()
        let builds = StepperBuilds()
        let window = show(DrivenPoolCreator(driver: driver, builds: builds, onPoolCreated: { opened.append($0) }))
        defer { window.isHidden = true }

        // The pool name field binds through a focus state, and edits made from code do not reach
        // its binding in a unit-test host, so the draft here is the preselected name; the stepper
        // and field identities show that typed text would be kept too.
        let nameField = try #require(textFields(in: window).first)
        let draft = preselectedName
        #expect(nameField.text == draft)
        #expect(builds.count == 1)

        let attempt = CreatePoolModel(poolLayoutId: preselectedLayoutId, poolName: draft, poolId: nil)
        let created = CreatePoolModel(poolLayoutId: preselectedLayoutId, poolName: draft, poolId: createdPoolId)
        let states: [MutationState<CreatePoolModel>] = [
            .mutating(original: initialModel, updated: attempt),
            .failure(original: initialModel, updated: attempt, error: UnknownLocalizedError()),
            .idle(initialModel),
            .mutating(original: initialModel, updated: attempt),
            .mutated(original: initialModel, updated: created),
        ]
        for state in states {
            driver.state = state
            settle()

            #expect(builds.count == 1, "after \(state)")
            #expect(textFields(in: window).first === nameField, "after \(state)")
            #expect(nameField.text == draft, "after \(state)")
        }

        driver.redraw()
        settle()
        #expect(opened == [createdPoolId])
    }
}

private let preselectedLayoutId = "layout"
private let preselectedName = "zz loading probe"
private let createdPoolId = "created-pool"
private let initialModel = CreatePoolModel(poolLayoutId: preselectedLayoutId, poolName: preselectedName, poolId: nil)

@MainActor
private final class CreationDriver: ObservableObject {
    @Published var state: MutationState<CreatePoolModel> = .idle(initialModel)

    func redraw() {
        objectWillChange.send()
    }
}

private struct DrivenPoolCreator: View {
    @ObservedObject var driver: CreationDriver
    let builds: StepperBuilds
    let onPoolCreated: (String) -> Void

    var body: some View {
        NavigationStack {
            PoolFromLayoutCreatorStatefulView(
                state: driver.state,
                onSaveClick: { _ in },
                onPoolCreated: onPoolCreated,
                reset: {},
                preselectedPoolLayoutId: preselectedLayoutId,
                preselectedPoolName: preselectedName
            )
        }
        .environment(\.diResolver, emptyTemplatesResolver(counting: builds))
    }
}

/// Counts the template-list use cases the creator resolves: the creator resolves one each time it
/// builds a stepper, so a rebuilt stepper shows up as a second resolution.
@MainActor
private final class StepperBuilds {
    var count = 0
}

private func emptyTemplatesResolver(counting builds: StepperBuilds) -> DIResolver {
    let container = Container()
    container.register(GetOpenPoolLayoutsUseCase.self) { _ in
        MainActor.assumeIsolated { builds.count += 1 }
        return GetOpenPoolLayoutsUseCase(poolLayoutRepository: NoTemplatesRepository())
    }
    return DIResolver(resolver: container.synchronize())
}

private struct NoTemplatesRepository: PoolLayoutRepository {
    func getOpenPoolLayouts(next: String?, searchText: String?) async -> Result<CursorPage<PoolLayout>, Error> {
        .success(CursorPage(items: [], next: nil))
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
