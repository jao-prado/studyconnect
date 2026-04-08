package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.dto.MaterialDTO;
import com.itb.inf3em.studyconnect.model.entity.Material;
import com.itb.inf3em.studyconnect.model.services.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/materiais")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @GetMapping
    public ResponseEntity<List<MaterialDTO>> findAll() {
        List<MaterialDTO> dtos = materialService.findAll().stream()
                .map(MaterialDTO::new)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialDTO> findById(@PathVariable Long id) {
        Material material = materialService.findById(id);
        return ResponseEntity.ok(new MaterialDTO(material));
    }

    @PostMapping
    public ResponseEntity<MaterialDTO> cadastrar(@RequestBody Material material) {
        Material novo = materialService.save(material);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MaterialDTO(novo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialDTO> atualizar(@PathVariable Long id,
                                                 @RequestBody Material material) {
        Material atualizado = materialService.update(id, material);
        return ResponseEntity.ok(new MaterialDTO(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletar(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.ok("Material excluído com sucesso!");
    }
}