import SwiftUI

private let overlayColor = Color.gray
private let overlayOpacity = 0.7
private let dragOpenThreshold = 0.1

public struct DrawerView<MainContent: View, DrawerContent: View>: View {
    @Binding var isOpen: Bool
    private let mainContent: () -> MainContent
    private let drawerContent: () -> DrawerContent
    @State private var openFraction: CGFloat
    
    public init(isOpen: Binding<Bool>,
         @ViewBuilder mainContent: @escaping () -> MainContent,
         @ViewBuilder drawerContent: @escaping () -> DrawerContent
    ) {
        self._isOpen = isOpen
        self.openFraction = isOpen.wrappedValue ? 1 : 0
        self.mainContent = mainContent
        self.drawerContent = drawerContent
    }
    
    public var body: some View {
        GeometryReader { geometryProxy in
            let drawerWidth = 360.0
            ZStack(alignment: .topLeading) {
                mainContent()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .overlay(mainOverlay)
                drawerContent()
                    .frame(minWidth: drawerWidth, idealWidth: drawerWidth,
                           maxWidth: drawerWidth, maxHeight: .infinity)
                    .offset(x: xOffset(drawerWidth), y: 0)
            }
            .gesture(dragGesture(geometryProxy.size.width))
            .onChange(of: isOpen) { newIsOpen in
                withAnimation {
                    self.openFraction = newIsOpen ? 1 : 0
                }
            }
        }
    }
    
    private var mainOverlay: some View {
        overlayColor.opacity(openFraction)
            .onTapGesture {
                withAnimation {
                    isOpen.toggle()
                }
            }
    }
    
    private func dragGesture(_ mainWidth: CGFloat) -> some Gesture {
        return DragGesture()
            .onChanged { dragValue in handleDragChanged(dragValue, relativeTo: mainWidth)}
            .onEnded { dragValue in handleDragEnded(dragValue, relativeTo: mainWidth) }
    }
    
    private func handleDragChanged(_ value: DragGesture.Value, relativeTo mainWidth: CGFloat) {
        if isOpen, value.translation.width < 0 {
            openFraction = openFraction(value.translation.width, from: -mainWidth...0)
        } else if !isOpen, value.startLocation.x < mainWidth * dragOpenThreshold, value.translation.width > 0 {
            openFraction = openFraction(value.translation.width, from: 0...mainWidth)
        }
    }
    
    private func handleDragEnded(_ value: DragGesture.Value, relativeTo mainWidth: CGFloat) {
        if openFraction == 1 || openFraction == 0 { return }

        let fromRange = isOpen ? -mainWidth...0 : 0...mainWidth
        let predictedMoveX = value.predictedEndTranslation.width
        let predictedOpenFraction = openFraction(predictedMoveX, from: fromRange)
        if predictedOpenFraction > 0.5 {
            withAnimation {
                openFraction = 1
                isOpen = true
            }
        } else {
            withAnimation {
                openFraction = 0
                isOpen = false
            }
        }
    }
    
    private func xOffset(_ drawerWidth: CGFloat) -> CGFloat {
        remap(openFraction, from: 0...1, to: -drawerWidth...0)
    }
    
    private func openFraction(_ moveX: CGFloat, from source: ClosedRange<CGFloat>) -> CGFloat {
        remap(moveX, from: source, to: 0...1)
    }
    
    private func remap(_ value: CGFloat, from source: ClosedRange<CGFloat>, to target: ClosedRange<CGFloat>) -> CGFloat {
        let targetDiff = target.upperBound - target.lowerBound
        let sourceDiff = source.upperBound - source.lowerBound
        return (value - source.lowerBound) * targetDiff / sourceDiff + target.lowerBound
    }
}

#Preview {
    DrawerView(isOpen: .constant(true)) {
        Color.red
        Text("Show drawer")
    } drawerContent: {
        Color.blue
        Text("Drawer Content")
    }
}
