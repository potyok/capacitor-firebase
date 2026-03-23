import Foundation
import FirebaseAuth
import FirebaseCore

@objc class VerifySecondFactorOptions: NSObject {
    private var resolver: MultiFactorResolver
    private var hint: PhoneMultiFactorInfo

    init(resolver: MultiFactorResolver, hint: PhoneMultiFactorInfo) {
        self.resolver = resolver
        self.hint = hint
    }

    func getResolver() -> MultiFactorResolver {
        return self.resolver
    }

    func getHint() -> PhoneMultiFactorInfo {
        return self.hint
    }
}