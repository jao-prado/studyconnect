package com.itb.inf3em.studyconnect.model.services;


import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.entity.Trilha;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.model.repository.TrilhaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
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


    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Usuario save(Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        }

        usuario.setAtivo(true);

        try {
            return usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        }
    }

    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario não encontrado com o id" + id));
    }


    public Usuario update(long id, Usuario usuario) {
        Usuario UsuarioExistente = findById(id);

        if (!UsuarioExistente.getEmail().equalsIgnoreCase(usuario.getEmail())
                && usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        }

        UsuarioExistente.setNome(usuario.getNome());
        UsuarioExistente.setEmail(usuario.getEmail());
        UsuarioExistente.setSenha(usuario.getSenha());
        UsuarioExistente.setTipoUsuario(usuario.getTipoUsuario());
        UsuarioExistente.setAtivo(usuario.isAtivo());
        return usuarioRepository.save(UsuarioExistente);
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
        findById(id); // valida existencia

        // 1. Tabela legada Curso — sem entidade Java, usa SQL nativo
        jdbc.update("DELETE FROM Curso WHERE professor_id = ?", id);

        // 2. Trilhas do professor (aulas deletadas em cascata pelo banco)
        List<Trilha> trilhas = trilhaRepository.findByProfessorId(id);
        if (!trilhas.isEmpty()) {
            trilhaRepository.deleteAll(trilhas);
        }

        // 3. Usuario
        usuarioRepository.deleteById(id);
    }

}
