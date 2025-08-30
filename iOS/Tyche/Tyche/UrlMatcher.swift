import Foundation

private let placeholderRE = try! NSRegularExpression(pattern: #"\{([A-Za-z_][A-Za-z0-9_]*)(?::([^}]+))?\}"#)

public func matchURL(_ urlString: String, to pattern: String) -> [String:String]? {
    guard
        let p = URLComponents(string: pattern),
        let scheme = p.scheme, let host = p.host,
        let u = URLComponents(string: urlString),
        u.scheme == scheme, u.host == host
    else { return nil }

    let rawPath = p.path.isEmpty ? "/" : p.path
    let full = NSRange(rawPath.startIndex..<rawPath.endIndex, in: rawPath)

    var names: [String] = []
    var out = ""
    var cursor = rawPath.startIndex

    for m in placeholderRE.matches(in: rawPath, range: full) {
        let r = Range(m.range, in: rawPath)!
        out += NSRegularExpression.escapedPattern(for: String(rawPath[cursor..<r.lowerBound]))
        let name = String(rawPath[Range(m.range(at: 1), in: rawPath)!])
        names.append(name)
        let hasConstraint = m.range(at: 2).location != NSNotFound
        let constraint = hasConstraint
            ? String(rawPath[Range(m.range(at: 2), in: rawPath)!])
            : "[^/]+"
        out += "(" + constraint + ")"
        cursor = r.upperBound
    }
    out += NSRegularExpression.escapedPattern(for: String(rawPath[cursor...]))

    let pathRegex = try! NSRegularExpression(pattern: "^\(out)$")
    let path = u.path.isEmpty ? "/" : u.path
    let range = NSRange(path.startIndex..<path.endIndex, in: path)
    guard let m = pathRegex.firstMatch(in: path, range: range) else { return nil }

    var params: [String:String] = [:]
    for (i, name) in names.enumerated() {
        let gr = m.range(at: i + 1)
        guard let rr = Range(gr, in: path) else { continue }
        let raw = String(path[rr])
        params[name] = raw.removingPercentEncoding ?? raw
    }
    return params
}
