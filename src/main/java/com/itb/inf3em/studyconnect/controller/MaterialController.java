package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.entity.Material;
import com.itb.inf3em.studyconnect.model.services.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/materiais")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @GetMapping
    public ResponseEntity<List<Material>> findAll() {
        return ResponseEntity.ok(materialService.findAll());
    }

    @PostMapping
    public ResponseEntity<Material> cadastrarMaterial(@RequestBody Material material) {
        Material novoMaterial = materialService.save(material);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoMaterial);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> findById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(materialService.findById(Long.parseLong(id)));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "error", "Bad request",
                            "message", "O Id informado não é valido: " + id
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(
                    Map.of(
                            "status", 404,
                            "error", "Not found",
                            "message", "Material não encontrado com o Id: " + id
                    )
            );
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> atualizarMaterial(@PathVariable String id, @RequestBody Material material) {
        try {
            return ResponseEntity.ok(materialService.update(Long.parseLong(id), material));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "error", "Bad request",
                            "message", "O Id informado não é valido: " + id
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(
                    Map.of(
                            "status", 404,
                            "error", "Not found",
                            "message", "Material não encontrado com o Id: " + id
                    )
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deletarMaterial(@PathVariable String id) {
        try {
            materialService.delete(Long.parseLong(id));
            return ResponseEntity.ok().body(
                    Map.of(
                            "status", 200,
                            "message", "Material excluido com sucesso!"
                    )
            );
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "error", "Bad request",
                            "message", "O id informado não é valido: " + id
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(
                    Map.of(
                            "status", 404,
                            "error", "Not found",
                            "message", "Material não encontrado com o Id: " + id
                    )
            );
        }
    }
}