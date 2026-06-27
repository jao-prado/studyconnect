package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.dto.TicketDTO;
import com.itb.inf3em.studyconnect.model.services.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    /** POST /api/v1/tickets
     *  Body: { usuarioId?, nome?, email, tipo, mensagem } */
    @PostMapping
    public ResponseEntity<TicketDTO> criar(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.criar(body));
    }

    /** GET /api/v1/tickets — lista todos (admin) */
    @GetMapping
    public ResponseEntity<List<TicketDTO>> listar() {
        return ResponseEntity.ok(ticketService.listarTodos());
    }

    /** GET /api/v1/tickets/usuario/{usuarioId} */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<TicketDTO>> porUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(ticketService.listarPorUsuario(usuarioId));
    }

    /** POST /api/v1/tickets/{id}/responder
     *  Body: { resposta } */
    @PostMapping("/{id}/responder")
    public ResponseEntity<TicketDTO> responder(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ticketService.responder(id, body.get("resposta")));
    }

    /** POST /api/v1/tickets/{id}/fechar */
    @PostMapping("/{id}/fechar")
    public ResponseEntity<TicketDTO> fechar(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.fechar(id));
    }
}
