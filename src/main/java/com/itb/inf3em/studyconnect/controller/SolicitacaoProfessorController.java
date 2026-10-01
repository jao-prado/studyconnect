package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.dto.SolicitacaoProfessorDTO;
import com.itb.inf3em.studyconnect.model.services.SolicitacaoProfessorService;
import com.itb.inf3em.studyconnect.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/solicitacoes-professor")
public class SolicitacaoProfessorController {

    private final SolicitacaoProfessorService solicitacaoService;
    private final CurrentUser currentUser;

    public SolicitacaoProfessorController(SolicitacaoProfessorService solicitacaoService,
                                          CurrentUser currentUser) {
        this.solicitacaoService = solicitacaoService;
        this.currentUser = currentUser;
    }

    /** POST /api/v1/solicitacoes-professor
     *  Body: { tipoComprovante, comprovanteUrl }
     *  usuarioId, status, dataAnalise e motivoReprovacao do body são ignorados */
    @PostMapping
    public ResponseEntity<SolicitacaoProfessorDTO> criar(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitacaoService.criar(body, currentUser.require()));
    }

    /** GET /api/v1/solicitacoes-professor/minhas
     *  Lista as solicitações do próprio usuário autenticado */
    @GetMapping("/minhas")
    public ResponseEntity<List<SolicitacaoProfessorDTO>> minhas() {
        return ResponseEntity.ok(solicitacaoService.listarMinhas(currentUser.require()));
    }

    // ── Operações ADMIN ──────────────────────────────────────────────

    /** GET /api/v1/solicitacoes-professor?status=PENDENTE
     *  Lista todas as solicitações. Filtro por status é opcional. */
    @GetMapping
    public ResponseEntity<List<SolicitacaoProfessorDTO>> listarTodas(
            @RequestParam(required = false) String status) {
        currentUser.requireAdmin();
        return ResponseEntity.ok(solicitacaoService.listarTodas(status));
    }

    /** GET /api/v1/solicitacoes-professor/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoProfessorDTO> porId(@PathVariable Long id) {
        currentUser.requireAdmin();
        return ResponseEntity.ok(solicitacaoService.buscarPorId(id));
    }

    /** PUT /api/v1/solicitacoes-professor/{id}/aprovar */
    @PutMapping("/{id}/aprovar")
    public ResponseEntity<SolicitacaoProfessorDTO> aprovar(@PathVariable Long id) {
        currentUser.requireAdmin();
        return ResponseEntity.ok(solicitacaoService.aprovar(id));
    }

    /** PUT /api/v1/solicitacoes-professor/{id}/reprovar
     *  Body: { motivoReprovacao } */
    @PutMapping("/{id}/reprovar")
    public ResponseEntity<SolicitacaoProfessorDTO> reprovar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        currentUser.requireAdmin();
        return ResponseEntity.ok(solicitacaoService.reprovar(id, body.get("motivoReprovacao")));
    }
}
