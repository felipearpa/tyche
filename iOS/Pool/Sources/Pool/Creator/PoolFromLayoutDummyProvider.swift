import Foundation

func poolLayoutFakeModel() -> PoolLayoutModel {
    return PoolLayoutModel(
        id: String(repeating: "X", count: 15),
        name: String(repeating: "X", count: 25),
        startDateTime: Date()
    )
}

func poolLayoutDummyModel() -> PoolLayoutModel {
    return PoolLayoutModel(
        id: "pool123",
        name: "Champions League",
        startDateTime: Date()
    )
}

func poolLayoutDummyModels() -> [PoolLayoutModel] {
    return [
        PoolLayoutModel(
            id: "pool123",
            name: "Champions League",
            startDateTime: Date()
        ),
        PoolLayoutModel(
            id: "pool456",
            name: "Premier League",
            startDateTime: Date()
        ),
        PoolLayoutModel(
            id: "pool789",
            name: "World Cup",
            startDateTime: Date()
        )
    ]
}
