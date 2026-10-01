package com.itb.inf3em.studyconnect.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitacao_professor")
public class SolicitacaoProfessor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprovante", length = 30, nullable = false)
    private TipoComprovante tipoComprovante;

    @Column(name = "comprovante_url", columnDefinition = "NVARCHAR(2000)", nullable = false)
    private String comprovanteUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StatusSolicitacao status;

    @Column(name = "data_solicitacao", nullable = false, updatable = false)
    private LocalDateTime dataSolicitacao;

    @Column(name = "data_analise")
    private LocalDateTime dataAnalise;

    @Column(name = "motivo_reprovacao", columnDefinition = "NVARCHAR(1000)")
    private String motivoReprovacao;

    @PrePersist
    protected void onCreate() {
        this.dataSolicitacao = LocalDateTime.now();
    }

    public SolicitacaoProfessor() {}

    public Long getId()                                      { return id; }
    public Usuario getUsuario()                              { return usuario; }
    public void setUsuario(Usuario usuario)                  { this.usuario = usuario; }
    public TipoComprovante getTipoComprovante()              { return tipoComprovante; }
    public void setTipoComprovante(TipoComprovante t)        { this.tipoComprovante = t; }
    public String getComprovanteUrl()                        { return comprovanteUrl; }
    public void setComprovanteUrl(String comprovanteUrl)     { this.comprovanteUrl = comprovanteUrl; }
    public StatusSolicitacao getStatus()                     { return status; }
    public void setStatus(StatusSolicitacao status)          { this.status = status; }
    public LocalDateTime getDataSolicitacao()                { return dataSolicitacao; }
    public LocalDateTime getDataAnalise()                    { return dataAnalise; }
    public void setDataAnalise(LocalDateTime dataAnalise)    { this.dataAnalise = dataAnalise; }
    public String getMotivoReprovacao()                      { return motivoReprovacao; }
    public void setMotivoReprovacao(String motivo)           { this.motivoReprovacao = motivo; }
}
