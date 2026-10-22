import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinInitKt.initKoin(config: nil)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}