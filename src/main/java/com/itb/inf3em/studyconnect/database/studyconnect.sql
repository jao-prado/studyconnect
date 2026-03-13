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