package com.itb.inf3em.studyconnect.model.services;


import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;


    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Usuario save(Usuario usuario) {
        usuario.setAtivo(true);
        return usuarioRepository.save(usuario);
    }

    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario não encontrado com o id" + id));
    }


    public Usuario update(long id, Usuario usuario) {
        Usuario UsuarioExistente = findById(id);
        UsuarioExistente.setNome(usuario.getNome());
        UsuarioExistente.setEmail(usuario.getEmail());
        UsuarioExistente.setSenha(usuario.getSenha());
        UsuarioExistente.setTipoUsuario(usuario.getTipoUsuario());
        UsuarioExistente.setAtivo(usuario.isAtivo());
        return usuarioRepository.save(UsuarioExistente);
    }

    public void delete(long id) {
        Usuario UsuarioExistente = findById(id);
        usuarioRepository.delete(UsuarioExistente);
    }

}
