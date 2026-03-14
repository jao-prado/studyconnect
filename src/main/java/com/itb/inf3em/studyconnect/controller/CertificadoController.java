package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.dto.CertificadoDTO;
import com.itb.inf3em.studyconnect.model.entity.Certificado;
import com.itb.inf3em.studyconnect.model.services.CertificadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/certificados")
public class CertificadoController {

    @Autowired
    private CertificadoService certificadoService;

    @GetMapping
    public ResponseEntity<List<Certificado>> findAll() {
        return ResponseEntity.ok(certificadoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(certificadoService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", 404,
                    "error", "Not Found",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping
    public ResponseEntity<Certificado> cadastrar(@RequestBody CertificadoDTO dto) {
        Certificado cert = certificadoService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        certificadoService.delete(id);
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Certificado excluído com sucesso"
        ));
    }
}