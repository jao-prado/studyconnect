package com.itb.inf3em.studyconnect.model.repository;


import com.itb.inf3em.studyconnect.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario,Integer> {
}
