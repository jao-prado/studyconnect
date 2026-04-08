package com.itb.inf3em.studyconnect.model.services;


import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;


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

    public void delete(long id) {
        Usuario UsuarioExistente = findById(id);
        usuarioRepository.delete(UsuarioExistente);
    }

}
