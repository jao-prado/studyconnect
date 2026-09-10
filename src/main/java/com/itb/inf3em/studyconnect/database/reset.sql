-- ============================================================
-- StudyConnect — Reset local de desenvolvimento
-- ATENÇÃO: use SOMENTE em ambiente local.
-- NÃO execute no banco de produção (Somee).
-- Apaga todos os dados e reinicia os IDs do zero.
-- ============================================================

-- 1. Desabilita todas as foreign keys
EXEC sp_MSforeachtable 'ALTER TABLE ? NOCHECK CONSTRAINT ALL'

-- 2. Deleta na ordem correta (filhos antes dos pais)
DELETE FROM dbo.Duvida
DELETE FROM dbo.ProgressoAula
DELETE FROM dbo.MatriculaTrilha
DELETE FROM dbo.PerfilAprendizado
DELETE FROM dbo.Aula
DELETE FROM dbo.Trilha
DELETE FROM dbo.Turma
DELETE FROM dbo.Usuario
DELETE FROM dbo.ticket
DELETE FROM dbo.email_verification_token
DELETE FROM dbo.password_reset_token
DELETE FROM dbo.EmailChangeToken

-- 3. Reinicia os IDs (IDENTITY) do zero
DBCC CHECKIDENT ('dbo.Duvida',                  RESEED, 0)
DBCC CHECKIDENT ('dbo.ProgressoAula',           RESEED, 0)
DBCC CHECKIDENT ('dbo.MatriculaTrilha',         RESEED, 0)
DBCC CHECKIDENT ('dbo.PerfilAprendizado',       RESEED, 0)
DBCC CHECKIDENT ('dbo.Aula',                    RESEED, 0)
DBCC CHECKIDENT ('dbo.Trilha',                  RESEED, 0)
DBCC CHECKIDENT ('dbo.Turma',                   RESEED, 0)
DBCC CHECKIDENT ('dbo.Usuario',                 RESEED, 0)
DBCC CHECKIDENT ('dbo.ticket',                  RESEED, 0)
DBCC CHECKIDENT ('dbo.email_verification_token',RESEED, 0)
DBCC CHECKIDENT ('dbo.password_reset_token',    RESEED, 0)
DBCC CHECKIDENT ('dbo.EmailChangeToken',        RESEED, 0)

-- 4. Reabilita todas as foreign keys
EXEC sp_MSforeachtable 'ALTER TABLE ? WITH CHECK CHECK CONSTRAINT ALL'

-- 5. Confirmação
SELECT 'Usuario'                  AS tabela, COUNT(*) AS registros FROM dbo.Usuario
UNION ALL
SELECT 'Trilha',                             COUNT(*) FROM dbo.Trilha
UNION ALL
SELECT 'Aula',                               COUNT(*) FROM dbo.Aula
UNION ALL
SELECT 'Turma',                              COUNT(*) FROM dbo.Turma
UNION ALL
SELECT 'MatriculaTrilha',                    COUNT(*) FROM dbo.MatriculaTrilha
UNION ALL
SELECT 'ProgressoAula',                      COUNT(*) FROM dbo.ProgressoAula
UNION ALL
SELECT 'Duvida',                             COUNT(*) FROM dbo.Duvida
UNION ALL
SELECT 'PerfilAprendizado',                  COUNT(*) FROM dbo.PerfilAprendizado
UNION ALL
SELECT 'ticket',                             COUNT(*) FROM dbo.ticket
UNION ALL
SELECT 'email_verification_token',           COUNT(*) FROM dbo.email_verification_token
UNION ALL
SELECT 'password_reset_token',               COUNT(*) FROM dbo.password_reset_token
UNION ALL
SELECT 'EmailChangeToken',                   COUNT(*) FROM dbo.EmailChangeToken
