import DataPool

extension PoolLayout {
    func toPoolLayoutModel() -> PoolLayoutModel {
        PoolLayoutModel(
            id: id,
            name: name,
            startDateTime: startDateTime
        )
    }
}
