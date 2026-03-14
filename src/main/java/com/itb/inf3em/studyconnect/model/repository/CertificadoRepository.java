package com.itb.inf3em.studyconnect.model.repository;


import com.itb.inf3em.studyconnect.model.entity.Material;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificadoRepository extends CrudRepository<Material, Long> {
}
