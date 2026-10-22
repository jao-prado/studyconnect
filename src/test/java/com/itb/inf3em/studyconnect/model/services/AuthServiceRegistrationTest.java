package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.LoginResponseDTO;
import com.itb.inf3em.studyconnect.model.dto.RegisterRequestDTO;
import com.itb.inf3em.studyconnect.model.entity.MfaCode;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.EmailVerificationTokenRepository;
import com.itb.inf3em.studyconnect.model.repository.MfaCodeRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceRegistrationTest {

    private static final String EMAIL = "student@example.com";
    private static final String REGISTRATION_TOKEN = "opaque-one-time-registration-token";
    private AuthService service;
    private UsuarioRepository usuarioRepository;
    private MfaCodeRepository mfaCodeRepository;
    private EmailVerificationService emailVerificationService;
    private MfaService mfaService;
    private BCryptPasswordEncoder passwordEncoder;
    private Usuario usuario;
    private MfaCode completion;

    @BeforeEach
    void setUp() {
        service = new AuthService();
        usuarioRepository = mock(UsuarioRepository.class);
        mfaCodeRepository = mock(MfaCodeRepository.class);
        emailVerificationService = mock(EmailVerificationService.class);
        mfaService = mock(MfaService.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        usuario = new Usuario();
        usuario.setId(71L);
        usuario.setNome("Aluno");
        usuario.setEmail(EMAIL);
        usuario.setTipoUsuario(TipoUsuario.ALUNO);
        usuario.setSenha(passwordEncoder.encode("password"));
        usuario.setAtivo(false);
        completion = new MfaCode(
                usuario.getId(),
                "REGISTRATION",
                passwordEncoder.encode(REGISTRATION_TOKEN),
                Instant.now().plusSeconds(600)
        );

        ReflectionTestUtils.setField(service, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(service, "mfaCodeRepository", mfaCodeRepository);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "emailVerificationService", emailVerificationService);
        ReflectionTestUtils.setField(service, "mfaService", mfaService);
        ReflectionTestUtils.setField(service, "jwtService", mock(JwtService.class));
        ReflectionTestUtils.setField(service, "usuarioService", mock(UsuarioService.class));
        ReflectionTestUtils.setField(service, "tokenRepository", mock(EmailVerificationTokenRepository.class));

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(mfaCodeRepository.findByUsuarioIdAndPurpose(71L, "REGISTRATION"))
                .thenReturn(Optional.of(completion));
        doAnswer(invocation -> {
            usuario.setAtivo(true);
            return null;
        }).when(emailVerificationService).verificar(EMAIL, "123456");
        when(((JwtService) ReflectionTestUtils.getField(service, "jwtService")).generateToken(usuario))
                .thenReturn("valid-access-token");
        when(((JwtService) ReflectionTestUtils.getField(service, "jwtService")).getExpirationSeconds())
                .thenReturn(3600L);
    }

    @Test
    void codigoDeEmailSemTokenDoCadastroNaoEmiteSessao() {
        assertThatThrownBy(() -> service.concluirCadastro(EMAIL, "123456", "wrong-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(emailVerificationService, never()).verificar(any(), any());
    }

    @Test
    void tokenDoCadastroEDeUsoUnicoEConcluiAutenticacaoDepoisDoEmail() {
        LoginResponseDTO response = service.concluirCadastro(EMAIL, "123456", REGISTRATION_TOKEN);

        assertThat(response.getAccessToken()).isEqualTo("valid-access-token");
        assertThat(response.isOnboardingRequired()).isTrue();
        assertThat(completion.isUsed()).isTrue();
        verify(emailVerificationService).verificar(EMAIL, "123456");
        verify(mfaCodeRepository).save(completion);

        assertThatThrownBy(() -> service.concluirCadastro(EMAIL, "123456", REGISTRATION_TOKEN))
                .isInstanceOf(ResponseStatusException.class);
        verify(emailVerificationService, times(1)).verificar(EMAIL, "123456");
    }

    @Test
    void cadastroComMfaNaoEmiteJwtAntesDaValidacaoMfa() {
        usuario.setMfaHabilitado(true);
        LoginResponseDTO pending = new LoginResponseDTO(EMAIL, true);
        when(mfaService.iniciarDesafio(usuario, true)).thenReturn(pending);

        LoginResponseDTO response = service.concluirCadastro(EMAIL, "123456", REGISTRATION_TOKEN);

        assertThat(response.isMfaPendente()).isTrue();
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.isOnboardingRequired()).isTrue();
        verify(mfaService).iniciarDesafio(usuario, true);
    }

    @Test
    void registrarEmiteTokenOpacoEArmazenaSomenteHash() {
        UsuarioService usuarioService = (UsuarioService) ReflectionTestUtils.getField(service, "usuarioService");
        when(usuarioService.save(any(Usuario.class))).thenReturn(usuario);
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setNome("Aluno");
        request.setEmail(EMAIL);
        request.setSenha("password");

        var response = service.registrar(request);

        assertThat(response.id()).isEqualTo(71L);
        assertThat(response.registrationToken()).matches("[A-Za-z0-9_-]{43}");
        assertThat(response.registrationToken()).isNotEqualTo("password");
        verify(mfaCodeRepository).save(argThat(saved -> saved.getUsuarioId().equals(71L)
                && saved.getPurpose().equals("REGISTRATION")
                && passwordEncoder.matches(response.registrationToken(), saved.getCodeHash())
                && !saved.isUsed()));
    }
}
