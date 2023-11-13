import Combine
import Dispatch

public final class DebounceString: ObservableObject {
    @Published public var text: String = ""
    @Published public private(set) var debouncedText: String = ""
    private var bag = Set<AnyCancellable>()

    public init(dueTime: DispatchQueue.SchedulerTimeType.Stride, initialText: String = "") {
        text = initialText
        debouncedText = initialText
        
        $text
            .removeDuplicates()
            .debounce(for: dueTime, scheduler: DispatchQueue.main)
            .sink(receiveValue: { [weak self] newValue in
                if self?.debouncedText != newValue {
                    self?.debouncedText = newValue
                }
            })
            .store(in: &bag)
    }
}
