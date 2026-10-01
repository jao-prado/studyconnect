package com.itb.inf3em.studyconnect.model.dto;

import com.itb.inf3em.studyconnect.model.entity.SolicitacaoProfessor;

import java.time.LocalDateTime;

public class SolicitacaoProfessorDTO {

    private Long id;
    private Long usuarioId;
    private String usuarioNome;
    private String usuarioEmail;
    private String tipoComprovante;
    private String comprovanteUrl;
    private String status;
    private LocalDateTime dataSolicitacao;
    private LocalDateTime dataAnalise;
    private String motivoReprovacao;

    public SolicitacaoProfessorDTO() {}

    public SolicitacaoProfessorDTO(SolicitacaoProfessor s) {
        this.id               = s.getId();
        this.usuarioId        = s.getUsuario().getId();
        this.usuarioNome      = s.getUsuario().getNome();
        this.usuarioEmail     = s.getUsuario().getEmail();
        this.tipoComprovante  = s.getTipoComprovante().name();
        this.comprovanteUrl   = s.getComprovanteUrl();
        this.status           = s.getStatus().name();
        this.dataSolicitacao  = s.getDataSolicitacao();
        this.dataAnalise      = s.getDataAnalise();
        this.motivoReprovacao = s.getMotivoReprovacao();
    }

    public Long getId()                        { return id; }
    public Long getUsuarioId()                 { return usuarioId; }
    public String getUsuarioNome()             { return usuarioNome; }
    public String getUsuarioEmail()            { return usuarioEmail; }
    public String getTipoComprovante()         { return tipoComprovante; }
    public String getComprovanteUrl()          { return comprovanteUrl; }
    public String getStatus()                  { return status; }
    public LocalDateTime getDataSolicitacao()  { return dataSolicitacao; }
    public LocalDateTime getDataAnalise()      { return dataAnalise; }
    public String getMotivoReprovacao()        { return motivoReprovacao; }
}
