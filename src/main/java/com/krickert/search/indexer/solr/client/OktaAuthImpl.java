package com.krickert.search.indexer.solr.client;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import okhttp3.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
@Requires(notEnv = Environment.TEST)
public class OktaAuthImpl implements OktaAuth {
    private static final Logger log = LoggerFactory.getLogger(OktaAuthImpl.class);

    private final boolean enabled;
    private final String clientSecret;
    private final String clientId;
    private final String tokenEndpoint;
    private final String scope;
    private final boolean dpopRequired;
    private final Proxy proxy;

    private volatile String cachedToken;
    private volatile long tokenExpirationTime;
    private final ReentrantLock lock = new ReentrantLock();

    @Inject
    public OktaAuthImpl(IndexerConfiguration indexerConfiguration) {
        SolrConfiguration.Connection.Authentication authentication = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getAuthentication();
        if (oAuthEnabled(authentication)) {
            assert authentication.getClientSecret() != null;
            assert authentication.getClientId() != null;
            assert authentication.getIssuer() != null;
            assert authentication.getScope() != null;

            this.clientSecret = authentication.getClientSecret();
            this.clientId = authentication.getClientId();
            this.tokenEndpoint = authentication.getIssuer();
            this.scope = authentication.getScope();
            this.enabled = true;
            this.dpopRequired = authentication.isRequireDpop();
            this.proxy = authentication.creaateProxy();
            log.info("Okta authentication enabled");
        } else {
            this.enabled = false;
            this.clientSecret = null;
            this.clientId = null;
            this.tokenEndpoint = null;
            this.scope = null;
            this.dpopRequired = false;
            this.proxy = Proxy.NO_PROXY;
            log.info("connection destination did not include oauth");
        }
    }


    private static boolean oAuthEnabled(SolrConfiguration.Connection.Authentication authentication) {
        return authentication != null &&
                authentication.isEnabled() &&
                authentication.getType() != null &&
                (authentication.getType().contains("oauth") || authentication.getType().equals("jwt"));
    }

    @Override
    public String getAccessToken() throws IOException {
        if (!enabled) {
            return null;
        }
        // Use double-checked locking to minimize locking overhead
        if (cachedToken == null || System.currentTimeMillis() > tokenExpirationTime) {
            lock.lock();
            try {
                if (cachedToken == null || System.currentTimeMillis() > tokenExpirationTime) {
                    refreshAccessToken();
                }
            } finally {
                lock.unlock();
            }
        }
        return cachedToken;
    }

    private void refreshAccessToken() throws IOException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.proxy(proxy);
        OkHttpClient client = builder.build();

        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        // Step 1: Make an initial token request to get the nonce
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("scope", scope)
                .build();

        Request nonceRequest = new Request.Builder()
                .url(tokenEndpoint)
                .addHeader("Authorization", "Basic " + encodedCredentials)
                .post(formBody)
                .build();

        String nonce;
        try (Response nonceResponse = client.newCall(nonceRequest).execute()) {
            if (nonceResponse.code() == 401) {
                nonce = getNonceFromResponse(nonceResponse);
            } else if (nonceResponse.code() == 200) {
                // Successful request; no nonce needed
                String responseBody = nonceResponse.body() != null ? nonceResponse.body().string() : "";
                JSONObject jsonObject = new JSONObject(responseBody);
                updateCachedToken(jsonObject.getString("access_token"), jsonObject.getInt("expires_in"));
                return;
            } else {
                String responseBody = nonceResponse.body() != null ? nonceResponse.body().string() : "No error body";
                throw new IOException("Unexpected code " + nonceResponse + " with body: " + responseBody);
            }
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(tokenEndpoint)
                .addHeader("Authorization", "Basic " + encodedCredentials);
        if (this.dpopRequired) {
            addDpopProofToHeader(nonce, requestBuilder);
        }
        Request request = requestBuilder.post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("Unexpected code " + response + " with body: " + responseBody);
            }
            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject jsonObject = new JSONObject(responseBody);
            updateCachedToken(jsonObject.getString("access_token"), jsonObject.getInt("expires_in"));
        }
    }

    private void addDpopProofToHeader(String nonce, Request.Builder requestBuilder) {
        // Step 2: Make the actual token request with the DPoP proof
        // Generate EC Key Pair (for DPoP)
        KeyPair keyPair;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(Curve.P_256.toECParameterSpec());
            keyPair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String dpopProof;
        try {
            dpopProof = createDpopHeader(tokenEndpoint, "POST", keyPair, nonce);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        requestBuilder.addHeader("DPoP", dpopProof);
    }

    private void updateCachedToken(String token, int expiresIn) {
        this.cachedToken = token;
        this.tokenExpirationTime = System.currentTimeMillis() + (expiresIn * 1000L) - 60000; // Refresh 1 minute before expiry
    }

    private static String createDpopHeader(String htu, String htm, KeyPair keyPair, String nonce) throws Exception {
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();

        // JWK key
        JWK jwk = new ECKey.Builder(Curve.P_256, publicKey).keyID("123").build();

        // Create JWT
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("htu", htu)
                .claim("htm", htm)
                .claim("nonce", nonce)
                .issueTime(new Date())
                .jwtID(java.util.UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256)
                        .type(new JOSEObjectType("dpop+jwt"))
                        .jwk(jwk)
                        .build(),
                claimsSet);

        // Sign the JWT
        signedJWT.sign(new ECDSASigner(privateKey));

        return signedJWT.serialize();
    }

    private static String getNonceFromResponse(Response response) {
        return response.header("DPoP-Nonce");
    }
}