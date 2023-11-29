// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "DataBet",
    platforms: [.iOS(.v16)],
    products: [
        // Products define the executables and libraries a package produces, making them visible to other packages.
        .library(
            name: "DataBet",
            targets: ["DataBet"]),
    ],
    dependencies: [
        .package(path: "../Core"),
        .package(path: "../Session"),
    ],
    targets: [
        // Targets are the basic building blocks of a package, defining a module or a test suite.
        // Targets can depend on other targets in this package and products from dependencies.
        .target(
            name: "DataBet",
            dependencies: [
                "Core",
                "Session",
            ]),
        .testTarget(
            name: "DataBetTests",
            dependencies: ["DataBet"]),
    ]
)
