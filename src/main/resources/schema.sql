-- Schema SQL for Trilhas and Aulas (SQL Server)

-- Create Trilha table
CREATE TABLE Trilha (
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

-- Create Aula table
CREATE TABLE Aula (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    conteudo TEXT,
    trilha_id BIGINT NOT NULL,
    ordem INT,
    criada_em DATETIME NOT NULL DEFAULT GETDATE(),
    atualizada_em DATETIME NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (trilha_id) REFERENCES Trilha(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_trilha_professor_id ON Trilha(professor_id);
CREATE INDEX idx_trilha_tipo ON Trilha(tipo);
CREATE INDEX idx_aula_trilha_id ON Aula(trilha_id);
CREATE INDEX idx_aula_ordem ON Aula(trilha_id, ordem);