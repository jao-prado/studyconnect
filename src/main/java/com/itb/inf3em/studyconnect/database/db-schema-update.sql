-- ============================================================
-- StudyConnect — Schema completo + migrations idempotentes
-- SQL Server / Azure SQL  |  seguro re-executar a qualquer hora
-- ============================================================

-- ── 1. Usuario ───────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Usuario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Usuario (
        id           BIGINT IDENTITY(1,1) PRIMARY KEY,
        nome         NVARCHAR(45)  NOT NULL,
        email        NVARCHAR(45)  NOT NULL UNIQUE,
        senha        NVARCHAR(255) NOT NULL,
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
        tipo           NVARCHAR(20),
        nivel          NVARCHAR(50),
        disciplina     NVARCHAR(50),
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
        tipo          NVARCHAR(20)  NULL,
        conteudo      NVARCHAR(MAX),
        trilha_id     BIGINT        NOT NULL,
        ordem         INT,
        status        NVARCHAR(20)  NOT NULL CONSTRAINT DF_Aula_status DEFAULT 'PUBLICADA',
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

-- ── 7. Curso (legado) ────────────────────────────────────────
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

-- ── 8. MatriculaTrilha ───────────────────────────────────────
IF OBJECT_ID(N'dbo.MatriculaTrilha', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MatriculaTrilha (
        id             BIGINT IDENTITY(1,1) PRIMARY KEY,
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
END

-- ── 9. ProgressoAula ─────────────────────────────────────────
IF OBJECT_ID(N'dbo.ProgressoAula', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProgressoAula (
        id           BIGINT IDENTITY(1,1) PRIMARY KEY,
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

-- ── 10. PerfilAprendizado ────────────────────────────────────
IF OBJECT_ID(N'dbo.PerfilAprendizado', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PerfilAprendizado (
        id           BIGINT IDENTITY(1,1) PRIMARY KEY,
        aluno_id     BIGINT        NOT NULL UNIQUE,
        objetivo     NVARCHAR(30),
        nivel        NVARCHAR(30),
        horas_semana INT,
        meta_semanal INT,
        ritmo        NVARCHAR(20),
        interesses   NVARCHAR(500),
        dificuldades NVARCHAR(500),
        CONSTRAINT FK_Perfil_Aluno
            FOREIGN KEY (aluno_id) REFERENCES dbo.Usuario(id) ON DELETE CASCADE
    );
    CREATE INDEX idx_perfil_aluno ON dbo.PerfilAprendizado(aluno_id);
    PRINT 'OK: PerfilAprendizado criada';
END

-- ============================================================
-- MIGRATIONS — idempotentes, seguro re-executar
-- ============================================================

-- 1. Expandir senha para bcrypt
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Usuario') AND name = 'senha' AND max_length < 255
)
BEGIN
    ALTER TABLE dbo.Usuario ALTER COLUMN senha NVARCHAR(255) NOT NULL;
    PRINT 'OK: Usuario.senha expandida para NVARCHAR(255)';
END

-- 2. Adicionar google_id na Usuario
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Usuario') AND name = 'google_id'
)
BEGIN
    ALTER TABLE dbo.Usuario ADD google_id NVARCHAR(255) NULL;
    PRINT 'OK: Usuario.google_id adicionada';
END

-- 3. Adicionar foto_url na Usuario
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Usuario') AND name = 'foto_url'
)
BEGIN
    ALTER TABLE dbo.Usuario ADD foto_url NVARCHAR(MAX) NULL;
    PRINT 'OK: Usuario.foto_url adicionada';
END
ELSE IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Usuario') AND name = 'foto_url' AND max_length <> -1
)
BEGIN
    ALTER TABLE dbo.Usuario ALTER COLUMN foto_url NVARCHAR(MAX) NULL;
    PRINT 'OK: Usuario.foto_url expandida para NVARCHAR(MAX)';
END

-- 4. Tornar senha nullable (usuarios Google nao tem senha local)
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Usuario') AND name = 'senha' AND is_nullable = 0
)
BEGIN
    ALTER TABLE dbo.Usuario ALTER COLUMN senha NVARCHAR(255) NULL;
    PRINT 'OK: Usuario.senha agora aceita NULL';
END

-- 5. Adicionar disciplina na Trilha
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Trilha') AND name = 'disciplina'
)
BEGIN
    ALTER TABLE dbo.Trilha ADD disciplina NVARCHAR(50) NULL;
    PRINT 'OK: Trilha.disciplina adicionada';
END

-- 3. FK Trilha → Usuario
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Trilha_Usuario')
BEGIN
    ALTER TABLE dbo.Trilha ADD CONSTRAINT FK_Trilha_Usuario
        FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id);
    PRINT 'OK: FK_Trilha_Usuario criada';
END

-- 4. FK Turma → Usuario
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Turma_Usuario')
BEGIN
    ALTER TABLE dbo.Turma ADD CONSTRAINT FK_Turma_Usuario
        FOREIGN KEY (professor_id) REFERENCES dbo.Usuario(id);
    PRINT 'OK: FK_Turma_Usuario criada';
END

-- 5. Aula.tipo aceitar NULL
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Aula') AND name = 'tipo' AND is_nullable = 0
)
BEGIN
    ALTER TABLE dbo.Aula ALTER COLUMN tipo NVARCHAR(20) NULL;
    PRINT 'OK: Aula.tipo agora aceita NULL';
END

-- 6. Aula.status (PUBLICADA | RASCUNHO)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Aula') AND name = 'status'
)
BEGIN
    ALTER TABLE dbo.Aula ADD status NVARCHAR(20) NOT NULL CONSTRAINT DF_Aula_status DEFAULT 'PUBLICADA';
    PRINT 'OK: Aula.status adicionada';
END
ELSE
BEGIN
    UPDATE dbo.Aula SET status = 'PUBLICADA' WHERE status IS NULL;
    IF NOT EXISTS (
        SELECT 1 FROM sys.default_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.Aula') AND name = 'DF_Aula_status'
    )
    BEGIN
        ALTER TABLE dbo.Aula ADD CONSTRAINT DF_Aula_status DEFAULT 'PUBLICADA' FOR status;
        PRINT 'OK: DF_Aula_status constraint adicionada';
    END
END

-- 7. Migrar Material: curso_id → trilha_id
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Material_Curso')
BEGIN
    ALTER TABLE dbo.Material DROP CONSTRAINT FK_Material_Curso;
    PRINT 'OK: FK_Material_Curso removida';
END

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Material') AND name = 'trilha_id'
)
BEGIN
    ALTER TABLE dbo.Material ADD trilha_id BIGINT NULL;
    PRINT 'OK: Material.trilha_id adicionada';
END

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Material_Trilha')
BEGIN
    ALTER TABLE dbo.Material ADD CONSTRAINT FK_Material_Trilha
        FOREIGN KEY (trilha_id) REFERENCES dbo.Trilha(id);
    PRINT 'OK: FK_Material_Trilha criada';
END

-- 8. MatriculaTrilha — adicionar se tabela já existia sem a constraint UNIQUE
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UQ_Matricula_Aluno_Trilha' AND object_id = OBJECT_ID(N'dbo.MatriculaTrilha')
)
BEGIN
    ALTER TABLE dbo.MatriculaTrilha
    ADD CONSTRAINT UQ_Matricula_Aluno_Trilha UNIQUE (aluno_id, trilha_id);
    PRINT 'OK: UQ_Matricula_Aluno_Trilha criada';
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

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_matricula_aluno' AND object_id = OBJECT_ID(N'dbo.MatriculaTrilha'))
    CREATE INDEX idx_matricula_aluno ON dbo.MatriculaTrilha(aluno_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_matricula_trilha' AND object_id = OBJECT_ID(N'dbo.MatriculaTrilha'))
    CREATE INDEX idx_matricula_trilha ON dbo.MatriculaTrilha(trilha_id);

-- ============================================================
-- TESTES — verificação rápida de todas as tabelas
-- ============================================================

SELECT 'Usuario'         AS tabela, COUNT(*) AS registros FROM dbo.Usuario;
SELECT 'Trilha'          AS tabela, COUNT(*) AS registros FROM dbo.Trilha;
SELECT 'Aula'            AS tabela, COUNT(*) AS registros FROM dbo.Aula;
SELECT 'MatriculaTrilha' AS tabela, COUNT(*) AS registros FROM dbo.MatriculaTrilha;
SELECT 'Certificado'     AS tabela, COUNT(*) AS registros FROM dbo.Certificado;
SELECT 'Turma'           AS tabela, COUNT(*) AS registros FROM dbo.Turma;
SELECT 'Material'        AS tabela, COUNT(*) AS registros FROM dbo.Material;
SELECT 'ProgressoAula'   AS tabela, COUNT(*) AS registros FROM dbo.ProgressoAula;

-- ── 11. Duvida ───────────────────────────────────────────────
IF OBJECT_ID(N'dbo.Duvida', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Duvida (
        id            BIGINT IDENTITY(1,1) PRIMARY KEY,
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
    CREATE INDEX idx_duvida_trilha ON dbo.Duvida(trilha_id);
    CREATE INDEX idx_duvida_aula   ON dbo.Duvida(aula_id);
    CREATE INDEX idx_duvida_aluno  ON dbo.Duvida(aluno_id);
    PRINT 'OK: Duvida criada';
END

-- Dados completos
SELECT * FROM dbo.Usuario;
SELECT * FROM dbo.Trilha;
SELECT * FROM dbo.Aula;
SELECT * FROM dbo.MatriculaTrilha;
SELECT * FROM dbo.Certificado;
SELECT * FROM dbo.Turma;
SELECT * FROM dbo.Material;
SELECT * FROM dbo.ProgressoAula;

-- Matrículas com nome do aluno e da trilha (join útil para debug)
SELECT
    m.id,
    u.nome  AS aluno,
    t.nome  AS trilha,
    t.disciplina,
    m.data_matricula,
    m.ativo
FROM dbo.MatriculaTrilha m
JOIN dbo.Usuario u ON u.id = m.aluno_id
JOIN dbo.Trilha  t ON t.id = m.trilha_id
ORDER BY m.data_matricula DESC;

-- teste