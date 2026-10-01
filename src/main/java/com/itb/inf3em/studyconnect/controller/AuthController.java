package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.dto.ForgotPasswordRequestDTO;
import com.itb.inf3em.studyconnect.model.dto.LoginRequestDTO;
import com.itb.inf3em.studyconnect.model.dto.LoginResponseDTO;
import com.itb.inf3em.studyconnect.model.dto.MfaToggleRequestDTO;
import com.itb.inf3em.studyconnect.model.dto.MfaVerifyRequestDTO;
import com.itb.inf3em.studyconnect.model.dto.ResetPasswordRequestDTO;
import com.itb.inf3em.studyconnect.model.services.AuthService;
import com.itb.inf3em.studyconnect.model.services.MfaService;
import com.itb.inf3em.studyconnect.model.services.PasswordResetService;
import com.itb.inf3em.studyconnect.security.AuthenticatedUser;
import com.itb.inf3em.studyconnect.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private MfaService mfaService;
    @Autowired private CurrentUser currentUser;

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.solicitarRecuperacao(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Se o e-mail estiver cadastrado, você receberá as instruções em breve."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.redefinirSenha(request.getToken(), request.getNovaSenha());
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
    }

    /** Público — valida o código MFA e emite o JWT real. */
    @PostMapping("/mfa/verify")
    public LoginResponseDTO mfaVerify(@RequestBody MfaVerifyRequestDTO request) {
        return mfaService.validarCodigo(request.getEmail(), request.getCode());
    }

    /** Autenticado — habilita MFA após confirmação de senha. */
    @PostMapping("/mfa/enable")
    public ResponseEntity<Map<String, String>> mfaEnable(@RequestBody MfaToggleRequestDTO request) {
        AuthenticatedUser user = currentUser.require();
        mfaService.habilitarMfa(user.usuarioId(), request.getSenha());
        return ResponseEntity.ok(Map.of("message", "MFA habilitado com sucesso."));
    }

    /** Autenticado — desabilita MFA após confirmação de senha. */
    @PostMapping("/mfa/disable")
    public ResponseEntity<Map<String, String>> mfaDisable(@RequestBody MfaToggleRequestDTO request) {
        AuthenticatedUser user = currentUser.require();
        mfaService.desabilitarMfa(user.usuarioId(), request.getSenha());
        return ResponseEntity.ok(Map.of("message", "MFA desabilitado com sucesso."));
    }
}
