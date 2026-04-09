package com.globalfieldops.gateway.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Generates signed JWT tokens for gateway security and routing tests.
 * Uses the RSA private key from test resources.
 */
public final class JwtTestUtil {

    private static final RSAPrivateKey PRIVATE_KEY = loadPrivateKey();

    private JwtTestUtil() {}

    public static String tokenWithRoles(String subject, List<String> roles) {
        try {
            JWSSigner signer = new RSASSASigner(PRIVATE_KEY);
            Instant now = Instant.now();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .claim("roles", roles)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.RS256),
                    claims
            );
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test JWT", e);
        }
    }

    public static String adminToken() {
        return tokenWithRoles("admin-user", List.of("ADMIN"));
    }

    public static String dispatcherToken() {
        return tokenWithRoles("dispatcher-user", List.of("DISPATCHER"));
    }

    public static String technicianToken() {
        return tokenWithRoles("tech-user", List.of("TECHNICIAN"));
    }

    private static RSAPrivateKey loadPrivateKey() {
        try (InputStream is = JwtTestUtil.class.getResourceAsStream("/keys/private-key.pem")) {
            if (is == null) {
                throw new IllegalStateException("Test private key not found at /keys/private-key.pem");
            }
            String pem = new String(is.readAllBytes())
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test private key", e);
        }
    }
}
