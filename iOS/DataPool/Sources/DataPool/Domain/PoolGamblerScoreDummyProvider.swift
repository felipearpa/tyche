func dummyPoolGamblerScore() -> PoolGamblerScore {
    PoolGamblerScore(
        poolId: "A3C2E1",
        poolName: "Neptune World Series 2023",
        gamblerId: "YF23H1",
        gamblerUsername: "neptune-player",
        position: 4,
        beforePosition: 3,
        score: 8
    )
}

func dummyPoolGamblerScores() -> [PoolGamblerScore] {
    return [
        PoolGamblerScore(
            poolId: "A3C2E1",
            poolName: "Neptune World Series 2023",
            gamblerId: "YF23H1",
            gamblerUsername: "neptune-player",
            position: 4,
            beforePosition: 3,
            score: 8
        ),
        PoolGamblerScore(
            poolId: "X4D7B9",
            poolName: "Ares Euro Championship 2024",
            gamblerId: "ZT94Q5",
            gamblerUsername: "ares-bettor",
            position: 2,
            beforePosition: nil,
            score: 10
        ),
        PoolGamblerScore(
            poolId: "J1K5M7",
            poolName: "Hermes International Cup 2023",
            gamblerId: "XN28J4",
            gamblerUsername: "hermes-punter",
            position: 1,
            beforePosition: 4,
            score: 6
        ),
        PoolGamblerScore(
            poolId: "P6Z8V4",
            poolName: "Athena Olympics 2024",
            gamblerId: "M9L2S7",
            gamblerUsername: "athena-challenger",
            position: nil,
            beforePosition: nil,
            score: nil
        )
    ]
}
