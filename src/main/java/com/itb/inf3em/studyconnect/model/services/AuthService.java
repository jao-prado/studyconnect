package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.LoginRequestDTO;
import com.itb.inf3em.studyconnect.model.dto.LoginResponseDTO;
import com.itb.inf3em.studyconnect.model.dto.RegisterRequestDTO;
import com.itb.inf3em.studyconnect.model.dto.RegistrationResponseDTO;
import com.itb.inf3em.studyconnect.model.entity.MfaCode;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.EmailVerificationTokenRepository;
import com.itb.inf3em.studyconnect.model.repository.MfaCodeRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String REGISTRATION_PURPOSE = "REGISTRATION";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private CredentialValidationService credentialValidationService;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MfaService mfaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MfaCodeRepository mfaCodeRepository;

    @Autowired
    private EmailVerificationService emailVerificationService;

    public RegistrationResponseDTO registrar(RegisterRequestDTO request) {
        Usuario novo = new Usuario();
        novo.setNome(request.getNome());
        novo.setEmail(request.getEmail());
        novo.setSenha(request.getSenha());
        Usuario salvo = usuarioService.save(novo);

        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String registrationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = passwordEncoder.encode(registrationToken);
        mfaCodeRepository.save(new MfaCode(
                salvo.getId(),
                REGISTRATION_PURPOSE,
                tokenHash,
                Instant.now().plus(30, ChronoUnit.MINUTES)
        ));
        return new RegistrationResponseDTO(salvo.getId(), salvo.getEmail(), registrationToken);
    }

    @Transactional
    public LoginResponseDTO concluirCadastro(String email, String code, String registrationToken) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Código inválido ou expirado."));
        MfaCode completion = mfaCodeRepository
                .findByUsuarioIdAndPurpose(usuario.getId(), REGISTRATION_PURPOSE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Código inválido ou expirado."));

        if (completion.isUsed()
                || Instant.now().isAfter(completion.getExpiresAt())
                || registrationToken == null
                || !passwordEncoder.matches(registrationToken, completion.getCodeHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido ou expirado.");
        }

        emailVerificationService.verificar(normalizedEmail, code);
        completion.setUsed(true);
        mfaCodeRepository.save(completion);

        LoginResponseDTO response;
        if (usuario.getTipoUsuario() == TipoUsuario.ADMIN || usuario.isMfaHabilitado()) {
            response = mfaService.iniciarDesafio(usuario, true);
            response.setOnboardingRequired(true);
        } else {
            response = new LoginResponseDTO(
                    usuario.getId(),
                    usuario.getNome(),
                    usuario.getTipoUsuario().name(),
                    usuario.getFotoUrl(),
                    usuario.getEmail(),
                    usuario.isAtivo(),
                    jwtService.generateToken(usuario),
                    jwtService.getExpirationSeconds(),
                    usuario.isMfaHabilitado()
            );
            response.setOnboardingRequired(true);
        }
        return response;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        long t0 = System.currentTimeMillis();

        credentialValidationService.validateEmail(request.getEmail());
        if (request.getSenha() == null || request.getSenha().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos");
        }

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos"));

        log.info("[AUTH] query DB: {}ms", System.currentTimeMillis() - t0);

        if (usuario.getSenha() == null || !passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos");
        }

        if (!usuario.isAtivo()) {
            boolean jaVerificada = usuario.getGoogleId() != null
                    || tokenRepository.findByEmail(usuario.getEmail())
                        .map(t -> t.isVerified())
                        .orElse(false);

            if (!jaVerificada) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "E-mail nao verificado. Verifique sua caixa de entrada.");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta suspensa. Entre em contato com o suporte.");
            // Conta suspensa — retorna normalmente com ativo=false
            // o frontend redireciona para a página de suspensão
        }

        log.info("[AUTH] login total: {}ms", System.currentTimeMillis() - t0);

        // MFA obrigatório para ADMIN; opcional para demais se habilitado
        boolean mfaObrigatorio = usuario.getTipoUsuario() == TipoUsuario.ADMIN;
        boolean mfaAtivo       = usuario.isMfaHabilitado();

        if (mfaObrigatorio || mfaAtivo) {
            return mfaService.iniciarDesafio(usuario);
        }

        return new LoginResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTipoUsuario().name(),
                usuario.getFotoUrl(),
                usuario.getEmail(),
                usuario.isAtivo(),
                jwtService.generateToken(usuario),
                jwtService.getExpirationSeconds(),
                usuario.isMfaHabilitado()
        );
    }
}
