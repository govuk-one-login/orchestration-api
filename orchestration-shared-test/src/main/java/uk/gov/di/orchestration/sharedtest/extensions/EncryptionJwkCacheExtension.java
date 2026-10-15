package uk.gov.di.orchestration.sharedtest.extensions;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import uk.gov.di.orchestration.shared.entity.JwksCacheItem;
import uk.gov.di.orchestration.shared.services.EncryptionJwkCacheService;
import uk.gov.di.orchestration.sharedtest.basetest.DynamoTestConfiguration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class EncryptionJwkCacheExtension extends DynamoExtension implements AfterEachCallback {
    public static final String TABLE_NAME = "local-Jwks-Cache";
    public static final String JWKS_URL_FIELD = "JwksUrl";
    public static final String KEY_ID_FIELD = "KeyId";
    public static final int testTimeout = 123;
    private final EncryptionJwkCacheService encryptionJwkCacheService;

    public EncryptionJwkCacheExtension() {
        createInstance();
        var configurationService =
                new DynamoTestConfiguration(REGION, ENVIRONMENT, DYNAMO_ENDPOINT) {
                    @Override
                    public int getJwkCacheExpirationInSeconds() {
                        return testTimeout;
                    }

                    @Override
                    public URL getIPVJwksUrl() {
                        try {
                            return new URL("http://localhost/.well-known/jwks.json");
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public URL getDocAppJwksUrl() {
                        try {
                            return new URL("http://localhost/.well-known/jwks.json");
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
        encryptionJwkCacheService = new EncryptionJwkCacheService(configurationService);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        clearDynamoTable(dynamoDB, TABLE_NAME, JWKS_URL_FIELD, Optional.of(KEY_ID_FIELD));
    }

    @Override
    protected void createTables() {
        createTableWithPartitionAndSortKey(TABLE_NAME, JWKS_URL_FIELD, KEY_ID_FIELD);
    }

    public JwksCacheItem getOrGenerateJwksCacheItem() {
        return encryptionJwkCacheService.getOrGenerateIpvJwksCacheItem();
    }

    public void putJwksCacheItem(JwksCacheItem jwksCacheItem) {
        encryptionJwkCacheService.put(jwksCacheItem);
    }
}
