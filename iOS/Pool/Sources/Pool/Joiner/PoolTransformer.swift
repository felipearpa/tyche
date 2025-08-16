import DataPool

extension Pool {
    func toPoolModel() -> PoolModel {
        PoolModel(id: id, name: name)
    }
}
