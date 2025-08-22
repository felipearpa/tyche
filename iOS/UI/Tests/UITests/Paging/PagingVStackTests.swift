import XCTest
import SwiftUI
import ViewInspector
@testable import UI
@testable import Core

final class PagingVStackTests: XCTestCase {
    @MainActor
    func testGivenALoadingPagingItemWhenInViewThenTheLoadingContentIsShown() {
        let loadingLazyPager = givenALoadingPagingItem()
        let pagingVStack = whenLoadingPageItemInView(lazyPager: loadingLazyPager)
        thenTheLoadingContentIsShown(pagingVStack: pagingVStack)
    }
    
    @MainActor
    func testGivenAnAppendLoadingPagingItemWhenInViewThenTheLoadingContentOnConcatenateIsShown() {
        let appendLoadingLazyPager = givenAnAppendLoadingPagingItem()
        let pagingVStack = whenAppendLoadingPageItemInView(lazyPager: appendLoadingLazyPager)
        thenTheLoadingContentOnConcatenateIsShown(pagingVStack: pagingVStack)
    }
    
    @MainActor
    func testGivenAnErrorPagingItemWhenInViewThenTheErrorContentIsShown() {
        let errorLazyPager = givenAnErrorPagingItem()
        let pagingVStack = whenErrorPageItemInView(lazyPager: errorLazyPager)
        thenTheErrorContentIsShown(pagingVStack: pagingVStack)
    }
    
    @MainActor
    func testGivenAnAppendErrorPagingItemWhenInViewThenTheErrorContentOnConcatenateIsShown() {
        let appendErrorLazyPager = givenAnAppendErrorPagingItem()
        let pagingVStack = whenAppendErrorPageItemInView(lazyPager: appendErrorLazyPager)
        thenTheAppendErrorContentIsShown(pagingVStack: pagingVStack)
    }
    
    @MainActor
    func testGivenAnEmptyPagingItemWhenInViewThenTheEmptyContentIsShown() {
        let emptyLazyPager = givenAnEmptyPagingItem()
        let pagingVStack = whenEmptyPageItemInView(lazyPager: emptyLazyPager)
        thenTheEmptyContentIsShown(pagingVStack: pagingVStack)
    }
    
    @MainActor
    func testGivenAFilledPagingItemWhenInViewThenTheItemContentIsShown() {
        let filledLazyPager = givenAFilledPagingItem()
        let pagingVStack = whenFilledPageItemInView(lazyPager: filledLazyPager)
        thenTheFilledContentIsShown(pagingVStack: pagingVStack)
    }
    
    @MainActor
    private func givenALoadingPagingItem() -> LazyPagingItems<String, FakeItemPagingSource> {
        FakeLoadingLazyPager()
    }
    
    private func whenLoadingPageItemInView(lazyPager: LazyPagingItems<String, FakeItemPagingSource>) -> StatefulLazyVStack<
        String,
        FakeItemPagingSource,
        ViewShown,
        EmptyView,
        EmptyView,
        EmptyView,
        EmptyView,
        EmptyView> {
            return StatefulLazyVStack<String, FakeItemPagingSource, ViewShown, EmptyView, EmptyView, EmptyView, EmptyView, EmptyView>(
                lazyPager: lazyPager,
                loadingContent: { ViewShown() },
                emptyContent: { EmptyView() },
                errorContent: { _ in EmptyView() },
                loadingContentOnConcatenate: { EmptyView() },
                errorContentOnConcatenate: { _ in EmptyView() }
            ) { _ in
                EmptyView()
            }
        }
    
    private func thenTheLoadingContentIsShown
    <Key,
     Item: Identifiable & Hashable,
     LoadingContent: View,
     EmptyContent: View,
     ErrorContent: View,
     LoadingContentOnConcatenate: View,
     ErrorContentOnConcatenate: View,
     ItemView: View>(
        pagingVStack: StatefulLazyVStack<
        Key,
        Item,
        LoadingContent,
        EmptyContent,
        ErrorContent,
        LoadingContentOnConcatenate,
        ErrorContentOnConcatenate,
        ItemView>) {
            thenTheViewIsShown(pagingVStack: pagingVStack)
        }
    
    @MainActor
    private func givenAnAppendLoadingPagingItem() -> LazyPagingItems<String, FakeItemPagingSource> {
        FakeAppendLoadingLazyPager()
    }
    
    private func whenAppendLoadingPageItemInView(lazyPager: LazyPagingItems<String, FakeItemPagingSource>) -> StatefulLazyVStack<
        String,
        FakeItemPagingSource,
        EmptyView,
        EmptyView,
        EmptyView,
        ViewShown,
        EmptyView,
        EmptyView> {
            return StatefulLazyVStack<
                String,
                FakeItemPagingSource,
                EmptyView,
                EmptyView,
                EmptyView,
                ViewShown,
                EmptyView,
                EmptyView>(
                    lazyPager: lazyPager,
                    loadingContent: { EmptyView() },
                    emptyContent: { EmptyView() },
                    errorContent: { _ in EmptyView() },
                    loadingContentOnConcatenate: { ViewShown() },
                    errorContentOnConcatenate: { _ in EmptyView() }
                ) { _ in
                    EmptyView()
                }
        }
    
    private func thenTheLoadingContentOnConcatenateIsShown
    <Key,
     Item: Identifiable & Hashable,
     LoadingContent: View,
     EmptyContent: View,
     ErrorContent: View,
     LoadingContentOnConcatenate: View,
     ErrorContentOnConcatenate: View,
     ItemView: View>(
        pagingVStack: StatefulLazyVStack<
        Key,
        Item,
        LoadingContent,
        EmptyContent,
        ErrorContent,
        LoadingContentOnConcatenate,
        ErrorContentOnConcatenate,
        ItemView>) {
            thenTheViewIsShown(pagingVStack: pagingVStack)
        }
    
    private func thenTheViewIsShown
    <Key,
     Item: Identifiable & Hashable,
     LoadingContent: View,
     EmptyContent: View,
     ErrorContent: View,
     LoadingContentOnConcatenate: View,
     ErrorContentOnConcatenate: View,
     ItemView: View>(
        pagingVStack: StatefulLazyVStack<
        Key,
        Item,
        LoadingContent,
        EmptyContent,
        ErrorContent,
        LoadingContentOnConcatenate,
        ErrorContentOnConcatenate,
        ItemView>) {
            let shownView = try? pagingVStack.inspect().find(ViewShown.self)
            XCTAssertNotNil(shownView)
        }
    
    @MainActor
    private func givenAnErrorPagingItem() -> LazyPagingItems<String, FakeItemPagingSource> {
        FakeErrorLazyPager()
    }
    
    private func whenErrorPageItemInView(lazyPager: LazyPagingItems<String, FakeItemPagingSource>) -> StatefulLazyVStack<
        String,
        FakeItemPagingSource,
        EmptyView,
        EmptyView,
        ViewShown,
        EmptyView,
        EmptyView,
        EmptyView> {
            return StatefulLazyVStack<
                String,
                FakeItemPagingSource,
                EmptyView,
                EmptyView,
                ViewShown,
                EmptyView,
                EmptyView,
                EmptyView>(
                    lazyPager: lazyPager,
                    loadingContent: { EmptyView() },
                    emptyContent: { EmptyView() },
                    errorContent: { _ in ViewShown() },
                    loadingContentOnConcatenate: { EmptyView() },
                    errorContentOnConcatenate: { _ in EmptyView() }
                ) { _ in
                    EmptyView()
                }
        }
    
    private func thenTheErrorContentIsShown
    <Key,
     Item: Identifiable & Hashable,
     LoadingContent: View,
     EmptyContent: View,
     ErrorContent: View,
     LoadingContentOnConcatenate: View,
     ErrorContentOnConcatenate: View,
     ItemView: View>(
        pagingVStack: StatefulLazyVStack<
        Key,
        Item,
        LoadingContent,
        EmptyContent,
        ErrorContent,
        LoadingContentOnConcatenate,
        ErrorContentOnConcatenate,
        ItemView>) {
            thenTheViewIsShown(pagingVStack: pagingVStack)
        }
    
    @MainActor
    private func givenAnAppendErrorPagingItem() -> LazyPagingItems<String, FakeItemPagingSource> {
        FakeAppendErrorLazyPager()
    }
    
    private func whenAppendErrorPageItemInView(lazyPager: LazyPagingItems<String, FakeItemPagingSource>) -> StatefulLazyVStack<
        String,
        FakeItemPagingSource,
        EmptyView,
        EmptyView,
        EmptyView,
        EmptyView,
        ViewShown,
        EmptyView> {
            return StatefulLazyVStack<
                String,
                FakeItemPagingSource,
                EmptyView,
                EmptyView,
                EmptyView,
                EmptyView,
                ViewShown,
                EmptyView>(
                    lazyPager: lazyPager,
                    loadingContent: { EmptyView() },
                    emptyContent: { EmptyView() },
                    errorContent: { _ in EmptyView() },
                    loadingContentOnConcatenate: { EmptyView() },
                    errorContentOnConcatenate: { _ in ViewShown() }
                ) { _ in
                    EmptyView()
                }
        }
    
    private func thenTheAppendErrorContentIsShown
    <Key,
     Item: Identifiable & Hashable,
     LoadingContent: View,
     EmptyContent: View,
     ErrorContent: View,
     LoadingContentOnConcatenate: View,
     ErrorContentOnConcatenate: View,
     ItemView: View>(
        pagingVStack: StatefulLazyVStack<
        Key,
        Item,
        LoadingContent,
        EmptyContent,
        ErrorContent,
        LoadingContentOnConcatenate,
        ErrorContentOnConcatenate,
        ItemView>) {
            thenTheViewIsShown(pagingVStack: pagingVStack)
        }
    
    @MainActor
    private func givenAnEmptyPagingItem() -> LazyPagingItems<String, FakeItemPagingSource> {
        FakeEmptyLazyPager()
    }
    
    private func whenEmptyPageItemInView(lazyPager: LazyPagingItems<String, FakeItemPagingSource>) -> StatefulLazyVStack<
        String,
        FakeItemPagingSource,
        EmptyView,
        ViewShown,
        EmptyView,
        EmptyView,
        EmptyView,
        EmptyView> {
            return StatefulLazyVStack<
                String,
                FakeItemPagingSource,
                EmptyView,
                ViewShown,
                EmptyView,
                EmptyView,
                EmptyView,
                EmptyView>(
                    lazyPager: lazyPager,
                    loadingContent: { EmptyView() },
                    emptyContent: { ViewShown() },
                    errorContent: { _ in EmptyView() },
                    loadingContentOnConcatenate: { EmptyView() },
                    errorContentOnConcatenate: { _ in EmptyView() }
                ) { _ in
                    EmptyView()
                }
        }
    
    private func thenTheEmptyContentIsShown
    <Key,
     Item: Identifiable & Hashable,
     LoadingContent: View,
     EmptyContent: View,
     ErrorContent: View,
     LoadingContentOnConcatenate: View,
     ErrorContentOnConcatenate: View,
     ItemView: View>(
        pagingVStack: StatefulLazyVStack<
        Key,
        Item,
        LoadingContent,
        EmptyContent,
        ErrorContent,
        LoadingContentOnConcatenate,
        ErrorContentOnConcatenate,
        ItemView>) {
            thenTheViewIsShown(pagingVStack: pagingVStack)
        }
    
    @MainActor
    private func givenAFilledPagingItem() -> LazyPagingItems<String, FakeItemPagingSource> {
        FakeFilledLazyPager()
    }
    
    private func whenFilledPageItemInView(
        lazyPager: LazyPagingItems<String, FakeItemPagingSource>
    ) -> StatefulLazyVStack<
        String,
        FakeItemPagingSource,
        EmptyView,
        EmptyView,
        EmptyView,
        EmptyView,
        EmptyView,
        ViewShown> {
            return StatefulLazyVStack<
                String,
                FakeItemPagingSource,
                EmptyView,
                EmptyView,
                EmptyView,
                EmptyView,
                EmptyView,
                ViewShown>(
                    lazyPager: lazyPager,
                    loadingContent: { EmptyView() },
                    emptyContent: { EmptyView() },
                    errorContent: { _ in EmptyView() },
                    loadingContentOnConcatenate: { EmptyView() },
                    errorContentOnConcatenate: { _ in EmptyView() }
                ) { _ in
                    ViewShown()
                }
        }
    
    private func thenTheFilledContentIsShown
    <Key,
     Item: Identifiable & Hashable,
     LoadingContent: View,
     EmptyContent: View,
     ErrorContent: View,
     LoadingContentOnConcatenate: View,
     ErrorContentOnConcatenate: View,
     ItemView: View>(
        pagingVStack: StatefulLazyVStack<
        Key,
        Item,
        LoadingContent,
        EmptyContent,
        ErrorContent,
        LoadingContentOnConcatenate,
        ErrorContentOnConcatenate,
        ItemView>) {
            thenTheViewIsShown(pagingVStack: pagingVStack)
        }
}

private struct ViewShown: View {
    var body: some View {
        EmptyView()
    }
}
