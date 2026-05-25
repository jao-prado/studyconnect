package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.MatriculaAlunoDTO;
import com.itb.inf3em.studyconnect.model.entity.MatriculaTrilha;
import com.itb.inf3em.studyconnect.model.repository.AulaRepository;
import com.itb.inf3em.studyconnect.model.repository.MatriculaTrilhaRepository;
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
            .map(t -> Map.of(
                "id",     t.getId(),
                "nome",   t.getNome(),
                "alunos", matriculaRepository.countByTrilhaIdAndAtivoTrue(t.getId()),
                "aulas",  aulaRepository.countByTrilhaId(t.getId())
            ))
            .toList();
        resumo.put("trilhas", top3);

        return resumo;
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
