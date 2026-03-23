import Foundation
import FirebaseCore
import FirebaseAuth

@objc class ConfirmSmsCodeOptions: ConfirmVerificationCodeOptions {
    private var resolver: MultiFactorResolver

    init(verificationId: String, verificationCode: String, resolver: MultiFactorResolver) {
        self.resolver = resolver
        super.init(verificationId: verificationId, verificationCode: verificationCode)
    }

    func getResolver() -> MultiFactorResolver {
        return self.resolver
    }
}