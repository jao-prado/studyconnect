package com.itb.inf3em.studyconnect.model.dto;

import com.itb.inf3em.studyconnect.model.entity.Certificado;
import com.itb.inf3em.studyconnect.model.entity.Usuario;

import java.util.List;
import java.util.stream.Collectors;

public class UsuarioDTO {

    private Long id;
    private String nome;
    private String email;
    private String tipoUsuario;
    private boolean ativo;
    private List<String> certificados;

    public UsuarioDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.tipoUsuario = usuario.getTipoUsuario().name();
        this.ativo = usuario.isAtivo();

        try {
            if (usuario.getCertificados() != null) {
                this.certificados = usuario.getCertificados().stream()
                        .map(Certificado::getNome)
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
            // certificados LAZY não carregados — omite sem quebrar a resposta
        }
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTipoUsuario() { return tipoUsuario; }
    public boolean isAtivo() { return ativo; }
    public List<String> getCertificados() { return certificados; }
}
