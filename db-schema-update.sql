-- Atualização do schema para o backend atual
-- Execute este script no seu banco SQL Server para garantir as tabelas esperadas.

IF OBJECT_ID(N'dbo.Usuario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Usuario (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome VARCHAR(100),
        email VARCHAR(100),
        senha VARCHAR(100),
        tipo_usuario VARCHAR(20),
        ativo BIT
    );
END

IF OBJECT_ID(N'dbo.Curso', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Curso (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome VARCHAR(100),
        descricao VARCHAR(255),
        duracao VARCHAR(40),
        nivel VARCHAR(20),
        ativo BIT,
        professor_id BIGINT,
        CONSTRAINT FK_Curso_Usuario FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id)
    );
END

IF OBJECT_ID(N'dbo.Material', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Material (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        titulo VARCHAR(60),
        descricao VARCHAR(100),
        categoria VARCHAR(20),
        ativo BIT,
        curso_id BIGINT,
        CONSTRAINT FK_Material_Curso FOREIGN KEY (curso_id) REFERENCES dbo.Curso(id)
    );
END

IF OBJECT_ID(N'dbo.Certificado', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Certificado (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome VARCHAR(45) NOT NULL,
        descricao VARCHAR(100),
        nivel VARCHAR(20) NOT NULL,
        usuario_id BIGINT NOT NULL,
        data_emissao DATETIME2 NOT NULL,
        ativo BIT NOT NULL,
        CONSTRAINT FK_Certificado_Usuario FOREIGN KEY (usuario_id) REFERENCES dbo.Usuario(id)
    );
END

IF OBJECT_ID(N'dbo.Turma', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Turma (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome VARCHAR(100) NOT NULL,
        descricao VARCHAR(255),
        codigo VARCHAR(50) NOT NULL UNIQUE,
        tipo VARCHAR(20),
        nivel VARCHAR(50),
        professor_id BIGINT NOT NULL,
        professor_nome VARCHAR(100),
        criada_em DATETIME NOT NULL DEFAULT GETDATE(),
        atualizada_em DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_Turma_Usuario FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id) ON DELETE CASCADE
    );
END

IF OBJECT_ID(N'dbo.Trilha', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Trilha (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome VARCHAR(100) NOT NULL,
        descricao VARCHAR(255),
        tipo VARCHAR(20),
        nivel VARCHAR(50),
        professor_id BIGINT NOT NULL,
        professor_nome VARCHAR(100) NOT NULL,
        criada_em DATETIME NOT NULL DEFAULT GETDATE(),
        atualizada_em DATETIME NOT NULL DEFAULT GETDATE()
    );
END

IF OBJECT_ID(N'dbo.Aula', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Aula (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        titulo VARCHAR(100) NOT NULL,
        tipo VARCHAR(20) NOT NULL,
        conteudo TEXT,
        trilha_id BIGINT NOT NULL,
        ordem INT,
        criada_em DATETIME NOT NULL DEFAULT GETDATE(),
        atualizada_em DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_Aula_Trilha FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id) ON DELETE CASCADE
    );
END

-- Índices
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_turma_codigo' AND object_id = OBJECT_ID(N'dbo.Turma'))
BEGIN
    CREATE INDEX idx_turma_codigo ON dbo.Turma(codigo);
END

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_turma_professor' AND object_id = OBJECT_ID(N'dbo.Turma'))
BEGIN
    CREATE INDEX idx_turma_professor ON dbo.Turma(professor_id);
END

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_trilha_professor_id' AND object_id = OBJECT_ID(N'dbo.Trilha'))
BEGIN
    CREATE INDEX idx_trilha_professor_id ON dbo.Trilha(professor_id);
END

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_aula_trilha_id' AND object_id = OBJECT_ID(N'dbo.Aula'))
BEGIN
    CREATE INDEX idx_aula_trilha_id ON dbo.Aula(trilha_id);
END

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_aula_ordem' AND object_id = OBJECT_ID(N'dbo.Aula'))
BEGIN
    CREATE INDEX idx_aula_ordem ON dbo.Aula(trilha_id, ordem);
END
