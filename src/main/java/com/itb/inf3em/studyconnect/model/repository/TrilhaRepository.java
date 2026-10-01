package com.itb.inf3em.studyconnect.model.repository;

import com.itb.inf3em.studyconnect.model.entity.Trilha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrilhaRepository extends JpaRepository<Trilha, Long> {

    List<Trilha> findByProfessorId(Long professorId);

    List<Trilha> findByTipo(String tipo);

    java.util.Optional<Trilha> findByCodigoAcesso(String codigoAcesso);

    /**
     * Trilhas visíveis para um aluno:
     *  - todas as públicas
     *  - privadas em que o aluno tem matrícula ativa
     */
    @Query("""
        SELECT t FROM Trilha t
        WHERE t.tipo = 'PUBLICA'
           OR (t.tipo = 'PRIVADA' AND EXISTS (
               SELECT 1 FROM MatriculaTrilha m
               WHERE m.trilhaId = t.id AND m.alunoId = :alunoId AND m.ativo = true
           ))
        """)
    List<Trilha> findVisiveisParaAluno(@Param("alunoId") Long alunoId);
}