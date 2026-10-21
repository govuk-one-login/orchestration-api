package uk.gov.di.authentication.oidc.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.nimbusds.jose.KeySourceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static uk.gov.di.orchestration.shared.helpers.LogLineHelper.LogFieldName.AWS_REQUEST_ID;
import static uk.gov.di.orchestration.shared.helpers.LogLineHelper.attachLogFieldToLogs;
import static uk.gov.di.orchestration.shared.helpers.LogLineHelper.attachTraceId;
import static uk.gov.di.orchestration.shared.utils.JwksUtils.retrieveJwkFromURLWithKeyId;

public class FetchJwksHandler implements RequestHandler<Map<String, String>, String> {
    private static final Logger LOG = LogManager.getLogger(FetchJwksHandler.class);

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        attachTraceId();
        attachLogFieldToLogs(AWS_REQUEST_ID, context.getAwsRequestId());
        String url = event.get("url");
        String keyId = event.get("keyId");
        final String errorResponse = "error";
        try {
            if (url == null || keyId == null) {
                throw new IllegalArgumentException(
                        "FetchJwksHandler invoked with invalid argument(s)");
            }
            return retrieveJwkFromURLWithKeyId(new URL(url), keyId).toJSONString();
        } catch (KeySourceException e) {
            String errorMsg =
                    "Failed to fetch JWKS: could not find key in JWKS that matches provided keyId";
            LOG.error(errorMsg, e);
            return errorResponse;
        } catch (MalformedURLException e) {
            String errorMsg = "Failed to fetch JWKS: URL is malformed";
            LOG.error(errorMsg, e);
            return errorResponse;
        } catch (IllegalArgumentException e) {
            String errorMsg = "Failed to fetch JWKS: url and/or keyId parameter not present";
            LOG.error(errorMsg, e);
            return errorResponse;
        }
    }
}
