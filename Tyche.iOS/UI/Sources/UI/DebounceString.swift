import Combine
import Dispatch

public final class DebounceString: ObservableObject {
    @Published public var text: String = ""
    @Published public var debouncedText: String = ""
    private var bag = Set<AnyCancellable>()

    public init(dueTime: DispatchQueue.SchedulerTimeType.Stride) {
        $text
            .removeDuplicates()
            .debounce(for: dueTime, scheduler: DispatchQueue.main)
            .sink(receiveValue: { [weak self] value in
                self?.debouncedText = value
            })
            .store(in: &bag)
    }
}
