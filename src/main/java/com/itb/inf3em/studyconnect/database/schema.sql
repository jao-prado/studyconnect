-- ============================================================
-- StudyConnect — Schema oficial
-- SQL Server (Somee)  |  estado atual do banco
-- Gerado em: 2025  |  seguro re-executar (idempotente)
-- ============================================================
-- Ordem respeita dependências de FK:
--   Usuario → Trilha → Aula → Duvida
--                    → MatriculaTrilha
--                    → ProgressoAula
--   Usuario → Turma
--   Usuario → PerfilAprendizado
--   (sem FK) → ticket
--   (sem FK) → email_verification_token
--   (sem FK) → password_reset_token
--   (sem FK) → EmailChangeToken
-- ============================================================

-- ── 1. Usuario ───────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Usuario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Usuario (
        id           BIGINT        IDENTITY(1,1) PRIMARY KEY,
        nome         NVARCHAR(45)  NOT NULL,
        email        NVARCHAR(45)  NOT NULL UNIQUE,
        senha        NVARCHAR(255) NULL,
        google_id    NVARCHAR(255) NULL,
        foto_url     NVARCHAR(MAX) NULL,
        tipo_usuario NVARCHAR(20)  NOT NULL,   -- ALUNO | PROFESSOR | ADMIN
        ativo        BIT           NOT NULL DEFAULT 1
    );
    PRINT 'OK: Usuario criada';
END

-- ── 2. Trilha ────────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Trilha', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Trilha (
        id             BIGINT        IDENTITY(1,1) PRIMARY KEY,
        nome           NVARCHAR(100) NOT NULL,
        descricao      NVARCHAR(255) NULL,
        tipo           NVARCHAR(20)  NULL,       -- PUBLICA | PRIVADA
        nivel          NVARCHAR(50)  NULL,
        disciplina     NVARCHAR(50)  NULL,
        professor_id   BIGINT        NOT NULL,
        professor_nome NVARCHAR(100) NOT NULL,
        criada_em      DATETIME2     NOT NULL DEFAULT GETDATE(),
        atualizada_em  DATETIME2     NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_Trilha_Usuario
            FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id)
    );
    CREATE INDEX idx_trilha_professor_id ON dbo.Trilha(professor_id);
    CREATE INDEX idx_trilha_disciplina   ON dbo.Trilha(disciplina);
    PRINT 'OK: Trilha criada';
END

-- ── 3. Aula ──────────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Aula', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Aula (
        id            BIGINT        IDENTITY(1,1) PRIMARY KEY,
        titulo        NVARCHAR(100) NOT NULL,
        tipo          NVARCHAR(20)  NULL,
        conteudo      NVARCHAR(MAX) NULL,
        trilha_id     BIGINT        NOT NULL,
        ordem         INT           NULL,
        status        NVARCHAR(20)  NOT NULL CONSTRAINT DF_Aula_status DEFAULT 'PUBLICADA',
        criada_em     DATETIME2     NOT NULL DEFAULT GETDATE(),
        atualizada_em DATETIME2     NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_Aula_Trilha
            FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id) ON DELETE CASCADE
    );
    CREATE INDEX idx_aula_trilha_id ON dbo.Aula(trilha_id);
    CREATE INDEX idx_aula_ordem     ON dbo.Aula(trilha_id, ordem);
    PRINT 'OK: Aula criada';
END

-- ── 4. Turma ─────────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Turma', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Turma (
        id             BIGINT        IDENTITY(1,1) PRIMARY KEY,
        nome           NVARCHAR(100) NOT NULL,
        descricao      NVARCHAR(255) NULL,
        codigo         NVARCHAR(50)  NOT NULL UNIQUE,
        tipo           NVARCHAR(20)  NULL,
        nivel          NVARCHAR(50)  NULL,
        professor_id   BIGINT        NOT NULL,
        professor_nome NVARCHAR(100) NOT NULL,
        criada_em      DATETIME2     NOT NULL DEFAULT GETDATE(),
        atualizada_em  DATETIME2     NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_Turma_Usuario
            FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id)
    );
    CREATE INDEX idx_turma_codigo    ON dbo.Turma(codigo);
    CREATE INDEX idx_turma_professor ON dbo.Turma(professor_id);
    PRINT 'OK: Turma criada';
END

-- ── 5. MatriculaTrilha ───────────────────────────────────────
IF OBJECT_ID(N'dbo.MatriculaTrilha', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MatriculaTrilha (
        id             BIGINT    IDENTITY(1,1) PRIMARY KEY,
        aluno_id       BIGINT    NOT NULL,
        trilha_id      BIGINT    NOT NULL,
        data_matricula DATETIME2 NOT NULL DEFAULT GETDATE(),
        ativo          BIT       NOT NULL DEFAULT 1,
        CONSTRAINT FK_Matricula_Aluno
            FOREIGN KEY (aluno_id)  REFERENCES dbo.Usuario(id),
        CONSTRAINT FK_Matricula_Trilha
            FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id) ON DELETE CASCADE,
        CONSTRAINT UQ_Matricula_Aluno_Trilha
            UNIQUE (aluno_id, trilha_id)
    );
    CREATE INDEX idx_matricula_aluno  ON dbo.MatriculaTrilha(aluno_id);
    CREATE INDEX idx_matricula_trilha ON dbo.MatriculaTrilha(trilha_id);
    PRINT 'OK: MatriculaTrilha criada';
END

-- ── 6. ProgressoAula ─────────────────────────────────────────
IF OBJECT_ID(N'dbo.ProgressoAula', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProgressoAula (
        id           BIGINT    IDENTITY(1,1) PRIMARY KEY,
        aluno_id     BIGINT    NOT NULL,
        aula_id      BIGINT    NOT NULL,
        concluida    BIT       NOT NULL DEFAULT 0,
        concluida_em DATETIME2 NULL,
        CONSTRAINT FK_Progresso_Aluno
            FOREIGN KEY (aluno_id) REFERENCES dbo.Usuario(id),
        CONSTRAINT FK_Progresso_Aula
            FOREIGN KEY (aula_id)  REFERENCES dbo.Aula(id) ON DELETE CASCADE,
        CONSTRAINT UQ_Progresso_Aluno_Aula
            UNIQUE (aluno_id, aula_id)
    );
    CREATE INDEX idx_progresso_aluno ON dbo.ProgressoAula(aluno_id);
    CREATE INDEX idx_progresso_aula  ON dbo.ProgressoAula(aula_id);
    PRINT 'OK: ProgressoAula criada';
END

-- ── 7. Duvida ────────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Duvida', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Duvida (
        id            BIGINT         IDENTITY(1,1) PRIMARY KEY,
        aluno_id      BIGINT         NOT NULL,
        aula_id       BIGINT         NOT NULL,
        trilha_id     BIGINT         NOT NULL,
        mensagem      NVARCHAR(1000) NOT NULL,
        resposta      NVARCHAR(1000) NULL,
        status        NVARCHAR(20)   NOT NULL CONSTRAINT DF_Duvida_status DEFAULT 'PENDENTE',
        criada_em     DATETIME2      NOT NULL DEFAULT GETDATE(),
        respondida_em DATETIME2      NULL,
        CONSTRAINT FK_Duvida_Aluno  FOREIGN KEY (aluno_id)  REFERENCES dbo.Usuario(id),
        CONSTRAINT FK_Duvida_Aula   FOREIGN KEY (aula_id)   REFERENCES dbo.Aula(id) ON DELETE CASCADE,
        CONSTRAINT FK_Duvida_Trilha FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id)
    );
    CREATE INDEX idx_duvida_aluno  ON dbo.Duvida(aluno_id);
    CREATE INDEX idx_duvida_aula   ON dbo.Duvida(aula_id);
    CREATE INDEX idx_duvida_trilha ON dbo.Duvida(trilha_id);
    PRINT 'OK: Duvida criada';
END

-- ── 8. PerfilAprendizado ─────────────────────────────────────
IF OBJECT_ID(N'dbo.PerfilAprendizado', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PerfilAprendizado (
        id           BIGINT        IDENTITY(1,1) PRIMARY KEY,
        aluno_id     BIGINT        NOT NULL UNIQUE,
        objetivo     NVARCHAR(30)  NULL,
        nivel        NVARCHAR(30)  NULL,
        horas_semana INT           NULL,
        meta_semanal INT           NULL,
        ritmo        NVARCHAR(20)  NULL,
        interesses   NVARCHAR(500) NULL,
        dificuldades NVARCHAR(500) NULL,
        CONSTRAINT FK_Perfil_Aluno
            FOREIGN KEY (aluno_id) REFERENCES dbo.Usuario(id) ON DELETE CASCADE
    );
    CREATE INDEX idx_perfil_aluno ON dbo.PerfilAprendizado(aluno_id);
    PRINT 'OK: PerfilAprendizado criada';
END

-- ── 9. ticket ────────────────────────────────────────────────
IF OBJECT_ID(N'dbo.ticket', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ticket (
        id            BIGINT         IDENTITY(1,1) PRIMARY KEY,
        usuario_id    BIGINT         NULL,
        nome          NVARCHAR(150)  NULL,
        email         NVARCHAR(150)  NULL,
        tipo          NVARCHAR(100)  NOT NULL,
        mensagem      NVARCHAR(2000) NOT NULL,
        resposta      NVARCHAR(2000) NULL,
        status        NVARCHAR(20)   NOT NULL DEFAULT 'ABERTO',
        criada_em     DATETIME2      NOT NULL DEFAULT GETDATE(),
        respondida_em DATETIME2      NULL
    );
    PRINT 'OK: ticket criada';
END

-- ── 10. email_verification_token ─────────────────────────────
IF OBJECT_ID(N'dbo.email_verification_token', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.email_verification_token (
        id         BIGINT           IDENTITY(1,1) PRIMARY KEY,
        email      NVARCHAR(255)    NOT NULL UNIQUE,
        code       NVARCHAR(6)      NOT NULL,
        expires_at DATETIMEOFFSET   NOT NULL,
        verified   BIT              NOT NULL DEFAULT 0
    );
    PRINT 'OK: email_verification_token criada';
END

-- ── 11. password_reset_token ─────────────────────────────────
IF OBJECT_ID(N'dbo.password_reset_token', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.password_reset_token (
        id         BIGINT           IDENTITY(1,1) PRIMARY KEY,
        email      NVARCHAR(255)    NOT NULL,
        token      NVARCHAR(255)    NOT NULL UNIQUE,
        expires_at DATETIMEOFFSET   NOT NULL,
        used       BIT              NOT NULL DEFAULT 0
    );
    PRINT 'OK: password_reset_token criada';
END

-- ── 12. EmailChangeToken ─────────────────────────────────────
IF OBJECT_ID(N'dbo.EmailChangeToken', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.EmailChangeToken (
        id             BIGINT          IDENTITY(1,1) PRIMARY KEY,
        usuario_id     BIGINT          NOT NULL,
        email_atual    NVARCHAR(255)   NOT NULL,
        email_novo     NVARCHAR(255)   NOT NULL,
        confirm_token  NVARCHAR(255)   NOT NULL UNIQUE,
        otp_code       NVARCHAR(6)     NULL,
        etapa          NVARCHAR(10)    NOT NULL DEFAULT 'STEP1',
        expires_at     DATETIMEOFFSET  NOT NULL
    );
    PRINT 'OK: EmailChangeToken criada';
END
