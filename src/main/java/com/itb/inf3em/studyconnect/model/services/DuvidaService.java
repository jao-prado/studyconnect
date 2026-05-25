package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.DuvidaDTO;
import com.itb.inf3em.studyconnect.model.entity.Duvida;
import com.itb.inf3em.studyconnect.model.repository.AulaRepository;
import com.itb.inf3em.studyconnect.model.repository.DuvidaRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DuvidaService {

    @Autowired private DuvidaRepository duvidaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AulaRepository aulaRepository;

    private DuvidaDTO toDTO(Duvida d) {
        String alunoNome  = usuarioRepository.findById(d.getAlunoId()).map(u -> u.getNome()).orElse("Aluno");
        String aulaTitulo = aulaRepository.findById(d.getAulaId()).map(a -> a.getTitulo()).orElse("Aula");
        return new DuvidaDTO(d, alunoNome, aulaTitulo);
    }

    public DuvidaDTO criar(Map<String, Object> body) {
        Long alunoId  = Long.valueOf(body.get("alunoId").toString());
        Long aulaId   = Long.valueOf(body.get("aulaId").toString());
        Long trilhaId = Long.valueOf(body.get("trilhaId").toString());
        String msg    = body.get("mensagem").toString().trim();

        if (msg.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mensagem obrigatória.");

        usuarioRepository.findById(alunoId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado."));
        aulaRepository.findById(aulaId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada."));

        Duvida d = new Duvida();
        d.setAlunoId(alunoId);
        d.setAulaId(aulaId);
        d.setTrilhaId(trilhaId);
        d.setMensagem(msg);
        return toDTO(duvidaRepository.save(d));
    }

    public DuvidaDTO responder(Long id, String resposta) {
        Duvida d = duvidaRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Dúvida não encontrada."));
        if (resposta == null || resposta.trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resposta obrigatória.");
        d.setResposta(resposta.trim());
        d.setStatus("RESPONDIDA");
        d.setRespondidaEm(LocalDateTime.now());
        return toDTO(duvidaRepository.save(d));
    }

    public DuvidaDTO resolver(Long id) {
        Duvida d = duvidaRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Dúvida não encontrada."));
        d.setStatus("RESPONDIDA");
        if (d.getRespondidaEm() == null) d.setRespondidaEm(LocalDateTime.now());
        return toDTO(duvidaRepository.save(d));
    }

    public List<DuvidaDTO> listarPorTrilha(Long trilhaId) {
        return duvidaRepository.findByTrilhaIdOrderByCriadaEmDesc(trilhaId)
            .stream().map(this::toDTO).toList();
    }

    public List<DuvidaDTO> listarPorAula(Long aulaId) {
        return duvidaRepository.findByAulaIdOrderByCriadaEmDesc(aulaId)
            .stream().map(this::toDTO).toList();
    }

    public List<DuvidaDTO> listarPorAlunoEAula(Long alunoId, Long aulaId) {
        return duvidaRepository.findByAlunoIdAndAulaIdOrderByCriadaEmDesc(alunoId, aulaId)
            .stream().map(this::toDTO).toList();
    }

    public List<DuvidaDTO> listarPorAluno(Long alunoId) {
        return duvidaRepository.findByAlunoIdOrderByCriadaEmDesc(alunoId)
            .stream().map(this::toDTO).toList();
    }
}
