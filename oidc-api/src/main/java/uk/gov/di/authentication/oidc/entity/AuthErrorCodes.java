package uk.gov.di.authentication.oidc.entity;

public enum AuthErrorCodes {
    SFAD_ERROR("account_creation_required");
    private final String value;

    AuthErrorCodes(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
