protocol AvatarRemoteDataSource {
    func issueUploadUrl(accountId: String, request: AvatarUploadUrlRequest) async throws -> AvatarUploadUrlResponse
}
