import SwiftUI

public struct TrendIndicator: View {
    let difference: Int
    
    public init(difference: Int) {
        self.difference = difference
    }
    
    public var body: some View {
        if difference > 0 {
            UpTrendIndicator(progress: difference)
        } else if difference < 0 {
            DownTrendIndicator(progress: difference)
        } else {
            StableTrendIndicator()
        }
    }
}

private struct UpTrendIndicator: View {
    let progress: Int
    
    var body: some View {
        HStack(spacing: 0) {
            Image(.arrowUpward)
                .resizable()
                .frame(width: iconSize, height: iconSize)
            Text(String(abs(progress)))
                .font(.footnote)
        }
        .foregroundStyle(Color(.positive))
    }
}

private struct DownTrendIndicator: View {
    let progress: Int
    
    var body: some View {
        HStack(spacing: 0) {
            Image(.arrowDownward)
                .resizable()
                .frame(width: iconSize, height: iconSize)
            Text(String(abs(progress)))
                .font(.footnote)
        }
        .foregroundStyle(Color(.negative))
    }
}

private struct StableTrendIndicator: View {
    var body: some View {
        Image(.horizontalRule)
            .resizable()
            .frame(width: iconSize, height: iconSize)
            .foregroundStyle(Color(.neutral))
    }
}

private let iconSize: CGFloat = 24

#Preview {
    TrendIndicator(difference: 0)
}

#Preview {
    TrendIndicator(difference: 1)
}

#Preview {
    TrendIndicator(difference: -1)
}
