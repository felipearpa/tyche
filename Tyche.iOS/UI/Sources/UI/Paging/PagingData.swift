public struct PagingConfig {
    let prefetchDistance: Int
    
    public init(prefetchDistance: Int) {
        self.prefetchDistance = prefetchDistance
    }
}

public class PagingData<Key, Item> {
    public let pagingConfig: PagingConfig
    public let pagingSourceFactory: PagingSource<Key, Item>
    public let initialKey: Key?
    
    public init(
        pagingConfig: PagingConfig,
        pagingSourceFactory: PagingSource<Key, Item>,
        initialKey: Key? = nil
    ) {
        self.pagingConfig = pagingConfig
        self.pagingSourceFactory = pagingSourceFactory
        self.initialKey = initialKey
    }
}
