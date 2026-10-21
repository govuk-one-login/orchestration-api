package uk.gov.di.authentication.oidc.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.orchestration.shared.services.ConfigurationService;
import uk.gov.di.orchestration.shared.services.KmsConnectionService;
import uk.gov.di.orchestration.shared.services.SigningJwkCacheService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static uk.gov.di.orchestration.shared.helpers.ApiGatewayResponseHelper.generateApiGatewayProxyResponse;
import static uk.gov.di.orchestration.shared.helpers.LogLineHelper.attachIpAddressAndUserAgentToLogs;
import static uk.gov.di.orchestration.shared.helpers.LogLineHelper.attachTraceId;

public class AuthJwksHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ConfigurationService configurationService;
    private final SigningJwkCacheService signingJwkCacheService;
    private static final Logger LOG = LogManager.getLogger(AuthJwksHandler.class);

    public AuthJwksHandler(
            ConfigurationService configurationService,
            SigningJwkCacheService signingJwkCacheService) {
        this.configurationService = configurationService;
        this.signingJwkCacheService = signingJwkCacheService;
    }

    public AuthJwksHandler(ConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.signingJwkCacheService =
                new SigningJwkCacheService(
                        configurationService, new KmsConnectionService(configurationService));
    }

    public AuthJwksHandler() {
        this(ConfigurationService.getInstance());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        try {
            attachIpAddressAndUserAgentToLogs(input);
            attachTraceId();
            LOG.info("AuthJwks request received");

            List<JWK> signingKeys = new ArrayList<>();

            signingKeys.add(signingJwkCacheService.getPublicAuthSigningJwkWithOpaqueId());

            JWKSet jwkSet = new JWKSet(signingKeys);

            LOG.info("Generating AuthJwks successful response");

            return generateApiGatewayProxyResponse(
                    200, jwkSet.toString(true), Map.of("Cache-Control", "max-age=86400"), null);
        } catch (Exception e) {
            LOG.error("Error in AuthJwks lambda", e);
            return generateApiGatewayProxyResponse(500, "Error providing AuthJwks data");
        }
    }
}
