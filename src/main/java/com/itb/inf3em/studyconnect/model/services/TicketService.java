package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.TicketDTO;
import com.itb.inf3em.studyconnect.model.entity.Ticket;
import com.itb.inf3em.studyconnect.model.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    @Autowired private TicketRepository ticketRepository;
    @Autowired private EmailService emailService;

    public TicketDTO criar(Map<String, Object> body) {
        String mensagem = body.getOrDefault("mensagem", "").toString().trim();
        String tipo     = body.getOrDefault("tipo", "").toString().trim();
        String email    = body.getOrDefault("email", "").toString().trim();

        if (mensagem.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mensagem obrigatória.");
        if (tipo.isEmpty())     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo obrigatório.");

        Ticket t = new Ticket();
        t.setMensagem(mensagem);
        t.setTipo(tipo);
        t.setEmail(email.isEmpty() ? null : email);

        Object uid = body.get("usuarioId");
        if (uid != null && !uid.toString().equals("null"))
            t.setUsuarioId(Long.valueOf(uid.toString()));

        Object nome = body.get("nome");
        if (nome != null) t.setNome(nome.toString().trim());

        return new TicketDTO(ticketRepository.save(t));
    }

    public List<TicketDTO> listarTodos() {
        return ticketRepository.findAllByOrderByCriadaEmDesc()
            .stream().map(TicketDTO::new).toList();
    }

    public List<TicketDTO> listarPorUsuario(Long usuarioId) {
        return ticketRepository.findByUsuarioIdOrderByCriadaEmDesc(usuarioId)
            .stream().map(TicketDTO::new).toList();
    }

    public TicketDTO responder(Long id, String resposta) {
        if (resposta == null || resposta.trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resposta obrigatória.");

        Ticket t = ticketRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado."));

        t.setResposta(resposta.trim());
        t.setStatus("RESPONDIDO");
        t.setRespondidaEm(LocalDateTime.now());
        TicketDTO dto = new TicketDTO(ticketRepository.save(t));

        if (t.getEmail() != null && !t.getEmail().isBlank()) {
            try {
                String subject = "Resposta ao seu ticket #" + t.getId() + " — StudyConnect";
                String body = "Olá" + (t.getNome() != null ? ", " + t.getNome() : "") + "!\n\n"
                    + "Seu ticket foi respondido:\n\n"
                    + "Assunto: " + t.getTipo() + "\n"
                    + "Sua mensagem: " + t.getMensagem() + "\n\n"
                    + "Resposta da equipe:\n" + resposta.trim() + "\n\n"
                    + "Atenciosamente,\nEquipe StudyConnect";
                emailService.sendSimpleEmail(t.getEmail(), subject, body);
            } catch (Exception ex) {
                // nao deixa falha de e-mail derrubar a resposta
                log.warn("[TicketService] Falha ao enviar e-mail para {}: {}", t.getEmail(), ex.getMessage());
            }
        }

        return dto;
    }

    public TicketDTO fechar(Long id) {
        Ticket t = ticketRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado."));
        t.setStatus("FECHADO");
        return new TicketDTO(ticketRepository.save(t));
    }
}
