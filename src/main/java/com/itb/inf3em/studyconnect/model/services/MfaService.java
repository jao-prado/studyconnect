package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.LoginResponseDTO;
import com.itb.inf3em.studyconnect.model.entity.MfaCode;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
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

@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);

    private static final int    EXPIRY_MINUTES = 5;
    private static final int    MAX_TENTATIVAS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public  static final String PURPOSE_LOGIN  = "LOGIN";
    public  static final String PURPOSE_DELETE = "DELETE_ACCOUNT";

    @Autowired private MfaCodeRepository     mfaCodeRepository;
    @Autowired private UsuarioRepository     usuarioRepository;
    @Autowired private EmailService          emailService;
    @Autowired private JwtService            jwtService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    /**
     * Gera um desafio MFA de LOGIN, invalida o anterior e envia o código por e-mail.
     * Retorna LoginResponseDTO com mfaPendente=true e sem accessToken.
     */
    @Transactional
    public LoginResponseDTO iniciarDesafio(Usuario usuario) {
        gerarEPersistirDesafio(usuario, PURPOSE_LOGIN);
        return new LoginResponseDTO(usuario.getEmail(), true);
    }

    /**
     * Gera um desafio MFA exclusivo para confirmação de exclusão de conta.
     * Não interfere com desafios de login em andamento.
     */
    @Transactional
    public void iniciarDesafioExclusao(Usuario usuario) {
        gerarEPersistirDesafio(usuario, PURPOSE_DELETE);
    }

    /**
     * Valida o código de LOGIN. Se válido, emite o JWT real.
     */
    @Transactional
    public LoginResponseDTO validarCodigo(String email, String codePuro) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Código inválido ou expirado."));

        validarDesafio(usuario.getId(), PURPOSE_LOGIN, codePuro);

        log.info("[MFA] Autenticação MFA concluída para usuarioId={}", usuario.getId());

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

    /**
     * Valida o código de exclusão de conta (purpose=DELETE_ACCOUNT).
     * Não emite JWT — apenas confirma o desafio.
     */
    @Transactional
    public void validarCodigoExclusao(Long usuarioId, String codePuro) {
        validarDesafio(usuarioId, PURPOSE_DELETE, codePuro);
        log.info("[MFA] Desafio de exclusão validado para usuarioId={}", usuarioId);
    }

    /**
     * Habilita MFA para o usuário após confirmar a senha atual.
     */
    @Transactional
    public void habilitarMfa(Long usuarioId, String senhaConfirmacao) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        if (usuario.getSenha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usuários autenticados via Google não podem configurar MFA por senha.");
        }

        if (!passwordEncoder.matches(senhaConfirmacao, usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta.");
        }

        usuario.setMfaHabilitado(true);
        usuarioRepository.save(usuario);
        log.info("[MFA] MFA habilitado para usuarioId={}", usuarioId);
    }

    /**
     * Desabilita MFA para o usuário após confirmar a senha atual.
     */
    @Transactional
    public void desabilitarMfa(Long usuarioId, String senhaConfirmacao) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        if (usuario.getSenha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usuários autenticados via Google não podem configurar MFA por senha.");
        }

        if (!passwordEncoder.matches(senhaConfirmacao, usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta.");
        }

        usuario.setMfaHabilitado(false);
        usuarioRepository.save(usuario);
        log.info("[MFA] MFA desabilitado para usuarioId={}", usuarioId);
    }

    // ── helpers privados ──────────────────────────────────────────────────────

    /** Gera, persiste (substituindo anterior do mesmo purpose) e envia o código. */
    private void gerarEPersistirDesafio(Usuario usuario, String purpose) {
        String codePuro = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(codePuro);
        Instant expiracao = Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES);

        mfaCodeRepository.findByUsuarioIdAndPurpose(usuario.getId(), purpose).ifPresentOrElse(existing -> {
            existing.setCodeHash(codeHash);
            existing.setExpiresAt(expiracao);
            existing.setUsed(false);
            existing.setTentativas(0);
            mfaCodeRepository.save(existing);
        }, () -> mfaCodeRepository.save(new MfaCode(usuario.getId(), purpose, codeHash, expiracao)));

        enviarEmail(usuario.getEmail(), codePuro);
        // codePuro sai de escopo aqui — não é retornado nem logado
    }

    /** Valida um desafio pelo purpose; marca como usado se correto. */
    private void validarDesafio(Long usuarioId, String purpose, String codePuro) {
        MfaCode desafio = mfaCodeRepository.findByUsuarioIdAndPurpose(usuarioId, purpose)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Código inválido ou expirado."));

        if (desafio.isUsed() || Instant.now().isAfter(desafio.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido ou expirado.");
        }

        if (!passwordEncoder.matches(codePuro, desafio.getCodeHash())) {
            int novasTentativas = desafio.getTentativas() + 1;
            desafio.setTentativas(novasTentativas);
            if (novasTentativas >= MAX_TENTATIVAS) {
                desafio.setUsed(true);
                log.warn("[MFA] Limite de tentativas atingido para usuarioId={} purpose={}", usuarioId, purpose);
            }
            mfaCodeRepository.save(desafio);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido ou expirado.");
        }

        desafio.setUsed(true);
        mfaCodeRepository.save(desafio);
    }

    private void enviarEmail(String email, String codePuro) {
        String corpo = "Seu código de autenticação é: " + codePuro + "\n\n"
                + "Este código é válido por " + EXPIRY_MINUTES + " minutos e pode ser usado apenas uma vez.\n\n"
                + "Não compartilhe este código.";
        try {
            emailService.sendSimpleEmail(email, "Código de autenticação MFA - StudyConnect", corpo);
        } catch (Exception ex) {
            log.error("[MFA] Falha ao enviar e-mail MFA para {}: {}", email, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível enviar o código MFA. Tente novamente.");
        }
    }
}
