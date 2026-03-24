package com.itb.inf3em.studyconnect.model.dto;

import com.itb.inf3em.studyconnect.model.entity.Curso;
import com.itb.inf3em.studyconnect.model.entity.Material;

import java.util.List;
import java.util.stream.Collectors;

public class CursoDTO {

    private Long id;
    private String nome;
    private String descricao;
    private String duracao;
    private String nivel;
    private boolean ativo;
    private String professor;
    private List<String> materiais;

    public CursoDTO(Curso curso) {
        this.id = curso.getId();
        this.nome = curso.getNome();
        this.descricao = curso.getDescricao();
        this.duracao = curso.getDuracao();
        this.nivel = curso.getNivel();
        this.ativo = curso.isAtivo();
        this.professor = curso.getProfessor() != null ? curso.getProfessor().getNome() : null;

        if (curso.getMateriais() != null) {
            this.materiais = curso.getMateriais().stream()
                    .map(Material::getTitulo)
                    .collect(Collectors.toList());
        }
    }


    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDuracao() {
        return duracao;
    }

    public String getNivel() {
        return nivel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public String getProfessor() {
        return professor;
    }

    public List<String> getMateriais() {
        return materiais;
    }
}