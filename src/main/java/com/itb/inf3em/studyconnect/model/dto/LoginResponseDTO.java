package com.itb.inf3em.studyconnect.model.dto;

public class LoginResponseDTO {

    private Long id;
    private String nome;
    private String role;

    public LoginResponseDTO(Long id, String nome, String role) {
        this.id = id;
        this.nome = nome;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getRole() { return role; }
}