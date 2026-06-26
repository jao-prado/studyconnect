-- ============================================================
-- StudyConnect — RESET COMPLETO
-- Apaga todos os dados e reinicia os IDs do zero.
-- Execute no SQL Server Management Studio ou Azure Data Studio.
-- ============================================================

-- 1. Desabilita todas as foreign keys
EXEC sp_MSforeachtable 'ALTER TABLE ? NOCHECK CONSTRAINT ALL'

-- 2. Deleta na ordem correta (filhos antes dos pais)
DELETE FROM dbo.Duvida
DELETE FROM dbo.ProgressoAula
DELETE FROM dbo.MatriculaTrilha
DELETE FROM dbo.PerfilAprendizado
DELETE FROM dbo.Certificado
DELETE FROM dbo.Material
DELETE FROM dbo.Aula
DELETE FROM dbo.Trilha
DELETE FROM dbo.Turma
DELETE FROM dbo.Curso
DELETE FROM dbo.Usuario

-- 3. Reinicia os IDs (IDENTITY) do zero
DBCC CHECKIDENT ('dbo.Duvida',           RESEED, 0)
DBCC CHECKIDENT ('dbo.ProgressoAula',    RESEED, 0)
DBCC CHECKIDENT ('dbo.MatriculaTrilha',  RESEED, 0)
DBCC CHECKIDENT ('dbo.PerfilAprendizado',RESEED, 0)
DBCC CHECKIDENT ('dbo.Certificado',      RESEED, 0)
DBCC CHECKIDENT ('dbo.Material',         RESEED, 0)
DBCC CHECKIDENT ('dbo.Aula',             RESEED, 0)
DBCC CHECKIDENT ('dbo.Trilha',           RESEED, 0)
DBCC CHECKIDENT ('dbo.Turma',            RESEED, 0)
DBCC CHECKIDENT ('dbo.Curso',            RESEED, 0)
DBCC CHECKIDENT ('dbo.Usuario',          RESEED, 0)

-- 4. Reabilita todas as foreign keys
EXEC sp_MSforeachtable 'ALTER TABLE ? WITH CHECK CHECK CONSTRAINT ALL'

-- 5. Confirmação
SELECT 'Usuario'          AS tabela, COUNT(*) AS registros FROM dbo.Usuario
UNION ALL
SELECT 'Trilha',           COUNT(*) FROM dbo.Trilha
UNION ALL
SELECT 'Aula',             COUNT(*) FROM dbo.Aula
UNION ALL
SELECT 'MatriculaTrilha',  COUNT(*) FROM dbo.MatriculaTrilha
UNION ALL
SELECT 'ProgressoAula',    COUNT(*) FROM dbo.ProgressoAula
UNION ALL
SELECT 'Duvida',           COUNT(*) FROM dbo.Duvida
UNION ALL
SELECT 'PerfilAprendizado',COUNT(*) FROM dbo.PerfilAprendizado
UNION ALL
SELECT 'Certificado',      COUNT(*) FROM dbo.Certificado
UNION ALL
SELECT 'Material',         COUNT(*) FROM dbo.Material
UNION ALL
SELECT 'Turma',            COUNT(*) FROM dbo.Turma
