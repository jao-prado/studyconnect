package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.entity.Material;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/v1/materiais")
public class MaterialController {

    List<Material> materiais = new ArrayList<Material>();

    @GetMapping
    public List<Material> findAll() {

        Material m1 = new Material();

        m1.setTitulo("Apostila de Matemática");
        m1.setDescricao("Material de apoio para matemática básica");
        m1.setCategoria("Apostila");


        Material m2 = new Material();

        m2.setTitulo("Introdução ao Java");
        m2.setDescricao("Material para aprender os conceitos básicos de Java");
        m2.setCategoria("Programação");


        Material m3 = new Material();

        m3.setTitulo("Banco de Dados SQL");
        m3.setDescricao("Material introdutório sobre SQL e banco de dados");
        m3.setCategoria("Banco de Dados");


        materiais.add(m1);
        materiais.add(m2);
        materiais.add(m3);

        return materiais;
    }
}