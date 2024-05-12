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
            Image(.arrowUpward)
                .resizable()
                .frame(width: 24, height: 24)
            Text(String(abs(progress)))
                .font(.footnote)
        }
        .foregroundStyle(Color(.positive))
    }
}

private struct DownProgressIndicator: View {
    let progress: Int
    
    var body: some View {
        HStack(spacing: 0) {
            Image(.arrowDownward)
                .resizable()
                .frame(width: 24, height: 24)
            Text(String(abs(progress)))
                .font(.footnote)
        }
        .foregroundStyle(Color(.negative))
    }
}

private struct StableProgressIndicator: View {
    var body: some View {
        Image(.horizontalRule)
            .resizable()
            .frame(width: 24, height: 24)
            .foregroundStyle(Color(.neutral))
    }
}

#Preview {
    ProgressIndicator(difference: 0)
}
