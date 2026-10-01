package com.itb.inf3em.studyconnect.model.dto;

public class LoginResponseDTO {

    private Long id;
    private String nome;
    private String role;
    private String fotoUrl;
    private String email;
    private boolean ativo;
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private boolean mfaPendente;
    private boolean mfaHabilitado;

    /** Construtor para login completo (sem MFA pendente). */
    public LoginResponseDTO(Long id, String nome, String role, String fotoUrl, String email, boolean ativo,
                            String accessToken, long expiresIn) {
        this.id = id;
        this.nome = nome;
        this.role = role;
        this.fotoUrl = fotoUrl;
        this.email = email;
        this.ativo = ativo;
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.mfaPendente = false;
        this.mfaHabilitado = false;
    }

    /** Construtor para login completo com flag mfaHabilitado. */
    public LoginResponseDTO(Long id, String nome, String role, String fotoUrl, String email, boolean ativo,
                            String accessToken, long expiresIn, boolean mfaHabilitado) {
        this(id, nome, role, fotoUrl, email, ativo, accessToken, expiresIn);
        this.mfaHabilitado = mfaHabilitado;
    }

    /** Construtor para resposta de MFA pendente — sem accessToken. */
    public LoginResponseDTO(String email, boolean mfaPendente) {
        this.email = email;
        this.mfaPendente = mfaPendente;
        this.accessToken = null;
        this.tokenType = null;
        this.expiresIn = 0;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getRole() { return role; }
    public String getFotoUrl() { return fotoUrl; }
    public String getEmail() { return email; }
    public boolean isAtivo() { return ativo; }
    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public boolean isMfaPendente() { return mfaPendente; }
    public boolean isMfaHabilitado() { return mfaHabilitado; }
}
