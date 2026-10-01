-- =============================================================================
-- MFA — Alterações manuais no banco de dados
-- Executar no SQL Server / Somee
-- =============================================================================

-- 1. Adicionar coluna mfa_habilitado na tabela Usuario
ALTER TABLE dbo.Usuario
    ADD mfa_habilitado BIT NOT NULL
        CONSTRAINT DF_Usuario_mfa_habilitado DEFAULT 0;

-- 2. Criar tabela mfa_code
--    Uma linha por usuário (unicidade em usuario_id).
--    Ao gerar novo desafio, a linha existente é substituída via UPDATE no service.
CREATE TABLE dbo.mfa_code (
    id          BIGINT         IDENTITY(1,1) NOT NULL,
    usuario_id  BIGINT         NOT NULL,
    code_hash   NVARCHAR(255)  NOT NULL,
    expires_at  DATETIMEOFFSET NOT NULL,
    used        BIT            NOT NULL CONSTRAINT DF_mfa_code_used      DEFAULT 0,
    tentativas  INT            NOT NULL CONSTRAINT DF_mfa_code_tentativas DEFAULT 0,
    CONSTRAINT PK_mfa_code          PRIMARY KEY (id),
    CONSTRAINT UQ_mfa_code_usuario  UNIQUE      (usuario_id),
    CONSTRAINT FK_mfa_code_usuario  FOREIGN KEY (usuario_id)
        REFERENCES dbo.Usuario(id)
);

CREATE INDEX idx_mfa_code_usuario_id ON dbo.mfa_code(usuario_id);
