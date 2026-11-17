package uk.gov.di.authentication.oidc.entity;

public enum AuthErrorCodes {
    SSAD_ERROR("account_creation_required");
    private final String value;

    AuthErrorCodes(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
