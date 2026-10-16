package uk.gov.di.orchestration.sis.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crac.Core;
import org.crac.Resource;
import uk.gov.di.orchestration.shared.services.ConfigurationService;
import uk.gov.di.orchestration.shared.services.JwksService;
import uk.gov.di.orchestration.shared.services.KmsConnectionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static uk.gov.di.orchestration.shared.helpers.ApiGatewayResponseHelper.generateApiGatewayProxyResponse;
import static uk.gov.di.orchestration.shared.helpers.LogLineHelper.attachIpAddressAndUserAgentToLogs;
import static uk.gov.di.orchestration.shared.helpers.LogLineHelper.attachTraceId;

public class SISJwksHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>,
                Resource {

    private final JwksService jwksService;
    private static final Logger LOG = LogManager.getLogger(SISJwksHandler.class);

    public SISJwksHandler(JwksService jwksService) {
        this.jwksService = jwksService;
    }

    public SISJwksHandler(ConfigurationService configurationService) {
        this.jwksService =
                new JwksService(
                        configurationService, new KmsConnectionService(configurationService));
    }

    public SISJwksHandler() {
        this(ConfigurationService.getInstance());
        Core.getGlobalContext().register(this);
    }

    @Override
    public void beforeCheckpoint(org.crac.Context<? extends Resource> context) throws Exception {
        LOG.info("Executing before checkpoint");
        this.sisJwksHandler();
        // Empty key cache, so we can force the key to be re-fetched everytime
        // the SnapStart image is restored. This allows us to fetch the key
        // on every restore, but continue to cache it for the duration that
        // the same lambda exists.
        this.jwksService.emptyCache();
    }

    @Override
    public void afterRestore(org.crac.Context<? extends Resource> context) throws Exception {}

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        attachIpAddressAndUserAgentToLogs(input);
        attachTraceId();
        return sisJwksHandler();
    }

    private APIGatewayProxyResponseEvent sisJwksHandler() {
        try {
            List<JWK> signingKeys = new ArrayList<>();

            signingKeys.add(jwksService.getPublicSisTokenJwkWithOpaqueId());

            JWKSet jwkSet = new JWKSet(signingKeys);

            LOG.info("Generating SISJwks successful response");

            return generateApiGatewayProxyResponse(
                    200, jwkSet.toString(true), Map.of("Cache-Control", "max-age=86400"), null);
        } catch (Exception e) {
            LOG.error("Error in SISJwks lambda", e);
            return generateApiGatewayProxyResponse(500, "Error providing SISJwks data");
        }
    }
}
