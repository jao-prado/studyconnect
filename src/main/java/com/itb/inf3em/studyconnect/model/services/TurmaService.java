package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.entity.Turma;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.TurmaRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.security.AuthenticatedUser;
import com.itb.inf3em.studyconnect.security.TurmaAuthorization;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TurmaAuthorization turmaAuthorization;

    public TurmaService(TurmaRepository turmaRepository,
                        UsuarioRepository usuarioRepository,
                        TurmaAuthorization turmaAuthorization) {
        this.turmaRepository = turmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.turmaAuthorization = turmaAuthorization;
    }

    /**
     * Get all turmas (public + all types).
     * Can be filtered in the future if needed.
     */
    public List<Turma> getAllTurmas() {
        return turmaRepository.findAll();
    }

    /**
     * Get a turma by its ID.
     * Throws exception if not found.
     */
    public Turma getTurmaById(Long id) {
        return turmaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Turma não encontrada com o id: " + id
                ));
    }

    /**
     * Get all turmas created by a specific teacher.
     */
    public List<Turma> getTurmasByProfessor(Long professorId) {
        return turmaRepository.findByProfessorId(professorId);
    }

    public List<Turma> getMyTurmas(Long requestedProfessorId) {
        AuthenticatedUser user = turmaAuthorization.requireProfessorOrAdmin();
        Long professorId = user.tipoUsuario() == com.itb.inf3em.studyconnect.model.entity.TipoUsuario.ADMIN
                && requestedProfessorId != null ? requestedProfessorId : user.usuarioId();
        return turmaRepository.findByProfessorId(professorId);
    }

    /**
     * Create a new turma.
     * Validates that the codigo is unique and professorId exists.
     */
    public Turma createTurma(Turma turma) {
        AuthenticatedUser authenticatedUser = turmaAuthorization.requireProfessorOrAdmin();
        Long professorId = authenticatedUser.usuarioId();
        if (authenticatedUser.tipoUsuario() == com.itb.inf3em.studyconnect.model.entity.TipoUsuario.ADMIN
                && turma.getProfessorId() != null) {
            professorId = turma.getProfessorId();
        }
        turma.setProfessorId(professorId);

        // Validate professor exists
        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Professor não encontrado com o id: " + turma.getProfessorId()
                ));

        // Validate codigo is unique
        if (turmaRepository.existsByCodigo(turma.getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este código de turma já está em uso."
            );
        }

        // Validate required fields
        if (turma.getNome() == null || turma.getNome().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nome da turma é obrigatório."
            );
        }

        if (turma.getCodigo() == null || turma.getCodigo().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Código da turma é obrigatório."
            );
        }

        if (turma.getProfessorId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID do professor é obrigatório."
            );
        }

        // Set professor nome if not provided
        if (turma.getProfessorNome() == null || turma.getProfessorNome().isEmpty()) {
            turma.setProfessorNome(professor.getNome());
        }

        turma.setProfessorNome(professor.getNome());

        try {
            return turmaRepository.save(turma);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao salvar turma no banco de dados: " + ex.getMostSpecificCause().getMessage()
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro inesperado ao criar turma: " + ex.getMessage()
            );
        }
    }

    /**
     * Join a turma using codigo.
     * Frontend will handle the actual enrollment (future feature).
     * For now, just validates the codigo exists.
     */
    public Turma joinTurmaByCode(String codigo) {
        return turmaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Turma não encontrada. Verifique o código digitado."
                ));
    }

    /**
     * Update a turma's information.
     * Only the professor who created it can update.
     */
    public Turma updateTurma(Long id, Turma turmaUpdate, Long currentUserId) {
        Turma turmaExistente = getTurmaById(id);
        turmaAuthorization.requireCanManage(turmaExistente);

        // Verify that current user is the professor
        if (false) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não tem permissão para editar esta turma."
            );
        }

        // Update allowed fields
        if (turmaUpdate.getNome() != null && !turmaUpdate.getNome().trim().isEmpty()) {
            turmaExistente.setNome(turmaUpdate.getNome());
        }

        if (turmaUpdate.getDescricao() != null) {
            turmaExistente.setDescricao(turmaUpdate.getDescricao());
        }

        if (turmaUpdate.getTipo() != null) {
            turmaExistente.setTipo(turmaUpdate.getTipo());
        }

        if (turmaUpdate.getNivel() != null) {
            turmaExistente.setNivel(turmaUpdate.getNivel());
        }

        return turmaRepository.save(turmaExistente);
    }

    /**
     * Delete a turma.
     * Only the professor who created it can delete.
     */
    public void deleteTurma(Long id, Long currentUserId) {
        Turma turma = getTurmaById(id);
        turmaAuthorization.requireCanManage(turma);

        // Verify that current user is the professor
        if (false) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não tem permissão para deletar esta turma."
            );
        }

        turmaRepository.delete(turma);
    }
}
