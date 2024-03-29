// swift-tools-version: 5.8
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "UI",
    defaultLocalization: "en",
    platforms: [.iOS(.v16)],
    products: [
        // Products define the executables and libraries a package produces, and make them visible to other packages.
        .library(
            name: "UI",
            targets: ["UI"]),
    ],
    dependencies: [
        // Dependencies declare other packages that this package depends on.
        // .package(url: /* package url */, from: "1.0.0"),
        .package(url: "https://github.com/markiv/SwiftUI-Shimmer.git", from: "1.4.0"),
        .package(path: "../Core"),
        .package(url: "https://github.com/nalexn/ViewInspector", from: "0.9.10"),
    ],
    targets: [
        // Targets are the basic building blocks of a package. A target can define a module or a test suite.
        // Targets can depend on other targets in this package, and on products in packages this package depends on.
        .target(
            name: "UI",
            dependencies: [
                .product(name: "Shimmer", package: "SwiftUI-Shimmer"),
                "Core",
            ],
            resources: [
                .process("Localizable/Localizable.xcstrings"),
                .process("Assets/Assets.xcassets"),
            ]),
        .testTarget(
            name: "UITests",
            dependencies: [
                "UI",
                "Core",
                "ViewInspector",
            ]),
    ]
)
