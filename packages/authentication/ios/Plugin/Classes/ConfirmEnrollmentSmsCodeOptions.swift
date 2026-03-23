import Foundation
import FirebaseCore
import FirebaseAuth

@objc class ConfirmEnrollmentSmsCodeOptions: ConfirmVerificationCodeOptions {
    private var user: User

    init(verificationId: String, verificationCode: String, user: User) {
        self.user = user
        super.init(verificationId: verificationId, verificationCode: verificationCode)
    }

    func getUser() -> User {
        return self.user
    }
}