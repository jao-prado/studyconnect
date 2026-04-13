package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.entity.Aula;
import com.itb.inf3em.studyconnect.model.entity.Trilha;
import com.itb.inf3em.studyconnect.model.repository.AulaRepository;
import com.itb.inf3em.studyconnect.model.repository.TrilhaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AulaService {

    @Autowired
    private AulaRepository aulaRepository;

    @Autowired
    private TrilhaRepository trilhaRepository;

    /**
     * Get all aulas for a specific trilha.
     */
    public List<Aula> getAulasByTrilha(Long trilhaId) {
        // Verify trilha exists
        trilhaRepository.findById(trilhaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trilha não encontrada com o id: " + trilhaId
                ));

        return aulaRepository.findByTrilhaIdOrderByOrdem(trilhaId);
    }

    /**
     * Get a aula by its ID.
     * Throws exception if not found.
     */
    public Aula getAulaById(Long id) {
        return aulaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aula não encontrada com o id: " + id
                ));
    }

    /**
     * Create a new aula.
     */
    public Aula createAula(Aula aula) {
        // Validate trilha exists
        Trilha trilha = trilhaRepository.findById(aula.getTrilhaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trilha não encontrada com o id: " + aula.getTrilhaId()
                ));

        // Validate required fields
        if (aula.getTitulo() == null || aula.getTitulo().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Título da aula é obrigatório."
            );
        }

        if (aula.getTipo() == null || aula.getTipo().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo da aula é obrigatório."
            );
        }

        if (aula.getConteudo() == null || aula.getConteudo().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Conteúdo da aula é obrigatório."
            );
        }

        if (aula.getTrilhaId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID da trilha é obrigatório."
            );
        }

        try {
            return aulaRepository.save(aula);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro inesperado ao criar aula: " + ex.getMessage()
            );
        }
    }

    /**
     * Update a aula's information.
     */
    public Aula updateAula(Long id, Aula aulaUpdate) {
        Aula aulaExistente = getAulaById(id);

        // Update allowed fields
        if (aulaUpdate.getTitulo() != null && !aulaUpdate.getTitulo().trim().isEmpty()) {
            aulaExistente.setTitulo(aulaUpdate.getTitulo());
        }

        if (aulaUpdate.getTipo() != null) {
            aulaExistente.setTipo(aulaUpdate.getTipo());
        }

        if (aulaUpdate.getConteudo() != null) {
            aulaExistente.setConteudo(aulaUpdate.getConteudo());
        }

        if (aulaUpdate.getOrdem() != null) {
            aulaExistente.setOrdem(aulaUpdate.getOrdem());
        }

        return aulaRepository.save(aulaExistente);
    }

    /**
     * Delete a aula.
     */
    public void deleteAula(Long id) {
        Aula aula = getAulaById(id);
        aulaRepository.delete(aula);
    }
}