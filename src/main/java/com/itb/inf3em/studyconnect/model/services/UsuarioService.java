package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.entity.Trilha;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.TrilhaRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.model.services.EmailVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TrilhaRepository trilhaRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private CredentialValidationService credentialValidationService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Usuario save(Usuario usuario) {
        credentialValidationService.validateEmail(usuario.getEmail());
        credentialValidationService.validatePassword(usuario.getSenha());

        usuario.setEmail(usuario.getEmail().trim().toLowerCase());
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));

        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail ja esta cadastrado.");
        }

        usuario.setAtivo(false); // ativado apenas após verificação de e-mail

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

    public Usuario update(long id, Usuario usuario) {
        Usuario usuarioExistente = findById(id);

        credentialValidationService.validateEmail(usuario.getEmail());
        String normalizedEmail = usuario.getEmail().trim().toLowerCase();

        if (!usuarioExistente.getEmail().equalsIgnoreCase(normalizedEmail)
                && usuarioRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail ja esta cadastrado.");
        }

        usuarioExistente.setNome(usuario.getNome());
        usuarioExistente.setEmail(normalizedEmail);

        if (usuario.getSenha() != null && !usuario.getSenha().isBlank()) {
            credentialValidationService.validatePassword(usuario.getSenha());
            usuarioExistente.setSenha(passwordEncoder.encode(usuario.getSenha()));
        }

        usuarioExistente.setTipoUsuario(usuario.getTipoUsuario());
        usuarioExistente.setAtivo(usuario.isAtivo());
        return usuarioRepository.save(usuarioExistente);
    }

    public int removeDuplicateUsuariosByEmail() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        Map<String, Usuario> seen = new HashMap<>();
        int deleted = 0;

        for (Usuario usuario : usuarios) {
            if (usuario.getEmail() == null) {
                continue;
            }

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

    @Transactional
    public void delete(long id) {
        findById(id);

        jdbc.update("DELETE FROM Curso WHERE professor_id = ?", id);

        List<Trilha> trilhas = trilhaRepository.findByProfessorId(id);
        if (!trilhas.isEmpty()) {
            trilhaRepository.deleteAll(trilhas);
        }

        usuarioRepository.deleteById(id);
    }
}
