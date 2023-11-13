public protocol LoginStorage {
    func store(loginProfile: LoginProfile) async throws
    
    func get() async throws -> LoginProfile?
}
