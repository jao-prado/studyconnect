-- =============================================================================
-- Trilhas Privadas — Migração manual
-- SQL Server / Azure SQL
-- Executar uma única vez no banco de produção/desenvolvimento
-- =============================================================================

-- 1. Adicionar coluna codigo_acesso na tabela Trilha
--    NULL para trilhas públicas existentes; será preenchido manualmente se necessário.
ALTER TABLE dbo.Trilha
    ADD codigo_acesso NVARCHAR(16) NULL;

-- 2. Índice para busca por código (não unique: NULL é permitido para públicas)
CREATE INDEX idx_trilha_codigo_acesso ON dbo.Trilha(codigo_acesso)
    WHERE codigo_acesso IS NOT NULL;

-- =============================================================================
-- Observações:
--
-- • A tabela MatriculaTrilha já possui UNIQUE(aluno_id, trilha_id) e é reutilizada
--   como registro de acesso autorizado para trilhas privadas.
--   Quem tem matrícula ativa = tem acesso à trilha privada.
--
-- • Não é necessário criar nova tabela de acesso.
--
-- • Trilhas existentes com tipo = 'PRIVADA' que não possuem código ficam com
--   codigo_acesso = NULL. O professor deve regenerar o código via:
--   POST /api/v1/trilhas/{id}/codigo/regenerar
--
-- • Nenhum dado existente é removido ou alterado por este script.
-- =============================================================================
