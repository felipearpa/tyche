public protocol LoginStorage {
    func store(loginProfile: LoginProfile) throws
    
    func get() throws -> LoginProfile?
}
