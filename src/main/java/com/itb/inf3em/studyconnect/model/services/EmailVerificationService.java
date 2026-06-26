package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.entity.EmailVerificationToken;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.EmailVerificationTokenRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Service
public class EmailVerificationService {

    @Autowired private EmailVerificationTokenRepository tokenRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmailService emailService;

    private static final int EXPIRY_MINUTES = 15;

    @Transactional
    public void enviarCodigo(String email) {
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        Instant expiracao = Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES);

        tokenRepository.findByEmail(email).ifPresentOrElse(token -> {
            token.setCode(code);
            token.setExpiresAt(expiracao);
            token.setVerified(false);
            tokenRepository.save(token);
        }, () -> tokenRepository.save(new EmailVerificationToken(email, code, expiracao)));

        String corpo = "Seu código de verificação do StudyConnect é:\n\n"
                + code + "\n\n"
                + "Válido por " + EXPIRY_MINUTES + " minutos.\n"
                + "Se você não criou uma conta, ignore este e-mail.";

        emailService.sendSimpleEmail(email, "Código de verificação — StudyConnect", corpo);
    }

    @Transactional
    public void verificar(String email, String code) {
        EmailVerificationToken token = tokenRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido ou expirado."));

        if (token.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já verificado.");
        }
        if (Instant.now().isAfter(token.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código expirado. Solicite um novo.");
        }
        if (!token.getCode().equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido.");
        }

        token.setVerified(true);
        tokenRepository.save(token);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário não encontrado."));
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }
}
