package com.itb.inf3em.studyconnect.model.repository;

import com.itb.inf3em.studyconnect.model.entity.MfaCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MfaCodeRepository extends JpaRepository<MfaCode, Long> {

    Optional<MfaCode> findByUsuarioId(Long usuarioId);

    Optional<MfaCode> findByUsuarioIdAndPurpose(Long usuarioId, String purpose);

    void deleteByUsuarioId(Long usuarioId);
}
