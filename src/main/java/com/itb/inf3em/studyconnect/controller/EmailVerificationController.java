package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.dto.ResendVerificationRequestDTO;
import com.itb.inf3em.studyconnect.model.dto.VerifyEmailRequestDTO;
import com.itb.inf3em.studyconnect.model.services.EmailVerificationService;
import com.itb.inf3em.studyconnect.model.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class EmailVerificationController {

    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    private AuthService authService;

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequestDTO request) {
        if (request.getRegistrationToken() != null && !request.getRegistrationToken().isBlank()) {
            return ResponseEntity.ok(authService.concluirCadastro(
                    request.getEmail(), request.getCode(), request.getRegistrationToken()));
        }
        emailVerificationService.verificar(request.getEmail(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "E-mail verificado com sucesso."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody ResendVerificationRequestDTO request) {
        emailVerificationService.enviarCodigo(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Código reenviado."));
    }
}
