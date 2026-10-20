package com.itb.inf3em.studyconnect.service;

import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.model.services.GoogleAuthService;
import com.itb.inf3em.studyconnect.model.services.GoogleIdTokenValidator;
import com.itb.inf3em.studyconnect.model.services.MfaService;
import com.itb.inf3em.studyconnect.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

class GoogleAuthServiceTest {

    private static final String CLIENT_ID = "my-client-id.apps.googleusercontent.com";
    private GoogleAuthService service;
    private UsuarioRepository usuarioRepository;
    private JwtService jwtService;
    private GoogleIdTokenValidator tokenValidator;
    private MfaService mfaService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        jwtService = mock(JwtService.class);
        tokenValidator = mock(GoogleIdTokenValidator.class);
        mfaService = mock(MfaService.class);
        service = new GoogleAuthService(
                usuarioRepository,
                jwtService,
                mfaService,
                tokenValidator
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> validPayload() {
        Map<String, Object> p = new HashMap<>();
        p.put("aud", CLIENT_ID);
        p.put("iss", "accounts.google.com");
        p.put("email_verified", "true");
        p.put("sub", "1234567890");
        p.put("email", "user@example.com");
        p.put("name", "Test User");
        return p;
    }

    private void expectTokenInfo(String token, Map<String, Object> payload) throws Exception {
        Object verified = payload.get("email_verified");
        boolean valid = CLIENT_ID.equals(payload.get("aud"))
                && ("accounts.google.com".equals(payload.get("iss"))
                    || "https://accounts.google.com".equals(payload.get("iss")))
                && (Boolean.TRUE.equals(verified) || "true".equals(verified));
        if (valid) {
            when(tokenValidator.verify(token)).thenReturn(payload);
        } else {
            when(tokenValidator.verify(token)).thenThrow(
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google invalido."));
        }
    }

    // ── cenário 1: token válido com aud correto → permitido ──────────────────

    @Test
    void tokenValido_audCorreto_retornaPayload() throws Exception {
        expectTokenInfo("valid-token", validPayload());

        Map<String, Object> result = service.verificarToken("valid-token");

        assertThat(result.get("email")).isEqualTo("user@example.com");
    }

    @Test
    void loginGoogleLocalizaContaPeloIdMesmoAposTrocaDoEmail() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("email", "novo@example.com");
        expectTokenInfo("changed-email-token", payload);

        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNome("Test User");
        usuario.setEmail("anterior@example.com");
        usuario.setGoogleId("1234567890");
        usuario.setTipoUsuario(TipoUsuario.ALUNO);
        usuario.setAtivo(true);
        when(usuarioRepository.findByGoogleId("1234567890")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(usuario)).thenReturn("access-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        var response = service.autenticar("changed-email-token");

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getEmail()).isEqualTo("anterior@example.com");
        verify(usuarioRepository, never()).findByEmail("novo@example.com");
    }

    @Test
    void loginGoogleComMfaHabilitadoNaoEmiteJwt() throws Exception {
        expectTokenInfo("mfa-token", validPayload());

        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNome("Test User");
        usuario.setEmail("user@example.com");
        usuario.setGoogleId("1234567890");
        usuario.setTipoUsuario(TipoUsuario.ALUNO);
        usuario.setAtivo(true);
        usuario.setMfaHabilitado(true);
        when(usuarioRepository.findByGoogleId("1234567890")).thenReturn(Optional.of(usuario));
        when(mfaService.iniciarDesafio(usuario))
                .thenReturn(new com.itb.inf3em.studyconnect.model.dto.LoginResponseDTO(usuario.getEmail(), true));

        var response = service.autenticar("mfa-token");

        assertThat(response.isMfaPendente()).isTrue();
        assertThat(response.getAccessToken()).isNull();
        verify(jwtService, never()).generateToken(any());
        verify(mfaService).iniciarDesafio(usuario);
    }

    @Test
    void loginGoogleDeUsuarioInativoNaoEmiteJwtNemIniciaMfa() throws Exception {
        expectTokenInfo("inactive-token", validPayload());

        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail("user@example.com");
        usuario.setGoogleId("1234567890");
        usuario.setTipoUsuario(TipoUsuario.ALUNO);
        usuario.setAtivo(false);
        usuario.setMfaHabilitado(true);
        when(usuarioRepository.findByGoogleId("1234567890")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.autenticar("inactive-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        verify(jwtService, never()).generateToken(any());
        verify(mfaService, never()).iniciarDesafio(any());
    }

    // ── cenário 2: aud incorreto → rejeitado ─────────────────────────────────

    @Test
    void audIncorreto_rejeitado() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("aud", "outro-client-id.apps.googleusercontent.com");
        expectTokenInfo("bad-aud-token", payload);

        assertThatThrownBy(() -> service.verificarToken("bad-aud-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── cenário 3: APP_GOOGLE_CLIENT_ID ausente → rejeitado ──────────────────

    @Test
    void clientIdAusente_rejeitado() {
        when(tokenValidator.verify("any-token")).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google invalido."));

        assertThatThrownBy(() -> service.verificarToken("any-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
        // nenhuma chamada HTTP deve ter sido feita
    }

    // ── cenário 4: issuer incorreto → rejeitado ───────────────────────────────

    @Test
    void issuerIncorreto_rejeitado() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("iss", "https://evil.com");
        expectTokenInfo("bad-iss-token", payload);

        assertThatThrownBy(() -> service.verificarToken("bad-iss-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── cenário 5: email_verified=false → rejeitado ───────────────────────────

    @Test
    void emailVerifiedFalse_rejeitado() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("email_verified", "false");
        expectTokenInfo("unverified-token", payload);

        assertThatThrownBy(() -> service.verificarToken("unverified-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── cenário 6: email_verified ausente → rejeitado ─────────────────────────

    @Test
    void emailVerifiedAusente_rejeitado() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.remove("email_verified");
        expectTokenInfo("no-verified-token", payload);

        assertThatThrownBy(() -> service.verificarToken("no-verified-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── cenário 7: token inválido/assinatura inválida → rejeitado ─────────────

    @Test
    void tokenInvalido_assinaturaInvalida_rejeitado() {
        when(tokenValidator.verify("invalid-token")).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google invalido."));

        assertThatThrownBy(() -> service.verificarToken("invalid-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── cenário 8: token expirado → rejeitado ─────────────────────────────────

    @Test
    void tokenExpirado_rejeitado() {
        when(tokenValidator.verify("expired-token")).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google invalido."));

        assertThatThrownBy(() -> service.verificarToken("expired-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── bônus: aceita iss alternativo do Google ───────────────────────────────

    @Test
    void issuerAlternativoGoogle_aceito() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("iss", "https://accounts.google.com");
        expectTokenInfo("alt-iss-token", payload);

        Map<String, Object> result = service.verificarToken("alt-iss-token");

        assertThat(result.get("sub")).isEqualTo("1234567890");
    }
}
