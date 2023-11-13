// swift-tools-version: 5.8
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Pool",
    defaultLocalization: "en",
    platforms: [.iOS(.v16)],
    products: [
        // Products define the executables and libraries a package produces, and make them visible to other packages.
        .library(
            name: "Pool",
            targets: ["Pool"]),
    ],
    dependencies: [
        // Dependencies declare other packages that this package depends on.
        // .package(url: /* package url */, from: "1.0.0"),
        .package(path: "../Core"),
        .package(path: "../UI"),
        .package(path: "../User"),
        .package(url: "https://github.com/Alamofire/Alamofire.git", from: "5.7.0"),
        .package(url: "https://github.com/Swinject/Swinject.git", from: "2.8.3"),
    ],
    targets: [
        // Targets are the basic building blocks of a package. A target can define a module or a test suite.
        // Targets can depend on other targets in this package, and on products in packages this package depends on.
        .target(
            name: "Pool",
            dependencies: [
                "Core",
                "UI",
                "User",
                "Alamofire",
                "Swinject",
            ],
            resources: [.process("Localizable/Localizable.xcstrings")]),
        .testTarget(
            name: "PoolTests",
            dependencies: ["Pool"]),
    ]
)
