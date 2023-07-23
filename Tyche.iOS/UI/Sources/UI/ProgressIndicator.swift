import SwiftUI

public struct ProgressIndicator: View {
    let difference: Int
    
    public init(difference: Int) {
        self.difference = difference
    }
    
    public var body: some View {
        if difference > 0 {
            UpProgressIndicator(progress: difference)
        } else if difference < 0 {
            DownProgressIndicator(progress: difference)
        } else {
            StableProgressIndicator()
        }
    }
}

private struct UpProgressIndicator: View {
    let progress: Int
    
    var body: some View {
        HStack(spacing: 0) {
            ResourceScheme.arrowUpward.image
                .resizable()
                .frame(width: 24, height: 24)
            Text(String(abs(progress)))
                .font(.footnote)
        }
        .foregroundColor(ColorScheme.positive.color)
    }
}

private struct DownProgressIndicator: View {
    let progress: Int
    
    var body: some View {
        HStack(spacing: 0) {
            ResourceScheme.arrowDownward.image
                .resizable()
                .frame(width: 24, height: 24)
            Text(String(abs(progress)))
                .font(.footnote)
        }
        .foregroundColor(ColorScheme.negative.color)
    }
}

private struct StableProgressIndicator: View {
    var body: some View {
        ResourceScheme.horizontalRule.image
            .resizable()
            .frame(width: 24, height: 24)
            .foregroundColor(ColorScheme.neutral.color)
    }
}

struct ProgressIndicator_Previews: PreviewProvider {
    static var previews: some View {
        ProgressIndicator(difference: 0)
    }
}
