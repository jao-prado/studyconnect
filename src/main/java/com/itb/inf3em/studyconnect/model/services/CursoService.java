package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.entity.Curso;
import com.itb.inf3em.studyconnect.model.repository.CursoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CursoService {

    @Autowired
    private CursoRepository cursoRepository;

    public List<Curso> findAll() {
        return cursoRepository.findAll();
    }

    public Curso save(Curso curso) {
        curso.setAtivo(true);
        return cursoRepository.save(curso);
    }

    public Curso findById(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Curso não encontrado com o id: " + id));
    }

    public Curso update(long id, Curso curso) {

        Curso cursoExistente = findById(id);

        cursoExistente.setNome(curso.getNome());
        cursoExistente.setDescricao(curso.getDescricao());
        cursoExistente.setDuracao(curso.getDuracao());
        cursoExistente.setNivel(curso.getNivel());
        cursoExistente.setAtivo(curso.isAtivo());

        return cursoRepository.save(cursoExistente);
    }

    public void delete(long id) {

        Curso cursoExistente = findById(id);

        cursoRepository.delete(cursoExistente);
    }
}