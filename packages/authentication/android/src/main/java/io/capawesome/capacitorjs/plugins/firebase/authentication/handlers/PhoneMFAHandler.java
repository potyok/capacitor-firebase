package io.capawesome.capacitorjs.plugins.firebase.authentication.handlers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorAssertion;
import com.google.firebase.auth.MultiFactorResolver;
import com.google.firebase.auth.MultiFactorSession;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneMultiFactorGenerator;
import com.google.firebase.auth.PhoneMultiFactorInfo;

import java.util.concurrent.TimeUnit;

import io.capawesome.capacitorjs.plugins.firebase.authentication.FirebaseAuthentication;
import io.capawesome.capacitorjs.plugins.firebase.authentication.classes.MFAEnrollmentVerificationCompletedEvent;
import io.capawesome.capacitorjs.plugins.firebase.authentication.classes.MFAVerificationCompletedEvent;
import io.capawesome.capacitorjs.plugins.firebase.authentication.classes.SignInResult;
import io.capawesome.capacitorjs.plugins.firebase.authentication.interfaces.EmptyResultCallback;
import io.capawesome.capacitorjs.plugins.firebase.authentication.interfaces.NonEmptyResultCallback;

public class PhoneMFAHandler {

    private FirebaseAuthentication pluginImplementation;

    @Nullable
    private PhoneAuthProvider.ForceResendingToken forceResendingToken;

    @Nullable
    private String lastVerificationId;

    public PhoneMFAHandler(FirebaseAuthentication pluginImplementation) {
        this.pluginImplementation = pluginImplementation;
    }

    public void verifySecondFactor(
            @NonNull PhoneMultiFactorInfo phoneHint,
            @NonNull MultiFactorResolver multiFactorResolver
    ) {
        lastVerificationId = null;
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(pluginImplementation.getFirebaseAuthInstance())
                .setMultiFactorSession(multiFactorResolver.getSession())
                .setMultiFactorHint(phoneHint)
                .setCallbacks(createCallbacks())
                .setActivity(pluginImplementation.getPlugin().getActivity())
                .setTimeout(60L, TimeUnit.SECONDS)
                .requireSmsValidation(true)
                .build());
    }

    public void confirmSmsCode(
            @NonNull MultiFactorResolver multiFactorResolver,
            @Nullable String verificationId,
            @Nullable String verificationCode,
            @NonNull NonEmptyResultCallback callback
    ) {
        if (verificationCode == null) {
            callback.error(new Exception("verificationCode is null"));
            return;
        }
        if (verificationId == null) {
            callback.error(new Exception("verificationId is null"));
            return;
        }
        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId, verificationCode);

        MultiFactorAssertion multiFactorAssertion =
                PhoneMultiFactorGenerator.getAssertion(credential);

        multiFactorResolver
            .resolveSignIn(multiFactorAssertion)
            .addOnCompleteListener(
                task -> {
                    if (task.isSuccessful()) {
                        SignInResult result = new SignInResult(task.getResult());
                        callback.success(result);
                    } else {
                        callback.error(task.getException());
                    }
                });
    }

    public void enrollSecondFactor(
            @NonNull MultiFactorSession multiFactorSession,
            @NonNull String phoneNumber
            ) {
        PhoneAuthOptions phoneAuthOptions =
            PhoneAuthOptions.newBuilder(pluginImplementation.getFirebaseAuthInstance())
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setMultiFactorSession(multiFactorSession)
                .setActivity(pluginImplementation.getPlugin().getActivity())
                .setCallbacks(createEnrollmentCallbacks())
                .build();
        PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions);
    }

    public void confirmEnrollmentSmsCode(
            @Nullable String verificationId,
            @Nullable String verificationCode,
            @NonNull EmptyResultCallback callback
    ) {
        if (verificationCode == null) {
            callback.error(new Exception("verificationCode is null"));
            return;
        }
        if (verificationId == null) {
            callback.error(new Exception("verificationId is null"));
            return;
        }
        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId, verificationCode);
        MultiFactorAssertion multiFactorAssertion = PhoneMultiFactorGenerator.getAssertion(credential);
        FirebaseUser user = pluginImplementation.getCurrentUser();
        if (user == null) {
            callback.error(new Exception("user is null"));
            return;
        }
        user.getMultiFactor()
            .enroll(multiFactorAssertion, "personal-phone-number")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.success();
                    } else {
                        callback.error(task.getException());
                    }
                });
    }

    public NonEmptyResultCallback<SignInResult> createMFAVerificationCompletedCallback(
            @Nullable String smsCode
    ) {
        return new NonEmptyResultCallback<>() {
            @Override
            public void success(SignInResult result) {
                MFAVerificationCompletedEvent event = new MFAVerificationCompletedEvent(
                        result.getUser(),
                        result.getCredential(),
                        result.getAdditionalUserInfo(),
                        smsCode
                );
                pluginImplementation.handleMFAVerificationCompleted(event);
            }

            @Override
            public void error(Exception exception) {
                pluginImplementation.handleMFAVerificationFailed(exception);
            }
        };

    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks createCallbacks(
    ) {
        return new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                NonEmptyResultCallback<SignInResult> callback = createMFAVerificationCompletedCallback(credential.getSmsCode());

                pluginImplementation.handleConfirmSmsCode(lastVerificationId, credential.getSmsCode(), callback);

            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException exception) {
                pluginImplementation.handleMFAVerificationFailed(exception);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                forceResendingToken = token;
                lastVerificationId = verificationId;
                pluginImplementation.handleMFACodeSent(verificationId);
            }
        };
    }

    public EmptyResultCallback createEnrollmentVerificationCompletedCallback(
            @Nullable String smsCode
    ) {
        return new EmptyResultCallback() {
            @Override
            public void success() {
                MFAEnrollmentVerificationCompletedEvent event = new MFAEnrollmentVerificationCompletedEvent(
                        smsCode
                );
                pluginImplementation.handleMFAEnrollmentVerificationCompleted(event);
            }

            @Override
            public void error(Exception exception) {
                pluginImplementation.handleMFAEnrollmentVerificationFailed(exception);
            }
        };
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks createEnrollmentCallbacks(
    ) {
        return new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                EmptyResultCallback callback = createEnrollmentVerificationCompletedCallback(credential.getSmsCode());

                pluginImplementation.handleConfirmEnrollmentSmsCode(lastVerificationId, credential.getSmsCode(), callback);

            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException exception) {
                pluginImplementation.handleMFAEnrollmentVerificationFailed(exception);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                forceResendingToken = token;
                lastVerificationId = verificationId;
                pluginImplementation.handleMFACodeSent(verificationId);
            }
        };
    }

}


