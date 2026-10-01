package com.itb.inf3em.studyconnect.model.dto;

import com.itb.inf3em.studyconnect.model.entity.Trilha;
import java.time.LocalDateTime;

public class TrilhaDTO {

    private Long id;
    private String nome;
    private String descricao;
    private String tipo;
    private String nivel;
    private String disciplina;
    private Long professorId;
    private String professorNome;
    private LocalDateTime criadaEm;
    private LocalDateTime atualizadaEm;
    /** Presente apenas quando o solicitante é o professor responsável ou admin. */
    private String codigoAcesso;

    /** Construtor padrão — sem expor o código de acesso. */
    public TrilhaDTO(Trilha trilha) {
        this(trilha, false);
    }

    /** Construtor com controle de exposição do código. */
    public TrilhaDTO(Trilha trilha, boolean incluirCodigo) {
        this.id            = trilha.getId();
        this.nome          = trilha.getNome();
        this.descricao     = trilha.getDescricao();
        this.tipo          = trilha.getTipo();
        this.nivel         = trilha.getNivel();
        this.disciplina    = trilha.getDisciplina();
        this.professorId   = trilha.getProfessorId();
        this.professorNome = trilha.getProfessorNome();
        this.criadaEm      = trilha.getCriadaEm();
        this.atualizadaEm  = trilha.getAtualizadaEm();
        this.codigoAcesso  = incluirCodigo ? trilha.getCodigoAcesso() : null;
    }

    // Default constructor
    public TrilhaDTO() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public String getDisciplina() { return disciplina; }
    public void setDisciplina(String disciplina) { this.disciplina = disciplina; }

    public Long getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Long professorId) {
        this.professorId = professorId;
    }

    public String getProfessorNome() {
        return professorNome;
    }

    public void setProfessorNome(String professorNome) {
        this.professorNome = professorNome;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }

    public void setAtualizadaEm(LocalDateTime atualizadaEm) {
        this.atualizadaEm = atualizadaEm;
    }

    public String getCodigoAcesso()                    { return codigoAcesso; }
    public void   setCodigoAcesso(String codigoAcesso) { this.codigoAcesso = codigoAcesso; }
}