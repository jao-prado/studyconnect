package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.AtualizarPerfilDTO;
import com.itb.inf3em.studyconnect.model.dto.DeleteAccountRequestDTO;
import com.itb.inf3em.studyconnect.model.entity.Trilha;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UsuarioService {

    @Autowired private UsuarioRepository              usuarioRepository;
    @Autowired private TrilhaRepository               trilhaRepository;
    @Autowired private TurmaRepository                turmaRepository;
    @Autowired private MatriculaTrilhaRepository      matriculaRepository;
    @Autowired private ProgressoAulaRepository        progressoRepository;
    @Autowired private DuvidaRepository               duvidaRepository;
    @Autowired private PerfilAprendizadoRepository    perfilRepository;
    @Autowired private SolicitacaoProfessorRepository solicitacaoProfessorRepository;
    @Autowired private EmailChangeTokenRepository     emailChangeTokenRepository;
    @Autowired private MfaCodeRepository              mfaCodeRepository;
    @Autowired private BCryptPasswordEncoder          passwordEncoder;
    @Autowired private CredentialValidationService    credentialValidationService;
    @Autowired private EmailVerificationService       emailVerificationService;
    @Autowired private MfaService                     mfaService;

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Usuario save(Usuario usuario) {
        credentialValidationService.validateEmail(usuario.getEmail());
        credentialValidationService.validatePassword(usuario.getSenha());

        usuario.setEmail(usuario.getEmail().trim().toLowerCase());
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setTipoUsuario(TipoUsuario.ALUNO);

        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail ja esta cadastrado.");
        }

        usuario.setAtivo(false);

        Usuario salvo;
        try {
            salvo = usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail ja esta cadastrado.");
        }

        emailVerificationService.enviarCodigo(salvo.getEmail());
        return salvo;
    }

    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado com o id" + id));
    }

    public Usuario updateOwn(long id, AtualizarPerfilDTO dto) {
        Usuario usuarioExistente = findById(id);
        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            usuarioExistente.setNome(dto.getNome().trim());
        }
        if (dto.getFotoUrl() != null) {
            usuarioExistente.setFotoUrl(dto.getFotoUrl());
        }
        return usuarioRepository.save(usuarioExistente);
    }

    public Usuario updateStatusAsAdmin(long id, boolean ativo) {
        Usuario usuarioExistente = findById(id);
        usuarioExistente.setAtivo(ativo);
        return usuarioRepository.save(usuarioExistente);
    }

    public int removeDuplicateUsuariosByEmail() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        Map<String, Usuario> seen = new HashMap<>();
        int deleted = 0;
        for (Usuario usuario : usuarios) {
            if (usuario.getEmail() == null) continue;
            String normalizedEmail = usuario.getEmail().trim().toLowerCase();
            if (seen.containsKey(normalizedEmail)) {
                usuarioRepository.delete(usuario);
                deleted++;
            } else {
                seen.put(normalizedEmail, usuario);
            }
        }
        return deleted;
    }

    /**
     * Envia o código MFA de exclusão de conta para o e-mail do usuário,
     * após confirmar que a senha está correta.
     * Só é chamado quando mfaHabilitado=true (ou ADMIN).
     */
    public void requestDeleteChallenge(long id, String senha) {
        Usuario usuario = findById(id);

        if (usuario.getSenha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usuários autenticados via Google não podem usar este fluxo.");
        }
        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta.");
        }

        mfaService.iniciarDesafioExclusao(usuario);
    }

    /**
     * Exclui a conta após validar senha e, se MFA ativo, o código de exclusão.
     *
     * Ordem de exclusão respeita as FKs (sem ON DELETE CASCADE):
     *  1. EmailChangeToken
     *  2. MatriculaTrilha
     *  3. ProgressoAula
     *  4. Duvida (aluno)
     *  5-7. [PROFESSOR/ADMIN] Duvidas das trilhas, Trilhas, Turmas
     *  8. PerfilAprendizado
     *  9. SolicitacaoProfessor
     * 10. MfaCode
     * 11. Usuario
     */
    @Transactional
    public void deleteWithAuth(long id, DeleteAccountRequestDTO dto) {
        Usuario usuario = findById(id);

        // Sempre exige senha
        if (usuario.getSenha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usuários autenticados via Google não podem usar este fluxo.");
        }
        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta.");
        }

        // Se MFA ativo (ou ADMIN), exige código de exclusão
        boolean exigeMfa = usuario.isMfaHabilitado()
                || usuario.getTipoUsuario() == TipoUsuario.ADMIN;
        if (exigeMfa) {
            if (dto.getCodigoMfa() == null || dto.getCodigoMfa().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código MFA obrigatório para excluir esta conta.");
            }
            mfaService.validarCodigoExclusao(id, dto.getCodigoMfa());
        }

        excluirDependenciasEUsuario(id, usuario);
    }

    /** Mantido para uso interno (ex: admin excluindo outra conta sem senha própria). */
    @Transactional
    public void delete(long id) {
        Usuario usuario = findById(id);
        excluirDependenciasEUsuario(id, usuario);
    }

    // ── privado ───────────────────────────────────────────────────────────────

    private void excluirDependenciasEUsuario(long id, Usuario usuario) {
        emailChangeTokenRepository.deleteByUsuarioId(id);
        matriculaRepository.deleteByAlunoId(id);
        progressoRepository.deleteByAlunoId(id);
        duvidaRepository.deleteByAlunoId(id);

        if (usuario.getTipoUsuario() == TipoUsuario.PROFESSOR
                || usuario.getTipoUsuario() == TipoUsuario.ADMIN) {
            List<Trilha> trilhas = trilhaRepository.findByProfessorId(id);
            for (Trilha trilha : trilhas) {
                duvidaRepository.deleteByTrilhaId(trilha.getId());
            }
            trilhaRepository.deleteAll(trilhas);
            turmaRepository.deleteAll(turmaRepository.findByProfessorId(id));
        }

        perfilRepository.deleteByAlunoId(id);
        solicitacaoProfessorRepository.deleteByUsuarioId(id);
        mfaCodeRepository.deleteByUsuarioId(id);
        usuarioRepository.deleteById(id);
    }
}
