package com.itb.inf3em.studyconnect.model.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GoogleIdTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenValidator.class);
    private static final List<String> VALID_ISSUERS =
            List.of("accounts.google.com", "https://accounts.google.com");

    private final String expectedClientId;
    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleIdTokenValidator(@Value("${app.google.client-id:}") String expectedClientId) {
        this(expectedClientId, buildVerifier(expectedClientId));
    }

    GoogleIdTokenValidator(String expectedClientId, GoogleIdTokenVerifier verifier) {
        this.expectedClientId = expectedClientId;
        this.verifier = verifier;
    }

    public Map<String, Object> verify(String idToken) {
        if (expectedClientId == null || expectedClientId.isBlank()) {
            log.error("[GoogleAuth] APP_GOOGLE_CLIENT_ID nao configurado.");
            throw unauthorized();
        }
        if (idToken == null || idToken.isBlank()) {
            throw unauthorized();
        }

        try {
            GoogleIdToken verifiedToken = verifier.verify(idToken);
            if (verifiedToken == null) {
                throw unauthorized();
            }

            GoogleIdToken.Payload payload = verifiedToken.getPayload();
            if (!expectedClientId.equals(payload.getAudience())
                    || !VALID_ISSUERS.contains(payload.getIssuer())
                    || !Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw unauthorized();
            }

            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", payload.getSubject());
            claims.put("email", payload.getEmail());
            claims.put("email_verified", payload.getEmailVerified());
            claims.put("aud", payload.getAudience());
            claims.put("iss", payload.getIssuer());
            claims.put("iat", payload.getIssuedAtTimeSeconds());
            copyOptionalClaim(payload, claims, "name");
            copyOptionalClaim(payload, claims, "picture");
            return claims;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("[GoogleAuth] Falha de rede ao obter chaves publicas do Google.");
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Nao foi possivel verificar o token Google no momento.");
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            log.warn("[GoogleAuth] ID token invalido.");
            throw unauthorized();
        }
    }

    private static GoogleIdTokenVerifier buildVerifier(String expectedClientId) {
        if (expectedClientId == null || expectedClientId.isBlank()) {
            return null;
        }
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(expectedClientId))
                .setIssuers(VALID_ISSUERS)
                .build();
    }

    private static void copyOptionalClaim(GoogleIdToken.Payload payload,
                                          Map<String, Object> claims,
                                          String name) {
        Object value = payload.get(name);
        if (value != null) {
            claims.put(name, value);
        }
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google invalido.");
    }
}
