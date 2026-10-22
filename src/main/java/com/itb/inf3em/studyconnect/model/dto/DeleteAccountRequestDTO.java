package com.itb.inf3em.studyconnect.model.dto;

public class DeleteAccountRequestDTO {

    private String senha;
    private String codigoMfa; // null quando MFA não está habilitado
    private String googleIdToken;

    public String getSenha()                   { return senha; }
    public void   setSenha(String senha)       { this.senha = senha; }
    public String getCodigoMfa()               { return codigoMfa; }
    public void   setCodigoMfa(String c)       { this.codigoMfa = c; }
    public String getGoogleIdToken()           { return googleIdToken; }
    public void   setGoogleIdToken(String token) { this.googleIdToken = token; }
}
