package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.MatriculaAlunoDTO;
import com.itb.inf3em.studyconnect.model.entity.MatriculaTrilha;
import com.itb.inf3em.studyconnect.model.repository.AulaRepository;
import com.itb.inf3em.studyconnect.model.repository.DuvidaRepository;
import com.itb.inf3em.studyconnect.model.repository.MatriculaTrilhaRepository;
import com.itb.inf3em.studyconnect.model.repository.ProgressoAulaRepository;
import com.itb.inf3em.studyconnect.model.repository.TrilhaRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatriculaTrilhaService {

    @Autowired
    private MatriculaTrilhaRepository matriculaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TrilhaRepository trilhaRepository;

    @Autowired
    private AulaRepository aulaRepository;

    @Autowired
    private ProgressoAulaRepository progressoAulaRepository;

    @Autowired
    private DuvidaRepository duvidaRepository;

    public MatriculaTrilha matricular(Long alunoId, Long trilhaId) {
        // Valida existência
        usuarioRepository.findById(alunoId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado."));
        trilhaRepository.findById(trilhaId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Trilha não encontrada."));

        // Verifica se já existe (ativa ou inativa)
        matriculaRepository.findByAlunoIdAndTrilhaId(alunoId, trilhaId).ifPresent(m -> {
            if (m.isAtivo()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Aluno já está matriculado nesta trilha.");
            }
            // Reativa matrícula cancelada
            m.setAtivo(true);
            matriculaRepository.save(m);
            throw new AlreadySavedException();
        });

        return matriculaRepository.save(new MatriculaTrilha(alunoId, trilhaId));
    }

    public void desmatricular(Long alunoId, Long trilhaId) {
        MatriculaTrilha m = matriculaRepository
            .findByAlunoIdAndTrilhaId(alunoId, trilhaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Matrícula não encontrada."));
        m.setAtivo(false);
        matriculaRepository.save(m);
    }

    public List<MatriculaAlunoDTO> listarPorTrilha(Long trilhaId) {
        return matriculaRepository.findByTrilhaIdAndAtivoTrue(trilhaId)
            .stream()
            .map(m -> {
                var aluno = usuarioRepository.findById(m.getAlunoId())
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aluno " + m.getAlunoId() + " não encontrado."));
                return new MatriculaAlunoDTO(m, aluno);
            })
            .toList();
    }

    public Map<String, Object> resumoProfessor(Long professorId) {
        var trilhas = trilhaRepository.findByProfessorId(professorId);
        long totalAlunos = trilhas.stream()
            .mapToLong(t -> matriculaRepository.countByTrilhaIdAndAtivoTrue(t.getId()))
            .sum();
        long totalAulas = trilhas.stream()
            .mapToLong(t -> aulaRepository.countByTrilhaId(t.getId()))
            .sum();
        var trilhaComMaisAlunos = trilhas.stream()
            .max(java.util.Comparator.comparingLong(
                t -> matriculaRepository.countByTrilhaIdAndAtivoTrue(t.getId())))
            .orElse(null);

        Map<String, Object> resumo = new HashMap<>();
        resumo.put("totalTrilhas",   trilhas.size());
        resumo.put("totalAlunos",    totalAlunos);
        resumo.put("totalAulas",     totalAulas);
        resumo.put("trilhasAtivas",  trilhas.stream().filter(t -> "PUBLICA".equals(t.getTipo())).count());
        resumo.put("trilhaDestaque", trilhaComMaisAlunos != null ? Map.of(
            "id",   trilhaComMaisAlunos.getId(),
            "nome", trilhaComMaisAlunos.getNome(),
            "alunos", matriculaRepository.countByTrilhaIdAndAtivoTrue(trilhaComMaisAlunos.getId())
        ) : null);

        // top 3 trilhas por alunos matriculados
        var top3 = trilhas.stream()
            .sorted(java.util.Comparator.comparingLong(
                (com.itb.inf3em.studyconnect.model.entity.Trilha t) ->
                    matriculaRepository.countByTrilhaIdAndAtivoTrue(t.getId())).reversed())
            .limit(3)
            .map(t -> {
                long alunos = matriculaRepository.countByTrilhaIdAndAtivoTrue(t.getId());
                var  aulaIds = aulaRepository.findByTrilhaIdOrderByOrdem(t.getId())
                    .stream().map(a -> a.getId()).toList();
                long totalAulasTrilha = aulaIds.size();
                long conclusoes = 0;
                if (alunos > 0 && totalAulasTrilha > 0) {
                    for (var m : matriculaRepository.findByTrilhaIdAndAtivoTrue(t.getId())) {
                        conclusoes += progressoAulaRepository
                            .countByAlunoIdAndAulaIdInAndConcluidaTrue(m.getAlunoId(), aulaIds);
                    }
                }
                long taxa = (alunos > 0 && totalAulasTrilha > 0)
                    ? Math.round((conclusoes * 100.0) / (alunos * totalAulasTrilha)) : 0;
                return Map.of(
                    "id",            t.getId(),
                    "nome",          t.getNome(),
                    "alunos",        alunos,
                    "aulas",         totalAulasTrilha,
                    "taxaConclusao", taxa
                );
            })
            .toList();
        resumo.put("trilhas", top3);

        long totalRascunhos = trilhas.stream()
            .mapToLong(t -> aulaRepository.countByTrilhaIdAndStatus(t.getId(), "RASCUNHO"))
            .sum();
        resumo.put("totalRascunhos", totalRascunhos);

        return resumo;
    }

    public Map<String, Object> estatisticasTrilha(Long trilhaId) {
        long totalAlunos = matriculaRepository.countByTrilhaIdAndAtivoTrue(trilhaId);
        var aulaIds = aulaRepository.findByTrilhaIdOrderByOrdem(trilhaId)
            .stream().map(a -> a.getId()).toList();
        long totalAulas = aulaIds.size();

        long totalConclusoes = 0;
        long totalProgresso  = 0;
        if (totalAulas > 0 && totalAlunos > 0) {
            var alunos = matriculaRepository.findByTrilhaIdAndAtivoTrue(trilhaId);
            for (var m : alunos) {
                totalConclusoes += progressoAulaRepository
                    .countByAlunoIdAndAulaIdInAndConcluidaTrue(m.getAlunoId(), aulaIds);
            }
            totalProgresso = totalAlunos * totalAulas;
        }

        long taxaConclusao = totalProgresso > 0
            ? Math.round((totalConclusoes * 100.0) / totalProgresso) : 0;

        long duvidasPendentes  = duvidaRepository.countByTrilhaIdAndStatus(trilhaId, "PENDENTE");
        long duvidasTotais     = duvidaRepository.findByTrilhaIdOrderByCriadaEmDesc(trilhaId).size();

        var aulaProgresso = aulaRepository.findByTrilhaIdOrderByOrdem(trilhaId).stream().map(a -> {
            long concluiram = totalAlunos > 0
                ? matriculaRepository.findByTrilhaIdAndAtivoTrue(trilhaId).stream()
                    .filter(m -> progressoAulaRepository
                        .findByAlunoIdAndAulaId(m.getAlunoId(), a.getId())
                        .map(p -> p.isConcluida()).orElse(false))
                    .count()
                : 0;
            long pct = totalAlunos > 0 ? Math.round((concluiram * 100.0) / totalAlunos) : 0;
            return Map.of("aulaId", a.getId(), "titulo", a.getTitulo(), "pct", pct, "concluiram", concluiram);
        }).toList();

        return Map.of(
            "totalAlunos",      totalAlunos,
            "totalAulas",       totalAulas,
            "totalConclusoes",  totalConclusoes,
            "taxaConclusao",    taxaConclusao,
            "duvidasPendentes", duvidasPendentes,
            "duvidasTotais",    duvidasTotais,
            "aulaProgresso",    aulaProgresso
        );
    }

    public List<MatriculaTrilha> listarPorAluno(Long alunoId) {
        return matriculaRepository.findByAlunoIdAndAtivoTrue(alunoId);
    }

    public boolean verificarMatricula(Long alunoId, Long trilhaId) {
        return matriculaRepository.existsByAlunoIdAndTrilhaIdAndAtivoTrue(alunoId, trilhaId);
    }

    // Sentinel exception used to short-circuit reactivation flow
    public static class AlreadySavedException extends RuntimeException {}
}
