package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/v1/usuarios")
public class UsuarioController {

    List<Usuario> usuarios = new ArrayList<>();

    @GetMapping
    public List<Usuario> findAll() {

        Usuario u1 = new Usuario();
        u1.setNome("João Pedro");
        u1.setEmail("joao@studyconnect.com");
        u1.setSenha("123456");
        u1.setTipoUsuario(TipoUsuario.ALUNO);
        u1.setAtivo(true);

        Usuario u2 = new Usuario();
        u2.setNome("Carlos Silva");
        u2.setEmail("carlos@studyconnect.com");
        u2.setSenha("123456");
        u2.setTipoUsuario(TipoUsuario.PROFESSOR);
        u2.setAtivo(true);

        Usuario u3 = new Usuario();
        u3.setNome("Ana Souza");
        u3.setEmail("ana@studyconnect.com");
        u3.setSenha("123456");
        u3.setTipoUsuario(TipoUsuario.ADMIN);
        u3.setAtivo(true);

        usuarios.add(u1);
        usuarios.add(u2);
        usuarios.add(u3);

        return usuarios;
    }
}