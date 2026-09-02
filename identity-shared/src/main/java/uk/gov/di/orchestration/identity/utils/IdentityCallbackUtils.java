package uk.gov.di.orchestration.identity.utils;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.orchestration.identity.exceptions.IdentityCallbackException;
import uk.gov.di.orchestration.shared.entity.LevelOfConfidence;

import java.util.List;
import java.util.Optional;

import static uk.gov.di.orchestration.shared.entity.IdentityClaims.VOT;
import static uk.gov.di.orchestration.shared.entity.IdentityClaims.VTM;

public class IdentityCallbackUtils {
    private IdentityCallbackUtils() {}

    private static final Logger LOG = LogManager.getLogger(IdentityCallbackUtils.class);

    public static Optional<ErrorObject> validateUserIdentityResponse(
            UserInfo userIdentityUserInfo,
            List<LevelOfConfidence> requestedLoCs,
            String trustmarkURL)
            throws IdentityCallbackException {
        LOG.info("Validating userinfo response");
        for (LevelOfConfidence loc : requestedLoCs) {
            if (loc.getValue().equals(userIdentityUserInfo.getClaim(VOT.getValue()))) {

                if (!trustmarkURL.equals(userIdentityUserInfo.getClaim(VTM.getValue()))) {
                    LOG.warn("VTM does not contain expected trustmark URL");
                    throw new IdentityCallbackException("Identity trustmark is invalid");
                }
                return Optional.empty();
            }
        }
        LOG.warn("User identity response missing vot or vot not in vtr list.");
        return Optional.of(OAuth2Error.ACCESS_DENIED);
    }
}
