import CryptoKit
import Foundation
import ImageIO
import UIKit

/// How long a validated avatar (or a cached absence) stays fresh before the next access
/// triggers one coalesced conditional revalidation.
public let avatarFreshnessInterval: TimeInterval = 5 * 60

/// Decoded avatar entries are bucketed to stable pixel sides so nearby render sizes share
/// one variant, and capped at the 512-pixel upload size.
let avatarPixelBucketStride = 32
let avatarMaxPixelSize = 512

/// The combined decoded cost of retained avatar images may not exceed this budget.
let avatarMemoryBudget = 16 * 1024 * 1024

/// Rounds a measured pixel target up to its size bucket, capped at the source size.
func avatarPixelBucket(forTargetPixelSize target: CGFloat) -> Int {
    guard target > 0 else { return avatarMaxPixelSize }
    let pixels = Int(target.rounded(.up))
    let bucket = ((pixels + avatarPixelBucketStride - 1) / avatarPixelBucketStride) * avatarPixelBucketStride
    return min(max(bucket, avatarPixelBucketStride), avatarMaxPixelSize)
}

/// Decodes an avatar response at no more than `maxPixelSize` per side using Image I/O,
/// so a row-sized consumer never retains a full-resolution bitmap.
func decodeAvatarImage(data: Data, maxPixelSize: Int) -> UIImage? {
    let sourceOptions: [CFString: Any] = [kCGImageSourceShouldCache: false]
    guard let source = CGImageSourceCreateWithData(data as CFData, sourceOptions as CFDictionary) else {
        return nil
    }

    let thumbnailOptions: [CFString: Any] = [
        kCGImageSourceCreateThumbnailFromImageAlways: true,
        kCGImageSourceCreateThumbnailWithTransform: true,
        kCGImageSourceShouldCacheImmediately: true,
        kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
    ]
    guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, thumbnailOptions as CFDictionary) else {
        return nil
    }

    return UIImage(cgImage: cgImage)
}

/// Per-account replacement counter observed by `AccountAvatar` so every visible surface
/// re-requests from the shared store when an upload or a changed revalidation replaces
/// the photo. Mutated on the main actor only.
final class AvatarStoreRevisions: ObservableObject {
    static let shared = AvatarStoreRevisions()

    @Published private(set) var revisions: [String: Int] = [:]

    func advance(accountId: String) {
        revisions[accountId, default: 0] += 1
    }

    func revision(for accountId: String) -> Int {
        revisions[accountId] ?? 0
    }
}

/// The application-scoped decoded-avatar store behind every `AccountAvatar`.
///
/// Decoded images are keyed by account, process-local generation, and size bucket; a
/// cached larger variant satisfies a smaller request. Cost is accounted from decoded
/// pixel bytes with a 16 MiB ceiling and least-recently-used eviction, while `NSCache`
/// additionally discards entries under platform memory pressure. Compressed bytes stay
/// in `URLCache`; a fresh entry never touches the URL, a stale one is served immediately
/// while one conditional revalidation per account is coalesced in the background, and a
/// missing avatar is remembered for the same interval so surfaces do not repeat 404s.
public actor AvatarImageStore {
    public static let shared = AvatarImageStore()

    typealias Fetch = @Sendable (URLRequest) async throws -> (Data, URLResponse)

    private struct EntryKey: Hashable {
        let accountId: String
        let generation: Int
        let bucket: Int

        var nsString: NSString { "\(accountId)#\(generation)#\(bucket)" as NSString }
    }

    private final class CachedImage {
        let image: UIImage

        init(image: UIImage) {
            self.image = image
        }
    }

    private struct ValidationState {
        var validatedAt: Date
        var etag: String?
    }

    private let fetch: Fetch
    private let cachedResponse: @Sendable (URLRequest) -> CachedURLResponse?
    private let now: @Sendable () -> Date
    private let memoryBudget: Int
    private let freshnessInterval: TimeInterval
    private let notifyReplacement: @Sendable (String) async -> Void

    private let cache = NSCache<NSString, CachedImage>()
    private var order: [EntryKey] = []
    private var costs: [EntryKey: Int] = [:]
    private var totalCost = 0

    private var validations: [String: ValidationState] = [:]
    private var missing: [String: Date] = [:]
    private var generations: [String: Int] = [:]
    private var loads: [EntryKey: Task<UIImage?, Never>] = [:]
    private var revalidations: [String: Task<Void, Never>] = [:]

    private let maxMissingEntries = 512

    init(
        fetch: @escaping Fetch = { try await URLSession.shared.data(for: $0) },
        cachedResponse: @escaping @Sendable (URLRequest) -> CachedURLResponse? = {
            URLCache.shared.cachedResponse(for: $0)
        },
        now: @escaping @Sendable () -> Date = { Date() },
        memoryBudget: Int = avatarMemoryBudget,
        freshnessInterval: TimeInterval = avatarFreshnessInterval,
        notifyReplacement: @escaping @Sendable (String) async -> Void = { accountId in
            await MainActor.run { AvatarStoreRevisions.shared.advance(accountId: accountId) }
        }
    ) {
        self.fetch = fetch
        self.cachedResponse = cachedResponse
        self.now = now
        self.memoryBudget = memoryBudget
        self.freshnessInterval = freshnessInterval
        self.notifyReplacement = notifyReplacement
        cache.totalCostLimit = memoryBudget

        NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: nil
        ) { [weak self] _ in
            Task { await self?.evictAllDecodedImages() }
        }
    }

    /// Returns the avatar for `accountId` decoded to cover `targetPixelSize`, or nil for
    /// a missing avatar. Serves memory first, coalesces overlapping loads, and never
    /// touches the URL for a fresh entry or a fresh cached absence.
    public func image(accountId: String, targetPixelSize: CGFloat) async -> UIImage? {
        guard !accountId.isEmpty else { return nil }

        let generation = generations[accountId] ?? 0
        let bucket = avatarPixelBucket(forTargetPixelSize: targetPixelSize)

        if let retained = retainedImage(accountId: accountId, generation: generation, minBucket: bucket) {
            if !isFresh(validations[accountId]?.validatedAt) {
                scheduleRevalidation(accountId: accountId, generation: generation)
            }
            return retained
        }

        if let recordedAt = missing[accountId], isFresh(recordedAt) {
            return nil
        }

        let key = EntryKey(accountId: accountId, generation: generation, bucket: bucket)
        if let inFlight = loads[key] {
            return await inFlight.value
        }

        let load = Task { await self.performLoad(key: key) }
        loads[key] = load
        let image = await load.value
        loads[key] = nil
        return image
    }

    /// Seeds the store with the optimized local image after a successful upload PUT:
    /// advances the account's generation, replaces every retained variant, clears any
    /// cached absence, and notifies active consumers — all without an avatar GET. The
    /// JPEG bytes provide the S3 `ETag` so the next revalidation recognizes the object
    /// as unchanged.
    public func installLocalAvatar(_ image: UIImage, jpegData: Data, accountId: String) async {
        guard !accountId.isEmpty else { return }

        let previousGeneration = generations[accountId] ?? 0
        let generation = previousGeneration + 1
        generations[accountId] = generation
        purgeEntries(accountId: accountId, generation: previousGeneration)

        let key = EntryKey(accountId: accountId, generation: generation, bucket: avatarMaxPixelSize)
        insert(image, key: key)

        let digest = Insecure.MD5.hash(data: jpegData)
        let etag = "\"" + digest.map { String(format: "%02x", $0) }.joined() + "\""
        validations[accountId] = ValidationState(validatedAt: now(), etag: etag)
        missing[accountId] = nil

        await notifyReplacement(accountId)
    }

    /// Discards every decoded image; validation metadata survives so later requests can
    /// recover through `URLCache` without a network round trip.
    func evictAllDecodedImages() {
        cache.removeAllObjects()
        order = []
        costs = [:]
        totalCost = 0
    }

    /// Seconds until a cached absence for `accountId` lapses, or nil when no absence is
    /// recorded. Lets a visible surface schedule one retry instead of showing the
    /// fallback until it leaves the screen.
    public func missingRetryDelay(accountId: String) -> TimeInterval? {
        guard let recordedAt = missing[accountId] else { return nil }
        return max(freshnessInterval - now().timeIntervalSince(recordedAt), 0.1)
    }

    /// Decodes the just-uploaded JPEG at source size and installs it as the account's
    /// current avatar — the byte-oriented entry point for the upload application flow.
    public func installUploadedAvatar(jpegData: Data, accountId: String) async {
        guard let image = decodeAvatarImage(data: jpegData, maxPixelSize: avatarMaxPixelSize) else { return }
        await installLocalAvatar(image, jpegData: jpegData, accountId: accountId)
    }

    /// Awaits any in-flight background revalidations.
    func awaitRevalidations() async {
        while let task = revalidations.values.first {
            await task.value
        }
    }

    /// The combined accounted cost of retained decoded entries.
    var accountedCost: Int { totalCost }

    private func performLoad(key: EntryKey) async -> UIImage? {
        guard let request = avatarPhotoRequest(accountId: key.accountId) else { return nil }

        // Fresh validation with the decoded entry evicted: recover from URLCache alone —
        // but only when the cached bytes are the validated object. After an upload the
        // validation carries the new ETag while URLCache still holds the previous photo;
        // trusting it would resurrect the old image under the new generation.
        if isFresh(validations[key.accountId]?.validatedAt),
            let cached = cachedResponse(request),
            let cachedHttpResponse = cached.response as? HTTPURLResponse,
            cachedHttpResponse.statusCode == 200,
            matchesKnownEtag(cachedHttpResponse, accountId: key.accountId),
            let image = decodeAvatarImage(data: cached.data, maxPixelSize: key.bucket)
        {
            if (generations[key.accountId] ?? 0) == key.generation {
                insert(image, key: key)
            }
            return image
        }

        do {
            let (data, response) = try await fetch(request)
            guard let httpResponse = response as? HTTPURLResponse else { return nil }

            guard httpResponse.statusCode == 200 else {
                // Only a definitive not-found becomes a cached absence; a transient server
                // error stays retryable, like a transport failure.
                if httpResponse.statusCode == 404 || httpResponse.statusCode == 403 {
                    recordMissing(accountId: key.accountId)
                }
                return nil
            }

            guard let image = decodeAvatarImage(data: data, maxPixelSize: key.bucket) else { return nil }
            guard (generations[key.accountId] ?? 0) == key.generation else { return image }

            let etag = httpResponse.value(forHTTPHeaderField: "Etag")
            let knownEtag = validations[key.accountId]?.etag
            validations[key.accountId] = ValidationState(validatedAt: now(), etag: etag)
            missing[key.accountId] = nil

            // A load for a size not decoded yet can be the first to meet a replaced photo.
            // Adopting its ETag alone would leave the other sizes showing the old photo, and
            // every later revalidation would certify them against the new ETag.
            guard let knownEtag, knownEtag != etag else {
                insert(image, key: key)
                return image
            }

            let nextGeneration = key.generation + 1
            generations[key.accountId] = nextGeneration
            purgeEntries(accountId: key.accountId, generation: key.generation)
            insert(image, key: EntryKey(accountId: key.accountId, generation: nextGeneration, bucket: key.bucket))
            await notifyReplacement(key.accountId)
            return image
        } catch {
            // Transport failure: no negative entry, so connectivity recovery retries.
            return nil
        }
    }

    private func scheduleRevalidation(accountId: String, generation: Int) {
        guard revalidations[accountId] == nil else { return }
        revalidations[accountId] = Task {
            await self.performRevalidation(accountId: accountId, generation: generation)
        }
    }

    private func performRevalidation(accountId: String, generation: Int) async {
        defer { revalidations[accountId] = nil }

        guard let request = avatarPhotoRequest(accountId: accountId) else { return }

        do {
            let (data, response) = try await fetch(request)
            guard (generations[accountId] ?? 0) == generation else { return }
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                // The stale image outlives a failed or not-found revalidation.
                return
            }

            let etag = httpResponse.value(forHTTPHeaderField: "Etag")
            if let known = validations[accountId]?.etag, known == etag {
                validations[accountId]?.validatedAt = now()
                return
            }

            let bucket = largestRetainedBucket(accountId: accountId, generation: generation) ?? avatarMaxPixelSize
            guard let image = decodeAvatarImage(data: data, maxPixelSize: bucket) else { return }

            let nextGeneration = generation + 1
            generations[accountId] = nextGeneration
            purgeEntries(accountId: accountId, generation: generation)
            insert(image, key: EntryKey(accountId: accountId, generation: nextGeneration, bucket: bucket))
            validations[accountId] = ValidationState(validatedAt: now(), etag: etag)
            missing[accountId] = nil

            await notifyReplacement(accountId)
        } catch {
            // Offline revalidation retains the stale image; a later access retries.
        }
    }

    private func matchesKnownEtag(_ response: HTTPURLResponse, accountId: String) -> Bool {
        guard let knownEtag = validations[accountId]?.etag else { return true }
        return response.value(forHTTPHeaderField: "Etag") == knownEtag
    }

    private func retainedImage(accountId: String, generation: Int, minBucket: Int) -> UIImage? {
        var bucket = minBucket
        while bucket <= avatarMaxPixelSize {
            let key = EntryKey(accountId: accountId, generation: generation, bucket: bucket)
            if costs[key] != nil {
                if let cached = cache.object(forKey: key.nsString) {
                    touch(key)
                    return cached.image
                }
                // NSCache dropped it under memory pressure; reconcile the accounting.
                removeBookkeeping(key)
            }
            bucket += avatarPixelBucketStride
        }
        return nil
    }

    private func largestRetainedBucket(accountId: String, generation: Int) -> Int? {
        costs.keys
            .filter { $0.accountId == accountId && $0.generation == generation }
            .map(\.bucket)
            .max()
    }

    private func insert(_ image: UIImage, key: EntryKey) {
        let cost = decodedCost(of: image)

        if costs[key] != nil {
            cache.removeObject(forKey: key.nsString)
            removeBookkeeping(key)
        }

        while totalCost + cost > memoryBudget, let oldest = order.first {
            cache.removeObject(forKey: oldest.nsString)
            removeBookkeeping(oldest)
        }

        cache.setObject(CachedImage(image: image), forKey: key.nsString, cost: cost)
        costs[key] = cost
        totalCost += cost
        order.append(key)
    }

    private func decodedCost(of image: UIImage) -> Int {
        if let cgImage = image.cgImage {
            return max(cgImage.bytesPerRow * cgImage.height, 1)
        }
        let pixelWidth = Int(image.size.width * image.scale)
        let pixelHeight = Int(image.size.height * image.scale)
        return max(pixelWidth * pixelHeight * 4, 1)
    }

    private func touch(_ key: EntryKey) {
        guard let index = order.firstIndex(of: key) else { return }
        order.remove(at: index)
        order.append(key)
    }

    private func removeBookkeeping(_ key: EntryKey) {
        if let cost = costs.removeValue(forKey: key) {
            totalCost -= cost
        }
        if let index = order.firstIndex(of: key) {
            order.remove(at: index)
        }
    }

    private func purgeEntries(accountId: String, generation: Int) {
        let keys = costs.keys.filter { $0.accountId == accountId && $0.generation == generation }
        for key in keys {
            cache.removeObject(forKey: key.nsString)
            removeBookkeeping(key)
        }
    }

    private func recordMissing(accountId: String) {
        if missing.count >= maxMissingEntries, missing[accountId] == nil {
            if let oldest = missing.min(by: { $0.value < $1.value }) {
                missing[oldest.key] = nil
            }
        }
        missing[accountId] = now()
    }

    private func isFresh(_ validatedAt: Date?) -> Bool {
        guard let validatedAt else { return false }
        return now().timeIntervalSince(validatedAt) < freshnessInterval
    }
}
