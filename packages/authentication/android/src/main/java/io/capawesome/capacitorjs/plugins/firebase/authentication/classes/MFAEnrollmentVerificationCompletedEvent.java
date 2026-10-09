package io.capawesome.capacitorjs.plugins.firebase.authentication.classes;

import androidx.annotation.Nullable;
import com.getcapacitor.JSObject;

public class MFAEnrollmentVerificationCompletedEvent {

    @Nullable
    private String verificationCode;

    public MFAEnrollmentVerificationCompletedEvent(@Nullable String verificationCode) {
        this.verificationCode = verificationCode;
    }

    @Nullable
    public String getVerificationCode() {
        return verificationCode;
    }

    public JSObject toJSObject() {
        JSObject result = new JSObject();
        result.put("verificationCode", this.verificationCode);
        return result;
    }
}
