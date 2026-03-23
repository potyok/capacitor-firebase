import Foundation
import Capacitor
import FirebaseCore
import FirebaseAuth

class PhoneMFAHandler: NSObject {
    private var pluginImplementation: FirebaseAuthentication

    init(_ pluginImplementation: FirebaseAuthentication) {
        self.pluginImplementation = pluginImplementation
        super.init()
    }

    func verifySecondFactor(_ options: VerifySecondFactorOptions) {
        PhoneAuthProvider.provider().verifyPhoneNumber(
            with: options.getHint(),
            uiDelegate: nil,
            multiFactorSession: options.getResolver().session
        ) { (verificationId, error) in
            if let error = error {
                self.pluginImplementation.handleMFAVerificationFailed(error)
            } else {
                self.pluginImplementation.handleMFACodeSent(verificationId ?? "")
            }
        }
    }

    func enrollSecondFactor(_ options: EnrollSecondFactorOptions) {
        PhoneAuthProvider.provider().verifyPhoneNumber(
            options.getPhoneNumber(),
            uiDelegate: nil,
            multiFactorSession: options.getSession()
        ) { (verificationId, error) in
            if let error = error {
                self.pluginImplementation.handleMFAEnrollmentVerificationFailed(error)
            } else {
                self.pluginImplementation.handleMFACodeSent(verificationId ?? "")
            }
        }
    }

    func confirmSmsCode(_ options: ConfirmSmsCodeOptions) {
        let credential = PhoneAuthProvider.provider().credential(
            withVerificationID: options.getVerificationId(),
            verificationCode: options.getVerificationCode())
        let assertion = PhoneMultiFactorGenerator.assertion(with: credential)
        let resolver = options.getResolver()
        resolver.resolveSignIn(with: assertion) { (authResult, error) in
            if let error = error {
                self.pluginImplementation.handleMFAEnrollmentVerificationFailed(error)
            } else {
                let result = FirebaseAuthenticationHelper.createSignInResult(credential: authResult?.credential, user: authResult?.user,
                                                                         idToken: nil, nonce: nil, accessToken: nil,
                                                                         additionalUserInfo: authResult?.additionalUserInfo)
                self.pluginImplementation.handleMFAVerificationCompleted(result)
            }
        }
    }

    func confirmEnrollmentSmsCode(_ options: ConfirmEnrollmentSmsCodeOptions) {
        let credential = PhoneAuthProvider.provider().credential(
            withVerificationID: options.getVerificationId(),
            verificationCode: options.getVerificationCode())
        let assertion = PhoneMultiFactorGenerator.assertion(with: credential)
        options.getUser().multiFactor.enroll(with: assertion, displayName: "personal-phone-number") { (error) in
            if let error = error {
                self.pluginImplementation.handleMFAEnrollmentVerificationFailed(error)
            } else {
                let result = JSObject()
                self.pluginImplementation.handleMFAEnrollmentVerificationCompleted(result)
            }
        }
    }
}