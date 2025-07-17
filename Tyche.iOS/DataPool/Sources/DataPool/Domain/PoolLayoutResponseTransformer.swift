extension PoolLayoutResponse {
    func toPoolLayout() -> PoolLayout {
        PoolLayout(
            id: id,
            name: name,
            startDateTime: startDateTime
        )
    }
}
