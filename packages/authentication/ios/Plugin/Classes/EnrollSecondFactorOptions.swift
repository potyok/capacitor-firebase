import Foundation
import FirebaseAuth
import FirebaseCore

@objc class EnrollSecondFactorOptions: NSObject {
    private var session: MultiFactorSession
    private var phoneNumber: String

    init(session: MultiFactorSession, phoneNumber: String) {
        self.session = session
        self.phoneNumber = phoneNumber
    }

    func getSession() -> MultiFactorSession {
        return self.session
    }

    func getPhoneNumber() -> String {
        return self.phoneNumber
    }
}
