package com.itb.inf3em.studyconnect.security;

import com.itb.inf3em.studyconnect.model.entity.Aula;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.entity.Trilha;
import com.itb.inf3em.studyconnect.model.repository.MatriculaTrilhaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AulaAuthorization {

    private final CurrentUser currentUser;
    private final TrilhaAuthorization trilhaAuthorization;
    private final MatriculaTrilhaRepository matriculaRepository;

    public AulaAuthorization(CurrentUser currentUser,
                             TrilhaAuthorization trilhaAuthorization,
                             MatriculaTrilhaRepository matriculaRepository) {
        this.currentUser = currentUser;
        this.trilhaAuthorization = trilhaAuthorization;
        this.matriculaRepository = matriculaRepository;
    }

    public AuthenticatedUser requireCanReadTrilha(Trilha trilha) {
        AuthenticatedUser user = currentUser.require();
        if (user.tipoUsuario() == TipoUsuario.ADMIN) {
            return user;
        }
        if (user.tipoUsuario() == TipoUsuario.PROFESSOR) {
            trilhaAuthorization.requireCanManage(trilha);
            return user;
        }
        if (user.tipoUsuario() == TipoUsuario.ALUNO
                && matriculaRepository.existsByAlunoIdAndTrilhaIdAndAtivoTrue(
                        user.usuarioId(), trilha.getId())) {
            return user;
        }
        throw new AccessDeniedException("Matricula ativa na trilha e obrigatoria para acessar as aulas.");
    }

    public AuthenticatedUser requireCanReadAula(Aula aula, Trilha trilha) {
        AuthenticatedUser user = requireCanReadTrilha(trilha);
        requirePublishedForStudent(aula, user);
        return user;
    }

    public void requirePublishedForCurrentStudent(Aula aula) {
        requirePublishedForStudent(aula, currentUser.require());
    }

    private void requirePublishedForStudent(Aula aula, AuthenticatedUser user) {
        if (user.tipoUsuario() == TipoUsuario.ALUNO && !"PUBLICADA".equals(aula.getStatus())) {
            throw new AccessDeniedException("Aula nao publicada.");
        }
    }
}
