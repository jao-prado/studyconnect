package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.dto.CursoDTO;
import com.itb.inf3em.studyconnect.model.entity.Curso;
import com.itb.inf3em.studyconnect.model.services.CursoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/cursos")
public class CursoController {

    @Autowired
    private CursoService cursoService;

    @GetMapping
    public ResponseEntity<List<CursoDTO>> findAll() {
        List<CursoDTO> dtos = cursoService.findAll().stream()
                .map(CursoDTO::new)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CursoDTO> findById(@PathVariable Long id) {
        Curso curso = cursoService.findById(id);
        return ResponseEntity.ok(new CursoDTO(curso));
    }

    @PostMapping
    public ResponseEntity<CursoDTO> cadastrar(@RequestBody Curso curso) {
        Curso novo = cursoService.save(curso);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CursoDTO(novo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CursoDTO> atualizar(@PathVariable Long id,
                                              @RequestBody Curso curso) {
        Curso atualizado = cursoService.update(id, curso);
        return ResponseEntity.ok(new CursoDTO(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletar(@PathVariable Long id) {
        cursoService.delete(id);
        return ResponseEntity.ok("Curso excluído com sucesso!");
    }
}