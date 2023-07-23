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
        .package(path: "../Core"),
    ],
    targets: [
        // Targets are the basic building blocks of a package. A target can define a module or a test suite.
        // Targets can depend on other targets in this package, and on products in packages this package depends on.
        .target(
            name: "UI",
            dependencies: ["Core"],
            resources: [
                .process("Localizable/en.lproj/Localizable.strings"),
                .process("Localizable/es-CO.lproj/Localizable.strings"),
                .process("Resource/Assets.xcassets"),
            ]),
        .testTarget(
            name: "UITests",
            dependencies: ["UI"]),
    ]
)
