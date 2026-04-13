package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.entity.Trilha;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.TrilhaRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TrilhaService {

    @Autowired
    private TrilhaRepository trilhaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Get all trilhas (public + all types).
     * Can be filtered in the future if needed.
     */
    public List<Trilha> getAllTrilhas() {
        return trilhaRepository.findAll();
    }

    /**
     * Get a trilha by its ID.
     * Throws exception if not found.
     */
    public Trilha getTrilhaById(Long id) {
        return trilhaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trilha não encontrada com o id: " + id
                ));
    }

    /**
     * Get all trilhas created by a specific teacher.
     */
    public List<Trilha> getTrilhasByProfessor(Long professorId) {
        return trilhaRepository.findByProfessorId(professorId);
    }

    /**
     * Create a new trilha.
     * Validates that the professorId exists.
     */
    public Trilha createTrilha(Trilha trilha) {
        // Validate professor exists
        Usuario professor = usuarioRepository.findById(trilha.getProfessorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Professor não encontrado com o id: " + trilha.getProfessorId()
                ));

        // Validate required fields
        if (trilha.getNome() == null || trilha.getNome().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nome da trilha é obrigatório."
            );
        }

        if (trilha.getTipo() == null || trilha.getTipo().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo da trilha é obrigatório."
            );
        }

        if (trilha.getNivel() == null || trilha.getNivel().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nível da trilha é obrigatório."
            );
        }

        if (trilha.getProfessorId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID do professor é obrigatório."
            );
        }

        // Set professor nome if not provided
        if (trilha.getProfessorNome() == null || trilha.getProfessorNome().isEmpty()) {
            trilha.setProfessorNome(professor.getNome());
        }

        try {
            return trilhaRepository.save(trilha);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao salvar trilha no banco de dados: " + ex.getMostSpecificCause().getMessage()
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro inesperado ao criar trilha: " + ex.getMessage()
            );
        }
    }

    /**
     * Update a trilha's information.
     * Only the professor who created it can update.
     */
    public Trilha updateTrilha(Long id, Trilha trilhaUpdate, Long currentUserId) {
        Trilha trilhaExistente = getTrilhaById(id);

        // Verify that current user is the professor
        if (!trilhaExistente.getProfessorId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não tem permissão para editar esta trilha."
            );
        }

        // Update allowed fields
        if (trilhaUpdate.getNome() != null && !trilhaUpdate.getNome().trim().isEmpty()) {
            trilhaExistente.setNome(trilhaUpdate.getNome());
        }

        if (trilhaUpdate.getDescricao() != null) {
            trilhaExistente.setDescricao(trilhaUpdate.getDescricao());
        }

        if (trilhaUpdate.getTipo() != null) {
            trilhaExistente.setTipo(trilhaUpdate.getTipo());
        }

        if (trilhaUpdate.getNivel() != null) {
            trilhaExistente.setNivel(trilhaUpdate.getNivel());
        }

        return trilhaRepository.save(trilhaExistente);
    }

    /**
     * Delete a trilha.
     * Only the professor who created it can delete.
     */
    public void deleteTrilha(Long id, Long currentUserId) {
        Trilha trilha = getTrilhaById(id);

        // Verify that current user is the professor
        if (!trilha.getProfessorId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não tem permissão para deletar esta trilha."
            );
        }

        trilhaRepository.delete(trilha);
    }
}