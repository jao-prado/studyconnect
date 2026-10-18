package com.itb.inf3em.studyconnect.security;

import com.itb.inf3em.studyconnect.model.dto.DeleteAccountRequestDTO;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.DuvidaRepository;
import com.itb.inf3em.studyconnect.model.repository.EmailChangeTokenRepository;
import com.itb.inf3em.studyconnect.model.repository.MatriculaTrilhaRepository;
import com.itb.inf3em.studyconnect.model.repository.MfaCodeRepository;
import com.itb.inf3em.studyconnect.model.repository.PerfilAprendizadoRepository;
import com.itb.inf3em.studyconnect.model.repository.ProgressoAulaRepository;
import com.itb.inf3em.studyconnect.model.repository.SolicitacaoProfessorRepository;
import com.itb.inf3em.studyconnect.model.repository.TrilhaRepository;
import com.itb.inf3em.studyconnect.model.repository.TurmaRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.model.services.CredentialValidationService;
import com.itb.inf3em.studyconnect.model.services.EmailVerificationService;
import com.itb.inf3em.studyconnect.model.services.MfaService;
import com.itb.inf3em.studyconnect.model.services.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UsuarioDeletionSecurityTest {

    private static final long ADMIN_ID = 1L;
    private static final long TARGET_ID = 20L;

    private UsuarioRepository usuarioRepository;
    private MfaService mfaService;
    private UsuarioService usuarioService;
    private Usuario admin;
    private Usuario target;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        mfaService = mock(MfaService.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(usuarioService, "trilhaRepository", mock(TrilhaRepository.class));
        ReflectionTestUtils.setField(usuarioService, "turmaRepository", mock(TurmaRepository.class));
        ReflectionTestUtils.setField(usuarioService, "matriculaRepository", mock(MatriculaTrilhaRepository.class));
        ReflectionTestUtils.setField(usuarioService, "progressoRepository", mock(ProgressoAulaRepository.class));
        ReflectionTestUtils.setField(usuarioService, "duvidaRepository", mock(DuvidaRepository.class));
        ReflectionTestUtils.setField(usuarioService, "perfilRepository", mock(PerfilAprendizadoRepository.class));
        ReflectionTestUtils.setField(usuarioService, "solicitacaoProfessorRepository",
                mock(SolicitacaoProfessorRepository.class));
        ReflectionTestUtils.setField(usuarioService, "emailChangeTokenRepository",
                mock(EmailChangeTokenRepository.class));
        ReflectionTestUtils.setField(usuarioService, "mfaCodeRepository", mock(MfaCodeRepository.class));
        ReflectionTestUtils.setField(usuarioService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(usuarioService, "credentialValidationService",
                mock(CredentialValidationService.class));
        ReflectionTestUtils.setField(usuarioService, "emailVerificationService",
                mock(EmailVerificationService.class));
        ReflectionTestUtils.setField(usuarioService, "mfaService", mfaService);

        admin = usuario(ADMIN_ID, TipoUsuario.ADMIN, passwordEncoder.encode("senha-admin"));
        admin.setMfaHabilitado(true);
        target = usuario(TARGET_ID, TipoUsuario.ALUNO, passwordEncoder.encode("senha-aluno"));
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
    }

    @Test
    void requestDeleteChallengeConfereSenhaEEnviaCodigoAoAdministrador() {
        usuarioService.requestDeleteChallenge(TARGET_ID, ADMIN_ID, "senha-admin");

        verify(mfaService).iniciarDesafioExclusao(admin);
        verify(mfaService, never()).iniciarDesafioExclusao(target);
    }

    @Test
    void requestDeleteChallengeRejeitaSenhaDoUsuarioAlvo() {
        assertThatThrownBy(() ->
                usuarioService.requestDeleteChallenge(TARGET_ID, ADMIN_ID, "senha-aluno"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(mfaService, never()).iniciarDesafioExclusao(any());
    }

    @Test
    void requestDeleteChallengeEnviaCodigoGoogleSemPedirSenha() {
        admin.setSenha(null);
        admin.setGoogleId("google-admin");

        usuarioService.requestDeleteChallenge(TARGET_ID, ADMIN_ID, null);

        verify(mfaService).iniciarDesafioExclusao(admin);
    }

    @Test
    void requestDeleteChallengeNaoPermitePularSenhaEmContaLocal() {
        assertThatThrownBy(() ->
                usuarioService.requestDeleteChallenge(TARGET_ID, ADMIN_ID, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(mfaService, never()).iniciarDesafioExclusao(any());
    }

    @Test
    void deleteWithAuthConfereCredenciaisDoAdminEExcluiSomenteAlvo() {
        DeleteAccountRequestDTO dto = new DeleteAccountRequestDTO();
        dto.setSenha("senha-admin");
        dto.setCodigoMfa("123456");

        usuarioService.deleteWithAuth(TARGET_ID, ADMIN_ID, dto);

        verify(mfaService).validarCodigoExclusao(ADMIN_ID, "123456");
        verify(usuarioRepository).deleteById(TARGET_ID);
        verify(usuarioRepository, never()).deleteById(ADMIN_ID);
    }

    @Test
    void deleteWithAuthNaoExcluiAlvoComSenhaIncorretaDoAdmin() {
        DeleteAccountRequestDTO dto = new DeleteAccountRequestDTO();
        dto.setSenha("senha-aluno");
        dto.setCodigoMfa("123456");

        assertThatThrownBy(() -> usuarioService.deleteWithAuth(TARGET_ID, ADMIN_ID, dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(usuarioRepository, never()).deleteById(TARGET_ID);
        verify(mfaService, never()).validarCodigoExclusao(anyLong(), anyString());
    }

    @Test
    void deleteWithAuthNaoExcluiAlvoSemCodigoMfaDoAdmin() {
        DeleteAccountRequestDTO dto = new DeleteAccountRequestDTO();
        dto.setSenha("senha-admin");

        assertThatThrownBy(() -> usuarioService.deleteWithAuth(TARGET_ID, ADMIN_ID, dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(usuarioRepository, never()).deleteById(TARGET_ID);
        verify(mfaService, never()).validarCodigoExclusao(anyLong(), anyString());
    }

    @Test
    void deleteWithAuthExigeCodigoParaContaGoogleSemSenha() {
        admin.setSenha(null);
        admin.setGoogleId("google-admin");
        DeleteAccountRequestDTO dto = new DeleteAccountRequestDTO();
        dto.setCodigoMfa("123456");

        usuarioService.deleteWithAuth(ADMIN_ID, ADMIN_ID, dto);

        verify(mfaService).validarCodigoExclusao(ADMIN_ID, "123456");
        verify(usuarioRepository).deleteById(ADMIN_ID);
    }

    @Test
    void deleteWithAuthRejeitaContaGoogleSemCodigo() {
        admin.setSenha(null);
        admin.setGoogleId("google-admin");
        DeleteAccountRequestDTO dto = new DeleteAccountRequestDTO();

        assertThatThrownBy(() -> usuarioService.deleteWithAuth(ADMIN_ID, ADMIN_ID, dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(usuarioRepository, never()).deleteById(ADMIN_ID);
        verify(mfaService, never()).validarCodigoExclusao(anyLong(), anyString());
    }

    @Test
    void deleteWithAuthNaoPermiteContaLocalPularSenhaComCodigo() {
        DeleteAccountRequestDTO dto = new DeleteAccountRequestDTO();
        dto.setCodigoMfa("123456");

        assertThatThrownBy(() -> usuarioService.deleteWithAuth(ADMIN_ID, ADMIN_ID, dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(usuarioRepository, never()).deleteById(ADMIN_ID);
        verify(mfaService, never()).validarCodigoExclusao(anyLong(), anyString());
    }

    @Test
    void deleteWithAuthNaoPermiteQueUsuarioExcluaOutraConta() {
        long otherUserId = 2L;
        Usuario otherUser = usuario(otherUserId, TipoUsuario.PROFESSOR, "hash");
        when(usuarioRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        DeleteAccountRequestDTO dto = new DeleteAccountRequestDTO();
        dto.setSenha("senha");

        assertThatThrownBy(() -> usuarioService.deleteWithAuth(TARGET_ID, otherUserId, dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(usuarioRepository, never()).deleteById(TARGET_ID);
    }

    private Usuario usuario(Long id, TipoUsuario tipo, String senha) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(tipo);
        usuario.setSenha(senha);
        return usuario;
    }
}
