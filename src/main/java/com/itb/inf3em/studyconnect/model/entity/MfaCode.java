package com.itb.inf3em.studyconnect.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mfa_code",
       uniqueConstraints = @jakarta.persistence.UniqueConstraint(
               name = "UQ_mfa_code_usuario_purpose",
               columnNames = {"usuario_id", "purpose"}))
public class MfaCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** LOGIN | DELETE_ACCOUNT */
    @Column(name = "purpose", nullable = false, length = 20)
    private String purpose;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private int tentativas;

    public MfaCode() {}

    public MfaCode(Long usuarioId, String purpose, String codeHash, Instant expiresAt) {
        this.usuarioId = usuarioId;
        this.purpose   = purpose;
        this.codeHash  = codeHash;
        this.expiresAt = expiresAt;
        this.used      = false;
        this.tentativas = 0;
    }

    public Long getId()                        { return id; }
    public Long getUsuarioId()                 { return usuarioId; }
    public String getPurpose()                 { return purpose; }
    public String getCodeHash()                { return codeHash; }
    public void   setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public Instant getExpiresAt()              { return expiresAt; }
    public void    setExpiresAt(Instant t)     { this.expiresAt = t; }
    public boolean isUsed()                    { return used; }
    public void    setUsed(boolean used)       { this.used = used; }
    public int  getTentativas()                { return tentativas; }
    public void setTentativas(int tentativas)  { this.tentativas = tentativas; }
}
