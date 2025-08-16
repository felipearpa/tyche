struct CreatePoolModel {
    let poolLayoutId: String
    let poolName: String
    let poolId: String?
}


private let _emptyCreatePoolModel = CreatePoolModel(
    poolLayoutId: "",
    poolName: "",
    poolId: nil
)

func emptyCreatePoolModel() -> CreatePoolModel {
    return _emptyCreatePoolModel
}

extension CreatePoolModel {
    func copy(_ build: (inout Builder) -> Void) -> CreatePoolModel {
        var builder = Builder(original: self)
        build(&builder)
        return builder.build()
    }

    struct Builder {
        var poolLayoutId: String
        var poolName: String
        var poolId: String?

        fileprivate init(original: CreatePoolModel) {
            self.poolLayoutId = original.poolLayoutId
            self.poolName = original.poolName
            self.poolId = original.poolId
        }

        fileprivate func build() -> CreatePoolModel {
            CreatePoolModel(
                poolLayoutId: poolLayoutId,
                poolName: poolName,
                poolId: poolId
            )
        }
    }
}
