package com.itb.inf3em.studyconnect.model.repository;

import com.itb.inf3em.studyconnect.model.entity.SolicitacaoProfessor;
import com.itb.inf3em.studyconnect.model.entity.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitacaoProfessorRepository extends JpaRepository<SolicitacaoProfessor, Long> {

    boolean existsByUsuarioIdAndStatus(Long usuarioId, StatusSolicitacao status);

    List<SolicitacaoProfessor> findByUsuarioIdOrderByDataSolicitacaoDesc(Long usuarioId);

    List<SolicitacaoProfessor> findAllByOrderByDataSolicitacaoDesc();

    List<SolicitacaoProfessor> findByStatusOrderByDataSolicitacaoDesc(StatusSolicitacao status);

    void deleteByUsuarioId(Long usuarioId);
}
