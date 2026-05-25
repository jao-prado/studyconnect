package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.ProgressoDTO;
import com.itb.inf3em.studyconnect.model.entity.ProgressoAula;
import com.itb.inf3em.studyconnect.model.repository.AulaRepository;
import com.itb.inf3em.studyconnect.model.repository.ProgressoAulaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProgressoAulaService {

    @Autowired
    private ProgressoAulaRepository progressoRepository;

    @Autowired
    private AulaRepository aulaRepository;

    /** Upsert: marca aula como concluída. Idempotente. */
    public ProgressoAula concluirAula(Long alunoId, Long aulaId) {
        if (alunoId == null || aulaId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "alunoId e aulaId são obrigatórios.");

        aulaRepository.findById(aulaId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada."));

        ProgressoAula progresso = progressoRepository
            .findByAlunoIdAndAulaId(alunoId, aulaId)
            .orElseGet(() -> new ProgressoAula(alunoId, aulaId));

        if (!progresso.isConcluida()) {
            progresso.setConcluida(true);
            progresso.setConcluidaEm(LocalDateTime.now());
            progressoRepository.save(progresso);
        }

        return progresso;
    }

    /** Retorna IDs das aulas concluídas (usado internamente e pelo hook de progresso). */
    public List<Long> getAulasConcluidas(Long alunoId) {
        return progressoRepository
            .findByAlunoIdAndConcluidaTrue(alunoId)
            .stream()
            .map(ProgressoAula::getAulaId)
            .toList();
    }

    /** Retorna objetos completos com aulaId + concluidaEm (usado pelo gráfico semanal). */
    public List<ProgressoAula> getProgressoCompleto(Long alunoId) {
        return progressoRepository.findByAlunoIdAndConcluidaTrue(alunoId);
    }

    /** Retorna progresso do aluno em uma trilha específica. */
    public ProgressoDTO getProgressoTrilha(Long trilhaId, Long alunoId) {
        List<Long> aulaIds = aulaRepository
            .findByTrilhaIdOrderByOrdem(trilhaId)
            .stream()
            .map(a -> a.getId())
            .toList();

        List<Long> concluidas = progressoRepository
            .findByAlunoIdAndAulaIdInAndConcluidaTrue(alunoId, aulaIds.isEmpty() ? List.of(-1L) : aulaIds)
            .stream()
            .map(ProgressoAula::getAulaId)
            .toList();

        return new ProgressoDTO(concluidas, aulaIds.size());
    }
}
