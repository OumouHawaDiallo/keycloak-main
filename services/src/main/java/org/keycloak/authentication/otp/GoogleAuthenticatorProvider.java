package org.keycloak.authentication.otp;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;

public class GoogleAuthenticatorProvider implements OTPApplicationProviderFactory, OTPApplicationProvider {

    @Override
    public OTPApplicationProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public String getId() {
        return "google";
    }

    @Override
    public String getName() {
        return "totpAppGoogleName";
    }

    @Override
    public boolean supports(OTPPolicy policy) {
        if (policy.getDigits() != 6) {
            return false;
        }

        if (!policy.getAlgorithm().equals("HmacSHA1")) {
            return false;
        }

        return policy.getType().equals("totp") && policy.getPeriod() == 30;
    }

    @Override
    public void close() {
    }

}
