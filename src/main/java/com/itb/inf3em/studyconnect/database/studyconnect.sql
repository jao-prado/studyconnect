CREATE TABLE Usuario (
    id BIGINT PRIMARY KEY IDENTITY,
    nome VARCHAR(100),
    email VARCHAR(100),
    senha VARCHAR(100),
    ativo BIT
);

CREATE TABLE Curso (
    id BIGINT PRIMARY KEY IDENTITY,
    nome VARCHAR(100),
    descricao VARCHAR(255)
);

CREATE TABLE Material (
    id BIGINT PRIMARY KEY IDENTITY,
    nome VARCHAR(100),
    tipo VARCHAR(50),
    curso_id BIGINT,
    FOREIGN KEY (curso_id) REFERENCES Curso(id)
);

CREATE TABLE Certificado (
    id BIGINT PRIMARY KEY IDENTITY,
    usuario_id BIGINT,
    curso_id BIGINT,
    data_emissao DATE,
    FOREIGN KEY (usuario_id) REFERENCES Usuario(id),
    FOREIGN KEY (curso_id) REFERENCES Curso(id)
);

-- ===== TURMA (CLASS) TABLE =====
CREATE TABLE Turma (
    id BIGINT PRIMARY KEY IDENTITY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    codigo VARCHAR(50) NOT NULL UNIQUE,
    tipo VARCHAR(20),                          -- PUBLICA, PRIVADA
    nivel VARCHAR(50),                          -- Fundamental, Médio, Superior, etc.
    professor_id BIGINT NOT NULL,
    professor_nome VARCHAR(100),
    criada_em DATETIME NOT NULL DEFAULT GETDATE(),
    atualizada_em DATETIME NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (professor_id) REFERENCES Usuario(id) ON DELETE CASCADE
);

-- Index on codigo for faster lookups when joining by code
CREATE INDEX idx_turma_codigo ON Turma(codigo);
-- Index on professor_id for finding classes by teacher
CREATE INDEX idx_turma_professor ON Turma(professor_id);