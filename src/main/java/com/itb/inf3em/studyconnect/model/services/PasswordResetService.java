package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.entity.PasswordResetToken;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.PasswordResetTokenRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private EmailService emailService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public void solicitarRecuperacao(String email) {
        // Sempre retorna 200 para não revelar se o e-mail existe
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            tokenRepository.deleteByEmail(email);

            String token = UUID.randomUUID().toString();
            Instant expiracao = Instant.now().plus(30, ChronoUnit.MINUTES);
            tokenRepository.save(new PasswordResetToken(token, email, expiracao));

            String link = frontendUrl + "/redefinir-senha?token=" + token;
            String corpo = "Olá, " + usuario.getNome() + "!\n\n"
                    + "Recebemos uma solicitação para redefinir a senha da sua conta StudyConnect.\n\n"
                    + "Clique no link abaixo para criar uma nova senha (válido por 30 minutos):\n"
                    + link + "\n\n"
                    + "Se você não fez essa solicitação, ignore este e-mail.";

            emailService.sendSimpleEmail(email, "Redefinição de senha — StudyConnect", corpo);
        });
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        if (novaSenha == null || novaSenha.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter no mínimo 6 caracteres.");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado."));

        if (resetToken.isUsed() || Instant.now().isAfter(resetToken.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado.");
        }

        Usuario usuario = usuarioRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado."));

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
