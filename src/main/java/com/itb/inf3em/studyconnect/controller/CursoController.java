package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.entity.Curso;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/v1/cursos")
public class CursoController {

    List<Curso> cursos = new ArrayList<>();

    @GetMapping
    public List<Curso> findAll() {

        Curso c1 = new Curso();

        c1.setNome("Matemática");
        c1.setDescricao("Curso de matemática básica");
        c1.setDuracao("4 horas");
        c1.setNivel("Iniciante");


        Curso c2 = new Curso();

        c2.setNome("Programação Java");
        c2.setDescricao("Introdução à programação com Java");
        c2.setDuracao("10 horas");
        c2.setNivel("Intermediário");


        Curso c3 = new Curso();

        c3.setNome("Banco de Dados");
        c3.setDescricao("Fundamentos de banco de dados");
        c3.setDuracao("8 horas");
        c3.setNivel("Iniciante");


        cursos.add(c1);
        cursos.add(c2);
        cursos.add(c3);

        return cursos;
    }
}