-- StudyConnect - schema completo atual para SQL Server / Somee
-- Seguro para executar em um banco que ja tenha tabelas/dados:
--   * nao apaga tabelas nem registros;
--   * cria tabelas ausentes;
--   * adiciona colunas/migracoes atuais que estiverem faltando;
--   * cria indices/constraints unicos quando estiverem faltando.
--
-- Inclui as 14 tabelas atuais do backend:
-- Aula, Duvida, email_verification_token, EmailChangeToken,
-- MatriculaTrilha, mfa_code, password_reset_token, PerfilAprendizado,
-- ProgressoAula, solicitacao_professor, ticket, Trilha, Turma e Usuario.

SET XACT_ABORT ON;
BEGIN TRANSACTION;

-- 1. Tabelas base
IF OBJECT_ID(N'dbo.Usuario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Usuario (
        id BIGINT IDENTITY(1,1) NOT NULL,
        nome NVARCHAR(45) NOT NULL,
        email NVARCHAR(45) NOT NULL,
        senha NVARCHAR(255) NULL,
        google_id NVARCHAR(255) NULL,
        foto_url NVARCHAR(MAX) NULL,
        tipo_usuario NVARCHAR(20) NOT NULL,
        ativo BIT NOT NULL CONSTRAINT DF_Usuario_ativo DEFAULT 1,
        mfa_habilitado BIT NOT NULL CONSTRAINT DF_Usuario_mfa_habilitado DEFAULT 0,
        CONSTRAINT PK_Usuario PRIMARY KEY (id),
        CONSTRAINT UQ_Usuario_email UNIQUE (email)
    );
END;

IF OBJECT_ID(N'dbo.Trilha', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Trilha (
        id BIGINT IDENTITY(1,1) NOT NULL,
        nome NVARCHAR(100) NOT NULL,
        descricao NVARCHAR(255) NULL,
        tipo NVARCHAR(20) NULL,
        codigo_acesso NVARCHAR(16) NULL,
        nivel NVARCHAR(50) NULL,
        disciplina NVARCHAR(50) NULL,
        professor_id BIGINT NOT NULL,
        professor_nome NVARCHAR(100) NOT NULL,
        criada_em DATETIME2 NOT NULL CONSTRAINT DF_Trilha_criada_em DEFAULT GETDATE(),
        atualizada_em DATETIME2 NOT NULL CONSTRAINT DF_Trilha_atualizada_em DEFAULT GETDATE(),
        CONSTRAINT PK_Trilha PRIMARY KEY (id),
        CONSTRAINT FK_Trilha_Usuario FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id)
    );
END;

IF OBJECT_ID(N'dbo.Aula', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Aula (
        id BIGINT IDENTITY(1,1) NOT NULL,
        titulo NVARCHAR(100) NOT NULL,
        tipo NVARCHAR(20) NULL,
        conteudo NVARCHAR(MAX) NULL,
        trilha_id BIGINT NOT NULL,
        ordem INT NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT DF_Aula_status DEFAULT 'PUBLICADA',
        criada_em DATETIME2 NOT NULL CONSTRAINT DF_Aula_criada_em DEFAULT GETDATE(),
        atualizada_em DATETIME2 NOT NULL CONSTRAINT DF_Aula_atualizada_em DEFAULT GETDATE(),
        CONSTRAINT PK_Aula PRIMARY KEY (id),
        CONSTRAINT FK_Aula_Trilha FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id) ON DELETE CASCADE
    );
END;

IF OBJECT_ID(N'dbo.Turma', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Turma (
        id BIGINT IDENTITY(1,1) NOT NULL,
        nome NVARCHAR(100) NOT NULL,
        descricao NVARCHAR(255) NULL,
        codigo NVARCHAR(50) NOT NULL,
        tipo NVARCHAR(20) NULL,
        nivel NVARCHAR(50) NULL,
        professor_id BIGINT NOT NULL,
        professor_nome NVARCHAR(100) NOT NULL,
        criada_em DATETIME2 NOT NULL CONSTRAINT DF_Turma_criada_em DEFAULT GETDATE(),
        atualizada_em DATETIME2 NOT NULL CONSTRAINT DF_Turma_atualizada_em DEFAULT GETDATE(),
        CONSTRAINT PK_Turma PRIMARY KEY (id),
        CONSTRAINT UQ_Turma_codigo UNIQUE (codigo),
        CONSTRAINT FK_Turma_Usuario FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id)
    );
END;

IF OBJECT_ID(N'dbo.MatriculaTrilha', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MatriculaTrilha (
        id BIGINT IDENTITY(1,1) NOT NULL,
        aluno_id BIGINT NOT NULL,
        trilha_id BIGINT NOT NULL,
        data_matricula DATETIME2 NOT NULL CONSTRAINT DF_MatriculaTrilha_data_matricula DEFAULT GETDATE(),
        ativo BIT NOT NULL CONSTRAINT DF_MatriculaTrilha_ativo DEFAULT 1,
        CONSTRAINT PK_MatriculaTrilha PRIMARY KEY (id),
        CONSTRAINT UQ_Matricula_Aluno_Trilha UNIQUE (aluno_id, trilha_id),
        CONSTRAINT FK_Matricula_Aluno FOREIGN KEY (aluno_id) REFERENCES dbo.Usuario(id),
        CONSTRAINT FK_Matricula_Trilha FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id) ON DELETE CASCADE
    );
END;

IF OBJECT_ID(N'dbo.PerfilAprendizado', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PerfilAprendizado (
        id BIGINT IDENTITY(1,1) NOT NULL,
        aluno_id BIGINT NOT NULL,
        objetivo NVARCHAR(30) NULL,
        nivel NVARCHAR(30) NULL,
        horas_semana INT NULL,
        meta_semanal INT NULL,
        ritmo NVARCHAR(20) NULL,
        interesses NVARCHAR(500) NULL,
        dificuldades NVARCHAR(500) NULL,
        CONSTRAINT PK_PerfilAprendizado PRIMARY KEY (id),
        CONSTRAINT UQ_PerfilAprendizado_aluno_id UNIQUE (aluno_id),
        CONSTRAINT FK_Perfil_Aluno FOREIGN KEY (aluno_id) REFERENCES dbo.Usuario(id) ON DELETE CASCADE
    );
END;

IF OBJECT_ID(N'dbo.ProgressoAula', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProgressoAula (
        id BIGINT IDENTITY(1,1) NOT NULL,
        aluno_id BIGINT NOT NULL,
        aula_id BIGINT NOT NULL,
        concluida BIT NOT NULL CONSTRAINT DF_ProgressoAula_concluida DEFAULT 0,
        concluida_em DATETIME2 NULL,
        CONSTRAINT PK_ProgressoAula PRIMARY KEY (id),
        CONSTRAINT UQ_Progresso_Aluno_Aula UNIQUE (aluno_id, aula_id),
        CONSTRAINT FK_Progresso_Aluno FOREIGN KEY (aluno_id) REFERENCES dbo.Usuario(id),
        CONSTRAINT FK_Progresso_Aula FOREIGN KEY (aula_id) REFERENCES dbo.Aula(id) ON DELETE CASCADE
    );
END;

IF OBJECT_ID(N'dbo.Duvida', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Duvida (
        id BIGINT IDENTITY(1,1) NOT NULL,
        aluno_id BIGINT NOT NULL,
        aula_id BIGINT NOT NULL,
        trilha_id BIGINT NOT NULL,
        mensagem NVARCHAR(1000) NOT NULL,
        resposta NVARCHAR(1000) NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT DF_Duvida_status DEFAULT 'PENDENTE',
        criada_em DATETIME2 NOT NULL CONSTRAINT DF_Duvida_criada_em DEFAULT GETDATE(),
        respondida_em DATETIME2 NULL,
        CONSTRAINT PK_Duvida PRIMARY KEY (id),
        CONSTRAINT FK_Duvida_Aluno FOREIGN KEY (aluno_id) REFERENCES dbo.Usuario(id),
        CONSTRAINT FK_Duvida_Aula FOREIGN KEY (aula_id) REFERENCES dbo.Aula(id) ON DELETE CASCADE,
        CONSTRAINT FK_Duvida_Trilha FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id)
    );
END;

IF OBJECT_ID(N'dbo.ticket', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ticket (
        id BIGINT IDENTITY(1,1) NOT NULL,
        usuario_id BIGINT NULL,
        nome NVARCHAR(150) NULL,
        email NVARCHAR(150) NULL,
        tipo NVARCHAR(100) NOT NULL,
        mensagem NVARCHAR(2000) NOT NULL,
        resposta NVARCHAR(2000) NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT DF_ticket_status DEFAULT 'ABERTO',
        protocolo NVARCHAR(20) NULL,
        email_enviado BIT NULL,
        criada_em DATETIME2 NOT NULL CONSTRAINT DF_ticket_criada_em DEFAULT GETDATE(),
        respondida_em DATETIME2 NULL,
        CONSTRAINT PK_ticket PRIMARY KEY (id)
    );
END;

IF OBJECT_ID(N'dbo.email_verification_token', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.email_verification_token (
        id BIGINT IDENTITY(1,1) NOT NULL,
        email NVARCHAR(255) NOT NULL,
        code NVARCHAR(6) NOT NULL,
        expires_at DATETIMEOFFSET NOT NULL,
        verified BIT NOT NULL CONSTRAINT DF_email_verification_token_verified DEFAULT 0,
        CONSTRAINT PK_email_verification_token PRIMARY KEY (id),
        CONSTRAINT UQ_email_verification_token_email UNIQUE (email)
    );
END;

IF OBJECT_ID(N'dbo.password_reset_token', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.password_reset_token (
        id BIGINT IDENTITY(1,1) NOT NULL,
        token NVARCHAR(255) NOT NULL,
        email NVARCHAR(255) NOT NULL,
        expires_at DATETIMEOFFSET NOT NULL,
        used BIT NOT NULL CONSTRAINT DF_password_reset_token_used DEFAULT 0,
        CONSTRAINT PK_password_reset_token PRIMARY KEY (id),
        CONSTRAINT UQ_password_reset_token_token UNIQUE (token)
    );
END;

IF OBJECT_ID(N'dbo.EmailChangeToken', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.EmailChangeToken (
        id BIGINT IDENTITY(1,1) NOT NULL,
        usuario_id BIGINT NOT NULL,
        email_atual NVARCHAR(255) NOT NULL,
        email_novo NVARCHAR(255) NOT NULL,
        confirm_token NVARCHAR(255) NOT NULL,
        otp_code NVARCHAR(6) NULL,
        etapa NVARCHAR(10) NOT NULL CONSTRAINT DF_EmailChangeToken_etapa DEFAULT 'STEP1',
        expires_at DATETIMEOFFSET NOT NULL,
        CONSTRAINT PK_EmailChangeToken PRIMARY KEY (id),
        CONSTRAINT UQ_EmailChangeToken_confirm_token UNIQUE (confirm_token)
    );
END;

IF OBJECT_ID(N'dbo.solicitacao_professor', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.solicitacao_professor (
        id BIGINT IDENTITY(1,1) NOT NULL,
        usuario_id BIGINT NOT NULL,
        tipo_comprovante NVARCHAR(30) NOT NULL,
        comprovante_url NVARCHAR(2000) NOT NULL,
        status NVARCHAR(20) NOT NULL,
        data_solicitacao DATETIME2 NOT NULL,
        data_analise DATETIME2 NULL,
        motivo_reprovacao NVARCHAR(1000) NULL,
        CONSTRAINT PK_solicitacao_professor PRIMARY KEY (id),
        CONSTRAINT FK_solicitacao_professor_Usuario FOREIGN KEY (usuario_id) REFERENCES dbo.Usuario(id)
    );
END;

IF OBJECT_ID(N'dbo.mfa_code', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.mfa_code (
        id BIGINT IDENTITY(1,1) NOT NULL,
        usuario_id BIGINT NOT NULL,
        purpose NVARCHAR(20) NOT NULL,
        code_hash NVARCHAR(255) NOT NULL,
        expires_at DATETIMEOFFSET NOT NULL,
        used BIT NOT NULL CONSTRAINT DF_mfa_code_used DEFAULT 0,
        tentativas INT NOT NULL CONSTRAINT DF_mfa_code_tentativas DEFAULT 0,
        CONSTRAINT PK_mfa_code PRIMARY KEY (id),
        CONSTRAINT UQ_mfa_code_usuario_purpose UNIQUE (usuario_id, purpose),
        CONSTRAINT FK_mfa_code_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.Usuario(id)
    );
END;

-- 2. Migrações para bancos que ja tinham as tabelas sem as colunas atuais.
IF COL_LENGTH(N'dbo.Usuario', N'mfa_habilitado') IS NULL
    ALTER TABLE dbo.Usuario ADD mfa_habilitado BIT NOT NULL
        CONSTRAINT DF_Usuario_mfa_habilitado DEFAULT 0;

IF COL_LENGTH(N'dbo.Trilha', N'codigo_acesso') IS NULL
    ALTER TABLE dbo.Trilha ADD codigo_acesso NVARCHAR(16) NULL;

IF COL_LENGTH(N'dbo.ticket', N'protocolo') IS NULL
    ALTER TABLE dbo.ticket ADD protocolo NVARCHAR(20) NULL;

IF COL_LENGTH(N'dbo.ticket', N'email_enviado') IS NULL
    ALTER TABLE dbo.ticket ADD email_enviado BIT NULL;

-- A tabela mfa_code antiga usava uma linha por usuario. A versao atual
-- permite uma linha para cada finalidade (LOGIN / DELETE_ACCOUNT).
IF COL_LENGTH(N'dbo.mfa_code', N'purpose') IS NULL
    ALTER TABLE dbo.mfa_code ADD purpose NVARCHAR(20) NULL;

UPDATE dbo.mfa_code SET purpose = N'LOGIN' WHERE purpose IS NULL;

IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.mfa_code')
      AND name = N'purpose'
      AND is_nullable = 1
)
    ALTER TABLE dbo.mfa_code ALTER COLUMN purpose NVARCHAR(20) NOT NULL;

-- Remove a constraint unica antiga de usuario_id, caso exista.
DECLARE @mfa_unique_constraint SYSNAME;
SELECT TOP (1) @mfa_unique_constraint = kc.name
FROM sys.key_constraints AS kc
JOIN sys.index_columns AS ic
    ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
JOIN sys.columns AS c
    ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE kc.parent_object_id = OBJECT_ID(N'dbo.mfa_code')
  AND kc.type = N'UQ'
  AND kc.name <> N'UQ_mfa_code_usuario_purpose'
GROUP BY kc.name
HAVING COUNT(*) = 1 AND MAX(c.name) = N'usuario_id';

IF @mfa_unique_constraint IS NOT NULL
BEGIN
    DECLARE @drop_mfa_constraint_sql NVARCHAR(400);
    SET @drop_mfa_constraint_sql = N'ALTER TABLE dbo.mfa_code DROP CONSTRAINT '
        + QUOTENAME(@mfa_unique_constraint);
    EXEC sp_executesql @drop_mfa_constraint_sql;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.mfa_code')
      AND name = N'UQ_mfa_code_usuario_purpose'
)
    ALTER TABLE dbo.mfa_code ADD CONSTRAINT UQ_mfa_code_usuario_purpose UNIQUE (usuario_id, purpose);

-- 3. Indices usados pelo backend.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Trilha') AND name = N'idx_trilha_professor_id')
    CREATE INDEX idx_trilha_professor_id ON dbo.Trilha(professor_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Trilha') AND name = N'idx_trilha_disciplina')
    CREATE INDEX idx_trilha_disciplina ON dbo.Trilha(disciplina);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Trilha') AND name = N'idx_trilha_codigo_acesso')
    CREATE INDEX idx_trilha_codigo_acesso ON dbo.Trilha(codigo_acesso) WHERE codigo_acesso IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Aula') AND name = N'idx_aula_trilha_id')
    CREATE INDEX idx_aula_trilha_id ON dbo.Aula(trilha_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Aula') AND name = N'idx_aula_ordem')
    CREATE INDEX idx_aula_ordem ON dbo.Aula(trilha_id, ordem);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Turma') AND name = N'idx_turma_codigo')
    CREATE INDEX idx_turma_codigo ON dbo.Turma(codigo);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Turma') AND name = N'idx_turma_professor')
    CREATE INDEX idx_turma_professor ON dbo.Turma(professor_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.MatriculaTrilha') AND name = N'idx_matricula_aluno')
    CREATE INDEX idx_matricula_aluno ON dbo.MatriculaTrilha(aluno_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.MatriculaTrilha') AND name = N'idx_matricula_trilha')
    CREATE INDEX idx_matricula_trilha ON dbo.MatriculaTrilha(trilha_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.PerfilAprendizado') AND name = N'idx_perfil_aluno')
    CREATE INDEX idx_perfil_aluno ON dbo.PerfilAprendizado(aluno_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.ProgressoAula') AND name = N'idx_progresso_aluno')
    CREATE INDEX idx_progresso_aluno ON dbo.ProgressoAula(aluno_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.ProgressoAula') AND name = N'idx_progresso_aula')
    CREATE INDEX idx_progresso_aula ON dbo.ProgressoAula(aula_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Duvida') AND name = N'idx_duvida_aluno')
    CREATE INDEX idx_duvida_aluno ON dbo.Duvida(aluno_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Duvida') AND name = N'idx_duvida_aula')
    CREATE INDEX idx_duvida_aula ON dbo.Duvida(aula_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Duvida') AND name = N'idx_duvida_trilha')
    CREATE INDEX idx_duvida_trilha ON dbo.Duvida(trilha_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.mfa_code') AND name = N'idx_mfa_code_usuario_id')
    CREATE INDEX idx_mfa_code_usuario_id ON dbo.mfa_code(usuario_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.ticket') AND name = N'UQ_ticket_protocolo')
    CREATE UNIQUE INDEX UQ_ticket_protocolo ON dbo.ticket(protocolo) WHERE protocolo IS NOT NULL;

COMMIT TRANSACTION;

-- Verificacao: deve retornar as 14 tabelas do schema atual.
SELECT s.name AS [schema], t.name AS tabela
FROM sys.tables AS t
JOIN sys.schemas AS s ON s.schema_id = t.schema_id
WHERE s.name = N'dbo'
  AND t.name IN (
      N'Aula', N'Duvida', N'email_verification_token', N'EmailChangeToken',
      N'MatriculaTrilha', N'mfa_code', N'password_reset_token',
      N'PerfilAprendizado', N'ProgressoAula', N'solicitacao_professor',
      N'ticket', N'Trilha', N'Turma', N'Usuario'
  )
ORDER BY t.name;
