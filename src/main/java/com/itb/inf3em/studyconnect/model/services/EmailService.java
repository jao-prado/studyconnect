package com.itb.inf3em.studyconnect.model.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final String TEST_SUBJECT = "Teste de envio de e-mail";
    private static final String TEST_BODY = "O envio de e-mail do StudyConnect funcionou.";

    private final JavaMailSender javaMailSender;
    private final String from;

    public EmailService(JavaMailSender javaMailSender,
                        @Value("${app.mail.from:}") String from) {
        this.javaMailSender = javaMailSender;
        this.from = from;
    }

    public void sendTestEmail(String to) {
        sendSimpleEmail(to, TEST_SUBJECT, TEST_BODY);
    }

    public void sendSimpleEmail(String to, String subject, String body) {
        validateRecipient(to);

        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to.trim());
        message.setSubject(subject);
        message.setText(body);

        try {
            javaMailSender.send(message);
        } catch (MailException ex) {
            Throwable cause = ex;
            while (cause.getCause() != null) cause = cause.getCause();
            log.error("[EmailService] Falha SMTP — tipo: {} — causa: {}", cause.getClass().getName(), cause.getMessage());
            log.error("[EmailService] Stack completa:", ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Nao foi possivel enviar o e-mail. Verifique a configuracao SMTP."
            );
        }
    }

    private void validateRecipient(String to) {
        if (to == null || to.isBlank() || !EMAIL_PATTERN.matcher(to.trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail de destino invalido.");
        }
    }
}
