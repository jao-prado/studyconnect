package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.dto.AtualizarPerfilDTO;
import com.itb.inf3em.studyconnect.model.dto.DeleteAccountRequestDTO;
import com.itb.inf3em.studyconnect.model.dto.UsuarioDTO;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.services.UsuarioService;
import com.itb.inf3em.studyconnect.security.AuthenticatedUser;
import com.itb.inf3em.studyconnect.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    @Autowired private UsuarioService usuarioService;
    @Autowired private CurrentUser    currentUser;

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> findAll() {
        List<UsuarioDTO> dtos = usuarioService.findAll().stream()
                .map(UsuarioDTO::new)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> findById(@PathVariable Long id) {
        currentUser.requireSameUserOrAdmin(id);
        Usuario usuario = usuarioService.findById(id);
        return ResponseEntity.ok(new UsuarioDTO(usuario));
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> cadastrar(@RequestBody Usuario usuario) {
        Usuario novoUsuario = usuarioService.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UsuarioDTO(novoUsuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> atualizar(@PathVariable Long id,
                                                @RequestBody AtualizarPerfilDTO dto) {
        AuthenticatedUser authenticatedUser = currentUser.requireSameUserOrAdmin(id);
        Usuario atualizado = authenticatedUser.tipoUsuario() == TipoUsuario.ADMIN
                && !authenticatedUser.usuarioId().equals(id)
                ? usuarioService.updateStatusAsAdmin(id, true)
                : usuarioService.updateOwn(id, dto);
        return ResponseEntity.ok(new UsuarioDTO(atualizado));
    }

    /**
     * POST /api/v1/usuarios/{id}/delete-challenge
     * Valida a senha e envia o código MFA de exclusão por e-mail.
     * Só deve ser chamado quando mfaHabilitado=true.
     */
    @PostMapping("/{id}/delete-challenge")
    public ResponseEntity<Map<String, String>> requestDeleteChallenge(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        currentUser.requireSameUserOrAdmin(id);
        String senha = body.get("senha");
        if (senha == null || senha.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Senha obrigatória."));
        }
        usuarioService.requestDeleteChallenge(id, senha);
        return ResponseEntity.ok(Map.of("message", "Código enviado para o seu e-mail."));
    }

    /**
     * DELETE /api/v1/usuarios/{id}
     * Exclui a conta após validar senha (+ código MFA se habilitado).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletar(@PathVariable Long id,
                                          @RequestBody DeleteAccountRequestDTO dto) {
        currentUser.requireSameUserOrAdmin(id);
        usuarioService.deleteWithAuth(id, dto);
        return ResponseEntity.ok("Usuário excluído com sucesso!");
    }
}
