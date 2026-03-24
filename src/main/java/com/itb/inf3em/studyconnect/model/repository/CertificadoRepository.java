package com.itb.inf3em.studyconnect.model.repository;


import com.itb.inf3em.studyconnect.model.entity.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Long> {
}
