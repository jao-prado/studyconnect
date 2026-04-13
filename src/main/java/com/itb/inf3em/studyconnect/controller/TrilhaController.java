package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.dto.TrilhaDTO;
import com.itb.inf3em.studyconnect.model.entity.Trilha;
import com.itb.inf3em.studyconnect.model.services.TrilhaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trilhas")
public class TrilhaController {

    @Autowired
    private TrilhaService trilhaService;

    /**
     * GET /api/v1/trilhas
     * Get all trilhas (public view).
     */
    @GetMapping
    public ResponseEntity<List<TrilhaDTO>> getAllTrilhas() {
        List<Trilha> trilhas = trilhaService.getAllTrilhas();
        List<TrilhaDTO> dtos = trilhas.stream()
                .map(TrilhaDTO::new)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/v1/trilhas/minhas
     * Get all trilhas for the current user (created by professor).
     * Query param: professorId (required)
     */
    @GetMapping("/minhas")
    public ResponseEntity<List<TrilhaDTO>> getMyTrilhas(@RequestParam Long professorId) {
        List<Trilha> trilhas = trilhaService.getTrilhasByProfessor(professorId);
        List<TrilhaDTO> dtos = trilhas.stream()
                .map(TrilhaDTO::new)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/v1/trilhas/professor/{professorId}
     * Get all trilhas created by a specific professor.
     */
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<List<TrilhaDTO>> getTrilhasByProfessor(@PathVariable Long professorId) {
        List<Trilha> trilhas = trilhaService.getTrilhasByProfessor(professorId);
        List<TrilhaDTO> dtos = trilhas.stream()
                .map(TrilhaDTO::new)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * POST /api/v1/trilhas
     * Create a new trilha.
     * Required fields: nome, tipo, nivel, professorId, professorNome
     */
    @PostMapping
    public ResponseEntity<TrilhaDTO> createTrilha(@RequestBody Trilha trilha) {
        Trilha novaTrilha = trilhaService.createTrilha(trilha);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TrilhaDTO(novaTrilha));
    }

    /**
     * GET /api/v1/trilhas/{id}
     * Get a specific trilha by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TrilhaDTO> getTrilhaById(@PathVariable Long id) {
        Trilha trilha = trilhaService.getTrilhaById(id);
        return ResponseEntity.ok(new TrilhaDTO(trilha));
    }

    /**
     * PUT /api/v1/trilhas/{id}
     * Update a trilha (professor only).
     * Requires professorId header or query param for verification.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TrilhaDTO> updateTrilha(
            @PathVariable Long id,
            @RequestBody Trilha trilhaUpdate,
            @RequestParam Long professorId) {

        Trilha atualizada = trilhaService.updateTrilha(id, trilhaUpdate, professorId);
        return ResponseEntity.ok(new TrilhaDTO(atualizada));
    }

    /**
     * DELETE /api/v1/trilhas/{id}
     * Delete a trilha (professor only).
     * Requires professorId query param for verification.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTrilha(
            @PathVariable Long id,
            @RequestParam Long professorId) {

        trilhaService.deleteTrilha(id, professorId);
        return ResponseEntity.ok("Trilha deletada com sucesso.");
    }
}