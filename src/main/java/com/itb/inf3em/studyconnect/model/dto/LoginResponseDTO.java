package com.itb.inf3em.studyconnect.model.dto;

public class LoginResponseDTO {

    private String nome;
    private String role;

    public LoginResponseDTO(String nome, String role) {
        this.nome = nome;
        this.role = role;
    }

    public String getNome() {
        return nome;
    }

    public String getRole() {
        return role;
    }
}