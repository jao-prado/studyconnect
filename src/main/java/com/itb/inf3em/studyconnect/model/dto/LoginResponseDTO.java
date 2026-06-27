package com.itb.inf3em.studyconnect.model.dto;

public class LoginResponseDTO {

    private Long id;
    private String nome;
    private String role;
    private String fotoUrl;
    private String email;
    private boolean ativo;

    public LoginResponseDTO(Long id, String nome, String role, String fotoUrl, String email, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.role = role;
        this.fotoUrl = fotoUrl;
        this.email = email;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getRole() { return role; }
    public String getFotoUrl() { return fotoUrl; }
    public String getEmail() { return email; }
    public boolean isAtivo() { return ativo; }
}
