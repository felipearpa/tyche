import SwiftUI
import Testing
import UI

/// The navigation state a drawer host leaves through. Controls respond according to the last
/// rendered update, so two activations handled before SwiftUI's next update both run; each case
/// runs the second against the state the first left, as happens when two fingers lift from the
/// screen in the same event. `DrawerInteractionTests.HostNavigationWiring` activates the drawer's
/// rendered rows the same way.
struct DrawerHostNavigationTests {
    @Test
    func aSecondHostControlBeforeTheDestinationShowsOpensNothing() {
        var navigation = DrawerHostNavigation()

        navigation.open(Route.gambler(1))
        navigation.open(Route.gambler(1))
        navigation.open(Route.gambler(2))

        #expect(navigation.path == NavigationPath([Route.gambler(1)]))
        #expect(navigation.isHostVisible == false)
        #expect(navigation.isHostInteractive == false)
    }

    @Test
    func aHostControlOpensAgainOnceTheHostIsVisibleAgain() {
        var navigation = DrawerHostNavigation()
        navigation.open(Route.gambler(1))

        // Back.
        navigation.path.removeLast()
        #expect(navigation.isHostVisible)
        navigation.open(Route.gambler(2))

        #expect(navigation.path == NavigationPath([Route.gambler(2)]))
    }

    @Test
    func theMenuControlAfterAHostRowLeavesTheDrawerClosed() {
        var navigation = DrawerHostNavigation()

        navigation.open(Route.gambler(1))
        navigation.toggleDrawer()

        #expect(navigation.path == NavigationPath([Route.gambler(1)]))
        #expect(navigation.isDrawerOpen == false)
    }

    @Test
    func aHostRowAfterTheMenuControlOpensNothing() {
        var navigation = DrawerHostNavigation()

        navigation.toggleDrawer()
        #expect(navigation.isHostInteractive == false)
        navigation.open(Route.gambler(1))

        #expect(navigation.path.isEmpty)
        #expect(navigation.isDrawerOpen)
    }

    @Test
    func theMenuControlOpensTheDrawerAgainOnceTheHostIsVisibleAgain() {
        var navigation = DrawerHostNavigation()
        navigation.open(Route.gambler(1))

        // Back.
        navigation.path.removeLast()
        navigation.toggleDrawer()

        #expect(navigation.isDrawerOpen)
    }

    @Test
    func aDrawerChoiceClosesTheDrawerAndOpensItsDestinationOnce() {
        var navigation = DrawerHostNavigation()
        navigation.isDrawerOpen = true

        navigation.openFromDrawer(Route.profile)
        navigation.openFromDrawer(Route.profile)
        navigation.openFromDrawer(Route.gamblers)

        #expect(navigation.path == NavigationPath([Route.profile]))
        #expect(navigation.isDrawerOpen == false)
    }

    @Test
    func aChoiceThatLeavesWithoutADestinationRunsOncePerOpening() {
        var navigation = DrawerHostNavigation()
        navigation.isDrawerOpen = true

        let first = navigation.closeDrawerForChoice()
        let second = navigation.closeDrawerForChoice()
        #expect(first)
        #expect(second == false)
        #expect(navigation.isDrawerOpen == false)

        navigation.isDrawerOpen = true
        let afterReopening = navigation.closeDrawerForChoice()
        #expect(afterReopening)
    }

    @Test
    func aDestinationChosenAfterAnotherChoiceIsNotOpened() {
        var navigation = DrawerHostNavigation()
        navigation.isDrawerOpen = true

        // Signing out, say, and then Profile.
        let signsOut = navigation.closeDrawerForChoice()
        navigation.openFromDrawer(Route.profile)

        #expect(signsOut)
        #expect(navigation.path.isEmpty)
    }

    @Test
    func aDrawerChoiceOpensAgainAfterReturningAndReopeningTheDrawer() {
        var navigation = DrawerHostNavigation()
        navigation.isDrawerOpen = true
        navigation.openFromDrawer(Route.profile)

        // Back, then the opener.
        navigation.path.removeLast()
        navigation.isDrawerOpen = true
        navigation.openFromDrawer(Route.gamblers)

        #expect(navigation.path == NavigationPath([Route.gamblers]))
        #expect(navigation.isDrawerOpen == false)
    }

    @Test
    func aHostControlAfterADrawerChoiceOpensNothing() {
        var navigation = DrawerHostNavigation()
        navigation.isDrawerOpen = true

        navigation.openFromDrawer(Route.profile)
        navigation.open(Route.gambler(1))

        #expect(navigation.path == NavigationPath([Route.profile]))
    }

    @Test
    func aDrawerChoiceAfterTheDrawerWasDismissedOpensNothing() {
        var navigation = DrawerHostNavigation()
        navigation.isDrawerOpen = true

        // A tap on the pushed screen closes the drawer.
        navigation.isDrawerOpen = false
        navigation.openFromDrawer(Route.profile)

        #expect(navigation.path.isEmpty)
    }
}

private enum Route: Hashable {
    case profile
    case gamblers
    case gambler(Int)
}
