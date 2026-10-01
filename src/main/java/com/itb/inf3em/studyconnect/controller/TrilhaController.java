package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.dto.TrilhaDTO;
import com.itb.inf3em.studyconnect.model.entity.Trilha;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.services.TrilhaService;
import com.itb.inf3em.studyconnect.security.AuthenticatedUser;
import com.itb.inf3em.studyconnect.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/trilhas")
public class TrilhaController {

    private final TrilhaService trilhaService;
    private final CurrentUser   currentUser;

    public TrilhaController(TrilhaService trilhaService, CurrentUser currentUser) {
        this.trilhaService = trilhaService;
        this.currentUser   = currentUser;
    }

    /** GET /api/v1/trilhas  |  GET /api/v1/trilhas?professorId=X */
    @GetMapping
    public ResponseEntity<List<TrilhaDTO>> findAll(
            @RequestParam(required = false) Long professorId) {
        List<Trilha> trilhas = (professorId != null)
                ? trilhaService.getTrilhasByProfessor(professorId)
                : trilhaService.getAllTrilhas();
        return ResponseEntity.ok(trilhas.stream().map(TrilhaDTO::new).toList());
    }

    /** GET /api/v1/trilhas/{id} — verifica autorização para trilhas privadas */
    @GetMapping("/{id}")
    public ResponseEntity<TrilhaDTO> findById(@PathVariable Long id) {
        Trilha trilha = trilhaService.getTrilhaById(id);
        // Expõe o código apenas para professor responsável ou admin
        boolean incluirCodigo = podeVerCodigo(trilha);
        return ResponseEntity.ok(new TrilhaDTO(trilha, incluirCodigo));
    }

    /** POST /api/v1/trilhas */
    @PostMapping
    public ResponseEntity<TrilhaDTO> create(@RequestBody Trilha trilha) {
        Trilha nova = trilhaService.createTrilha(trilha);
        // Retorna o código para o professor que acabou de criar
        return ResponseEntity.status(HttpStatus.CREATED).body(new TrilhaDTO(nova, true));
    }

    /** PUT /api/v1/trilhas/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<TrilhaDTO> update(@PathVariable Long id,
                                            @RequestBody Trilha trilha) {
        Trilha atualizada = trilhaService.updateTrilha(id, trilha);
        return ResponseEntity.ok(new TrilhaDTO(atualizada, podeVerCodigo(atualizada)));
    }

    /** DELETE /api/v1/trilhas/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        trilhaService.deleteTrilha(id);
        return ResponseEntity.ok("Trilha deletada com sucesso.");
    }

    /**
     * GET /api/v1/trilhas/{id}/codigo
     * Retorna o código de acesso. Apenas professor responsável ou admin.
     */
    @GetMapping("/{id}/codigo")
    public ResponseEntity<Map<String, String>> getCodigo(@PathVariable Long id) {
        String codigo = trilhaService.getCodigo(id);
        return ResponseEntity.ok(Map.of("codigoAcesso", codigo));
    }

    /**
     * POST /api/v1/trilhas/{id}/codigo/regenerar
     * Regenera o código de acesso. Apenas professor responsável ou admin.
     */
    @PostMapping("/{id}/codigo/regenerar")
    public ResponseEntity<Map<String, String>> regenerarCodigo(@PathVariable Long id) {
        String novoCodigo = trilhaService.regenerarCodigo(id);
        return ResponseEntity.ok(Map.of("codigoAcesso", novoCodigo));
    }

    /**
     * POST /api/v1/trilhas/{id}/acesso
     * Aluno informa o código e recebe acesso à trilha privada.
     * Body: { "codigo": "XXXXXXXXXXXX" }
     */
    @PostMapping("/{id}/acesso")
    public ResponseEntity<Map<String, String>> concederAcesso(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String codigo = body.get("codigo");
        trilhaService.concederAcesso(id, codigo);
        return ResponseEntity.ok(Map.of("message", "Acesso concedido. Você já pode acessar esta trilha."));
    }

    // ── privado ───────────────────────────────────────────────────────────────

    private boolean podeVerCodigo(Trilha trilha) {
        try {
            AuthenticatedUser user = currentUser.require();
            return user.tipoUsuario() == TipoUsuario.ADMIN
                    || trilha.getProfessorId().equals(user.usuarioId());
        } catch (Exception e) {
            return false;
        }
    }
}
