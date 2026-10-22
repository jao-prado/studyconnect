package com.itb.inf3em.studyconnect.model.dto;

public class DeleteAccountChallengeRequestDTO {
    private String senha;
    private String googleIdToken;

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getGoogleIdToken() { return googleIdToken; }
    public void setGoogleIdToken(String googleIdToken) { this.googleIdToken = googleIdToken; }
}
