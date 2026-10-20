package com.itb.inf3em.studyconnect.model.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleIdTokenValidatorTest {

    private static final String CLIENT_ID = "web-client.apps.googleusercontent.com";

    @Test
    void tokenVerificadoPelaBibliotecaRetornaSomenteClaimsNecessarias() throws Exception {
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken token = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(verifier.verify("signed-token")).thenReturn(token);
        when(token.getPayload()).thenReturn(payload);
        when(payload.getAudience()).thenReturn(CLIENT_ID);
        when(payload.getIssuer()).thenReturn("https://accounts.google.com");
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getSubject()).thenReturn("google-subject");
        when(payload.getEmail()).thenReturn("aluno@example.com");
        when(payload.get("name")).thenReturn("Aluno");

        var claims = new GoogleIdTokenValidator(CLIENT_ID, verifier).verify("signed-token");

        assertThat(claims).containsEntry("sub", "google-subject")
                .containsEntry("email", "aluno@example.com")
                .containsEntry("email_verified", true)
                .containsEntry("aud", CLIENT_ID);
    }

    @Test
    void tokenRejeitadoPeloVerificadorOficialRetorna401() throws Exception {
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        when(verifier.verify("invalid-token")).thenReturn(null);

        assertThatThrownBy(() -> new GoogleIdTokenValidator(CLIENT_ID, verifier).verify("invalid-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void emailNaoVerificadoEhRejeitado() throws Exception {
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken token = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(verifier.verify("unverified-token")).thenReturn(token);
        when(token.getPayload()).thenReturn(payload);
        when(payload.getAudience()).thenReturn(CLIENT_ID);
        when(payload.getIssuer()).thenReturn("accounts.google.com");
        when(payload.getEmailVerified()).thenReturn(false);

        assertThatThrownBy(() -> new GoogleIdTokenValidator(CLIENT_ID, verifier).verify("unverified-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void falhaDeRedeAoBuscarChavesRetorna503() throws Exception {
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        when(verifier.verify("network-failure")).thenThrow(new IOException("offline"));

        assertThatThrownBy(() -> new GoogleIdTokenValidator(CLIENT_ID, verifier).verify("network-failure"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
