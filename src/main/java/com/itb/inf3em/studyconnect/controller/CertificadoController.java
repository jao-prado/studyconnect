package com.itb.inf3em.studyconnect.controller;

import com.itb.inf3em.studyconnect.model.entity.Certificado;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/v1/certificados")
public class CertificadoController {

    List<Certificado> certificados = new ArrayList<>();

    @GetMapping
    public List<Certificado> findAll() {

        Certificado c1 = new Certificado();
        c1.setNome("Certificado de Java");
        c1.setDescricao("Certificado de conclusão do curso de Java");

        Certificado c2 = new Certificado();
        c2.setNome("Certificado de Banco de Dados");
        c2.setDescricao("Certificado de conclusão do curso de SQL");

        certificados.add(c1);
        certificados.add(c2);

        return certificados;
    }
}