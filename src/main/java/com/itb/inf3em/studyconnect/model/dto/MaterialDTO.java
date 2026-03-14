package com.itb.inf3em.studyconnect.model.dto;

import com.itb.inf3em.studyconnect.model.entity.Material;

public class MaterialDTO {

    private Long id;
    private String titulo;
    private String descricao;
    private String categoria;
    private boolean ativo;
    private String curso;

    public MaterialDTO(Material material) {
        this.id = material.getId();
        this.titulo = material.getTitulo();
        this.descricao = material.getDescricao();
        this.categoria = material.getCategoria();
        this.ativo = material.isAtivo();
        this.curso = material.getCurso() != null ? material.getCurso().getNome() : null;
    }


    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public boolean isAtivo() { return ativo; }
    public String getCurso() { return curso; }
}