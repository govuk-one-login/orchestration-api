package uk.gov.di.orchestration.identity.utils;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.di.orchestration.identity.exceptions.IdentityCallbackException;
import uk.gov.di.orchestration.shared.entity.LevelOfConfidence;

import java.util.List;

import static com.nimbusds.oauth2.sdk.OAuth2Error.ACCESS_DENIED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.di.orchestration.identity.utils.IdentityCallbackUtils.validateUserIdentityResponse;

class IdentityCallbackUtilsTest {

    private static final String TRUSTMARK_URL = "http://test.com/trustmark";
    private static final Subject SUBJECT =
            new Subject("urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
    private static final String SUCCESSFUL_USER_INFO_HTTP_RESPONSE_CONTENT =
            "{"
                    + " \"sub\": \""
                    + SUBJECT
                    + "\","
                    + " \"vot\": \"P2\","
                    + " \"vtm\": \"<trust mark>\""
                    + "}";
    private static final String BACKEND_URI = "http://test-backend-uri";
    private static final BearerAccessToken BEARER_ACCESS_TOKEN = new BearerAccessToken();
    private static final TokenResponse SUCCESSFUL_TOKEN_RESPONSE =
            new AccessTokenResponse(new Tokens(BEARER_ACCESS_TOKEN, null));

    @Nested
    class ValidateUserIdentityResponse {

        @Test
        void shouldReturnAccessDeniedIfVotIsNotContainedInRequestedLoCs()
                throws IdentityCallbackException {
            var userInfo = new UserInfo(SUBJECT);
            userInfo.setClaim("vot", LevelOfConfidence.MEDIUM_LEVEL.getValue());

            var result =
                    validateUserIdentityResponse(
                            userInfo, List.of(LevelOfConfidence.NONE), TRUSTMARK_URL);

            assertTrue(result.isPresent());
            assertThat(result.get(), equalTo(ACCESS_DENIED));
        }

        @Test
        void shouldThrowExceptionWhenVtmDoesNotEqualTrustmarkUrl() {
            var userInfo = new UserInfo(SUBJECT);
            userInfo.setClaim("vot", LevelOfConfidence.MEDIUM_LEVEL.getValue());
            userInfo.setClaim("vtm", "http://different-trustmark-url");

            assertThrows(
                    IdentityCallbackException.class,
                    () ->
                            validateUserIdentityResponse(
                                    userInfo,
                                    List.of(LevelOfConfidence.MEDIUM_LEVEL),
                                    TRUSTMARK_URL));
        }

        @Test
        void shouldNotReturnErrorIfVotIsInRequestedLoCsAndVtmMatchesTrustmarkUrl()
                throws IdentityCallbackException {
            var userInfo = new UserInfo(SUBJECT);
            userInfo.setClaim("vot", LevelOfConfidence.MEDIUM_LEVEL.getValue());
            userInfo.setClaim("vtm", TRUSTMARK_URL);

            var result =
                    validateUserIdentityResponse(
                            userInfo, List.of(LevelOfConfidence.MEDIUM_LEVEL), TRUSTMARK_URL);

            assertTrue(result.isEmpty());
        }
    }
}
