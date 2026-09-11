package com.itb.inf3em.studyconnect.security;

import com.itb.inf3em.studyconnect.model.entity.Aula;
import com.itb.inf3em.studyconnect.model.entity.Duvida;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.entity.Trilha;
import com.itb.inf3em.studyconnect.model.entity.Turma;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.AulaRepository;
import com.itb.inf3em.studyconnect.model.repository.DuvidaRepository;
import com.itb.inf3em.studyconnect.model.repository.TrilhaRepository;
import com.itb.inf3em.studyconnect.model.repository.TurmaRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.model.services.DuvidaService;
import com.itb.inf3em.studyconnect.model.services.TurmaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DuvidaTurmaAuthorizationTest {

    private final CurrentUser currentUser = new CurrentUser();
    private DuvidaRepository duvidaRepository;
    private AulaRepository aulaRepository;
    private TrilhaRepository trilhaRepository;
    private UsuarioRepository usuarioRepository;
    private TurmaRepository turmaRepository;
    private DuvidaService duvidaService;
    private TurmaService turmaService;

    @BeforeEach
    void setUp() {
        duvidaRepository = mock(DuvidaRepository.class);
        aulaRepository = mock(AulaRepository.class);
        trilhaRepository = mock(TrilhaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        turmaRepository = mock(TurmaRepository.class);
        AlunoAuthorization alunoAuthorization = new AlunoAuthorization(currentUser);
        TrilhaAuthorization trilhaAuthorization = new TrilhaAuthorization(currentUser);
        DuvidaAuthorization duvidaAuthorization = new DuvidaAuthorization(
                currentUser, alunoAuthorization, trilhaAuthorization, aulaRepository, trilhaRepository);
        duvidaService = new DuvidaService(duvidaRepository, usuarioRepository, aulaRepository, duvidaAuthorization);
        turmaService = new TurmaService(turmaRepository, usuarioRepository, new TurmaAuthorization(currentUser));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void alunoACriaDuvidaPropriaMesmoComAlunoETrilhaManipulados() {
        authenticate(10L, TipoUsuario.ALUNO);
        Aula aula = aula(200L, 100L);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario(10L, "Aluno A")));
        when(aulaRepository.findById(200L)).thenReturn(Optional.of(aula));
        when(trilhaRepository.findById(100L)).thenReturn(Optional.of(trilha(100L, 30L)));
        when(duvidaRepository.save(any(Duvida.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var criada = duvidaService.criar(Map.of(
                "alunoId", 20L, "aulaId", 200L, "trilhaId", 999L, "mensagem", "Preciso de ajuda"));

        assertEquals(10L, criada.getAlunoId());
        assertEquals(100L, criada.getTrilhaId());
    }

    @Test
    void alunoANaoPodeResponderOuResolverDuvidaDeB() {
        authenticate(10L, TipoUsuario.ALUNO);
        Duvida duvidaDeB = duvida(500L, 20L, 200L, 100L);
        when(duvidaRepository.findById(500L)).thenReturn(Optional.of(duvidaDeB));

        assertThrows(AccessDeniedException.class, () -> duvidaService.responder(500L, "Resposta"));
        assertThrows(AccessDeniedException.class, () -> duvidaService.resolver(500L));
    }

    @Test
    void professorARespondeDuvidaDaPropriaTrilha() {
        authenticate(30L, TipoUsuario.PROFESSOR);
        Duvida duvida = duvida(500L, 10L, 200L, 100L);
        when(duvidaRepository.findById(500L)).thenReturn(Optional.of(duvida));
        when(aulaRepository.findById(200L)).thenReturn(Optional.of(aula(200L, 100L)));
        when(trilhaRepository.findById(100L)).thenReturn(Optional.of(trilha(100L, 30L)));
        when(duvidaRepository.save(any(Duvida.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario(10L, "Aluno A")));

        duvidaService.responder(500L, "Veja o exemplo da aula.");

        assertEquals("RESPONDIDA", duvida.getStatus());
    }

    @Test
    void professorBNaoPodeResponderDuvidaDaTrilhaDeA() {
        authenticate(40L, TipoUsuario.PROFESSOR);
        when(duvidaRepository.findById(500L)).thenReturn(Optional.of(duvida(500L, 10L, 200L, 100L)));
        when(aulaRepository.findById(200L)).thenReturn(Optional.of(aula(200L, 100L)));
        when(trilhaRepository.findById(100L)).thenReturn(Optional.of(trilha(100L, 30L)));

        assertThrows(AccessDeniedException.class, () -> duvidaService.responder(500L, "Resposta indevida"));
    }

    @Test
    void adminPodeAdministrarQualquerDuvida() {
        authenticate(99L, TipoUsuario.ADMIN);
        Duvida duvida = duvida(500L, 10L, 200L, 100L);
        when(duvidaRepository.findById(500L)).thenReturn(Optional.of(duvida));
        when(duvidaRepository.save(any(Duvida.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario(10L, "Aluno A")));
        when(aulaRepository.findById(200L)).thenReturn(Optional.of(aula(200L, 100L)));

        assertDoesNotThrow(() -> duvidaService.responder(500L, "Resposta administrativa"));
    }

    @Test
    void professorACriaEditaEExcluiTurmaPropria() {
        authenticate(30L, TipoUsuario.PROFESSOR);
        Turma turma = turma(30L, "COD-A");
        turma.setProfessorId(40L);
        when(usuarioRepository.findById(30L)).thenReturn(Optional.of(usuario(30L, "Professor A")));
        when(turmaRepository.existsByCodigo("COD-A")).thenReturn(false);
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Turma criada = turmaService.createTurma(turma);
        assertEquals(30L, criada.getProfessorId());
        assertEquals("Professor A", criada.getProfessorNome());

        when(turmaRepository.findById(700L)).thenReturn(Optional.of(criada));
        Turma atualizacao = new Turma();
        atualizacao.setNome("Turma atualizada");
        turmaService.updateTurma(700L, atualizacao, 40L);
        assertEquals("Turma atualizada", criada.getNome());
        turmaService.deleteTurma(700L, 40L);
        verify(turmaRepository).delete(criada);
    }

    @Test
    void professorBNaoPodeEditarOuExcluirTurmaDeA() {
        authenticate(40L, TipoUsuario.PROFESSOR);
        Turma turmaDeA = turma(30L, "COD-A");
        when(turmaRepository.findById(700L)).thenReturn(Optional.of(turmaDeA));

        assertThrows(AccessDeniedException.class, () -> turmaService.updateTurma(700L, new Turma(), 30L));
        assertThrows(AccessDeniedException.class, () -> turmaService.deleteTurma(700L, 30L));
    }

    @Test
    void alunoNaoPodeAdministrarTurmas() {
        authenticate(10L, TipoUsuario.ALUNO);

        assertThrows(AccessDeniedException.class, () -> turmaService.createTurma(turma(10L, "COD-X")));
    }

    @Test
    void adminPodeAdministrarTurmaDeQualquerProfessor() {
        authenticate(99L, TipoUsuario.ADMIN);
        Turma turmaDeA = turma(30L, "COD-A");
        when(turmaRepository.findById(700L)).thenReturn(Optional.of(turmaDeA));
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> turmaService.updateTurma(700L, new Turma(), 30L));
        assertDoesNotThrow(() -> turmaService.deleteTurma(700L, 30L));
    }

    @Test
    void ausenciaDeIdentidadeBloqueiaDuvidasETurmas() {
        assertThrows(AccessDeniedException.class, () -> duvidaService.criar(Map.of(
                "aulaId", 200L, "mensagem", "Ajuda")));
        assertThrows(AccessDeniedException.class, () -> turmaService.createTurma(turma(30L, "COD-A")));
    }

    private void authenticate(Long usuarioId, TipoUsuario tipoUsuario) {
        AuthenticatedUser user = new AuthenticatedUser(usuarioId, "usuario@example.com", tipoUsuario);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + tipoUsuario.name()))));
    }

    private Aula aula(Long id, Long trilhaId) {
        Aula aula = new Aula();
        aula.setId(id);
        aula.setTrilhaId(trilhaId);
        aula.setTitulo("Aula de teste");
        return aula;
    }

    private Duvida duvida(Long id, Long alunoId, Long aulaId, Long trilhaId) {
        Duvida duvida = new Duvida();
        duvida.setAlunoId(alunoId);
        duvida.setAulaId(aulaId);
        duvida.setTrilhaId(trilhaId);
        duvida.setMensagem("Duvida de teste");
        return duvida;
    }

    private Trilha trilha(Long id, Long professorId) {
        Trilha trilha = new Trilha();
        trilha.setId(id);
        trilha.setProfessorId(professorId);
        return trilha;
    }

    private Turma turma(Long professorId, String codigo) {
        Turma turma = new Turma();
        turma.setProfessorId(professorId);
        turma.setCodigo(codigo);
        turma.setNome("Turma de teste");
        return turma;
    }

    private Usuario usuario(Long id, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        return usuario;
    }
}
