package com.itb.inf3em.studyconnect.model.dto;

import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.entity.Curso;
import com.itb.inf3em.studyconnect.model.entity.Material;
import com.itb.inf3em.studyconnect.model.entity.Certificado;

import java.util.List;
import java.util.stream.Collectors;

public class UsuarioDTO {

    private Long id;
    private String nome;
    private String email;
    private String tipoUsuario;
    private List<String> cursos;
    private List<String> materiais;
    private List<String> certificados;
    private boolean ativo;

    public UsuarioDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.tipoUsuario = usuario.getTipoUsuario().name();
        this.ativo = usuario.isAtivo();

        if (usuario.getCursos() != null) {
            this.cursos = usuario.getCursos().stream()
                    .map(Curso::getNome)
                    .collect(Collectors.toList());

            this.materiais = usuario.getCursos().stream()
                    .flatMap(c -> c.getMateriais() != null ? c.getMateriais().stream() : List.<Material>of().stream())
                    .map(Material::getTitulo)
                    .collect(Collectors.toList());
        }

        if (usuario.getCertificados() != null) {
            this.certificados = usuario.getCertificados().stream()
                    .map(Certificado::getNome)
                    .collect(Collectors.toList());
        }
    }


    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTipoUsuario() { return tipoUsuario; }
    public List<String> getCursos() { return cursos; }
    public List<String> getMateriais() { return materiais; }
    public List<String> getCertificados() { return certificados; }
    public boolean isAtivo() { return ativo; }
}