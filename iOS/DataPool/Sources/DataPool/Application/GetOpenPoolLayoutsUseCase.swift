import Core

public class GetOpenPoolLayoutsUseCase {
    private let poolLayoutRepository: PoolLayoutRepository

    public init(poolLayoutRepository: PoolLayoutRepository) {
        self.poolLayoutRepository = poolLayoutRepository
    }

    public func execute(
        next: String? = nil,
        searchText: String? = nil
    ) async -> Result<CursorPage<PoolLayout>, Error> {
        return await poolLayoutRepository.getOpenPoolLayouts(next: next, searchText: searchText)
    }
}
