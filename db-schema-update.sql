-- ============================================================
-- STUDYCONNECT — Schema completo v2
-- SQL Server (somee.com)
-- Seguro para rodar em banco novo OU já existente.
-- ============================================================

-- ── 1. Usuario ───────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Usuario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Usuario (
        id           BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome         NVARCHAR(45)  NOT NULL,
        email        NVARCHAR(45)  NOT NULL UNIQUE,
        senha        NVARCHAR(255) NOT NULL,   -- bcrypt = 60 chars, 255 com folga
        tipo_usuario NVARCHAR(20)  NOT NULL,   -- ALUNO | PROFESSOR | ADMIN
        ativo        BIT           NOT NULL DEFAULT 1
    );
END

-- ── 2. Certificado ───────────────────────────────────────────
IF OBJECT_ID(N'dbo.Certificado', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Certificado (
        id           BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome         NVARCHAR(45)  NOT NULL,
        descricao    NVARCHAR(100),
        nivel        NVARCHAR(20)  NOT NULL,
        usuario_id   BIGINT        NOT NULL,
        data_emissao DATETIME2     NOT NULL,
        ativo        BIT           NOT NULL DEFAULT 1,
        CONSTRAINT FK_Certificado_Usuario
            FOREIGN KEY (usuario_id) REFERENCES dbo.Usuario(id)
    );
END

-- ── 3. Trilha ────────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Trilha', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Trilha (
        id             BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome           NVARCHAR(100) NOT NULL,
        descricao      NVARCHAR(255),
        tipo           NVARCHAR(20),               -- PUBLICA | PRIVADA
        nivel          NVARCHAR(50),
        disciplina     NVARCHAR(50),               -- Matemática, Português, etc.
        professor_id   BIGINT        NOT NULL,
        professor_nome NVARCHAR(100) NOT NULL,
        criada_em      DATETIME2     NOT NULL DEFAULT GETDATE(),
        atualizada_em  DATETIME2     NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_Trilha_Usuario
            FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id)
    );
END

-- ── 4. Aula ──────────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Aula', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Aula (
        id            BIGINT IDENTITY(1,1) PRIMARY KEY,
        titulo        NVARCHAR(100) NOT NULL,
        tipo          NVARCHAR(20),               -- NULL: frontend usa blocos JSON no conteudo
        conteudo      NVARCHAR(MAX),              -- armazena JSON dos blocos
        trilha_id     BIGINT        NOT NULL,
        ordem         INT,
        criada_em     DATETIME2     NOT NULL DEFAULT GETDATE(),
        atualizada_em DATETIME2     NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_Aula_Trilha
            FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id) ON DELETE CASCADE
    );
END

-- ── 5. Turma ─────────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Turma', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Turma (
        id             BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome           NVARCHAR(100) NOT NULL,
        descricao      NVARCHAR(255),
        codigo         NVARCHAR(50)  NOT NULL UNIQUE,
        tipo           NVARCHAR(20),
        nivel          NVARCHAR(50),
        professor_id   BIGINT        NOT NULL,
        professor_nome NVARCHAR(100) NOT NULL,
        criada_em      DATETIME2     NOT NULL DEFAULT GETDATE(),
        atualizada_em  DATETIME2     NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_Turma_Usuario
            FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id)
    );
END

-- ── 6. Material ──────────────────────────────────────────────
-- Migrado de curso_id → trilha_id (Curso foi removido do sistema)
IF OBJECT_ID(N'dbo.Material', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Material (
        id        BIGINT IDENTITY(1,1) PRIMARY KEY,
        titulo    NVARCHAR(60)  NOT NULL,
        descricao NVARCHAR(100) NOT NULL,
        categoria NVARCHAR(20)  NOT NULL,
        ativo     BIT           NOT NULL DEFAULT 1,
        trilha_id BIGINT,
        CONSTRAINT FK_Material_Trilha
            FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id)
    );
END

-- ── 7. Curso (legado — mantida para não perder dados existentes) ──
IF OBJECT_ID(N'dbo.Curso', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Curso (
        id           BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome         NVARCHAR(45),
        descricao    NVARCHAR(100),
        duracao      NVARCHAR(40),
        nivel        NVARCHAR(20),
        ativo        BIT DEFAULT 1,
        professor_id BIGINT
    );
END

-- ============================================================
-- MIGRATIONS — rode se as tabelas JÁ EXISTEM no banco
-- Cada bloco verifica antes de alterar. 100% seguro re-executar.
-- ============================================================

-- 1. Aumentar senha para suportar bcrypt (60 chars) com folga
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Usuario')
    AND name = 'senha'
    AND max_length < 255
)
BEGIN
    ALTER TABLE dbo.Usuario ALTER COLUMN senha NVARCHAR(255) NOT NULL;
    PRINT 'OK: Usuario.senha expandida para NVARCHAR(255)';
END

-- 2. Adicionar coluna disciplina na Trilha
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Trilha')
    AND name = 'disciplina'
)
BEGIN
    ALTER TABLE dbo.Trilha ADD disciplina NVARCHAR(50) NULL;
    PRINT 'OK: Trilha.disciplina adicionada';
END

-- 3. FK Trilha → Usuario
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'FK_Trilha_Usuario'
)
BEGIN
    ALTER TABLE dbo.Trilha
    ADD CONSTRAINT FK_Trilha_Usuario
        FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id);
    PRINT 'OK: FK_Trilha_Usuario criada';
END

-- 4. FK Turma → Usuario (sem CASCADE para não apagar turmas ao deletar professor)
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'FK_Turma_Usuario'
)
BEGIN
    ALTER TABLE dbo.Turma
    ADD CONSTRAINT FK_Turma_Usuario
        FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id);
    PRINT 'OK: FK_Turma_Usuario criada';
END

-- 5. Corrigir Aula.tipo para aceitar NULL (frontend não manda esse campo)
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Aula')
    AND name = 'tipo'
    AND is_nullable = 0
)
BEGIN
    ALTER TABLE dbo.Aula ALTER COLUMN tipo NVARCHAR(20) NULL;
    PRINT 'OK: Aula.tipo agora aceita NULL';
END

-- 6. Migrar Material: curso_id → trilha_id
--    Passo 1: remover FK antiga se existir
IF EXISTS (
    SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Material_Curso'
)
BEGIN
    ALTER TABLE dbo.Material DROP CONSTRAINT FK_Material_Curso;
    PRINT 'OK: FK_Material_Curso removida';
END

--    Passo 2: adicionar coluna trilha_id se não existir
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Material')
    AND name = 'trilha_id'
)
BEGIN
    ALTER TABLE dbo.Material ADD trilha_id BIGINT NULL;
    PRINT 'OK: Material.trilha_id adicionada';
END

--    Passo 3: adicionar FK Material → Trilha se não existir
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Material_Trilha'
)
BEGIN
    ALTER TABLE dbo.Material
    ADD CONSTRAINT FK_Material_Trilha
        FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id);
    PRINT 'OK: FK_Material_Trilha criada';
END

-- ── Índices ──────────────────────────────────────────────────
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_trilha_professor_id' AND object_id = OBJECT_ID(N'dbo.Trilha'))
    CREATE INDEX idx_trilha_professor_id ON dbo.Trilha(professor_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_trilha_disciplina' AND object_id = OBJECT_ID(N'dbo.Trilha'))
    CREATE INDEX idx_trilha_disciplina ON dbo.Trilha(disciplina);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_aula_trilha_id' AND object_id = OBJECT_ID(N'dbo.Aula'))
    CREATE INDEX idx_aula_trilha_id ON dbo.Aula(trilha_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_aula_ordem' AND object_id = OBJECT_ID(N'dbo.Aula'))
    CREATE INDEX idx_aula_ordem ON dbo.Aula(trilha_id, ordem);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_turma_codigo' AND object_id = OBJECT_ID(N'dbo.Turma'))
    CREATE INDEX idx_turma_codigo ON dbo.Turma(codigo);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_turma_professor' AND object_id = OBJECT_ID(N'dbo.Turma'))
    CREATE INDEX idx_turma_professor ON dbo.Turma(professor_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_material_trilha_id' AND object_id = OBJECT_ID(N'dbo.Material'))
    CREATE INDEX idx_material_trilha_id ON dbo.Material(trilha_id);
