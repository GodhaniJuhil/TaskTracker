import SwiftUI
import FirebaseCore
import Shared

@main
struct iOSApp: App {
    init() {
        // Reads GoogleService-Info.plist from the app bundle (kept out of git; CI writes it from a secret).
        FirebaseApp.configure()
        _ = KoinInitKt.doInitKoin(config: nil)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
