-- =============================================================================
-- Ticket — Migração: protocolo único e flag email_enviado
-- SQL Server / Azure SQL
-- Executar uma única vez no banco de produção/desenvolvimento
-- =============================================================================

-- 1. Adicionar coluna protocolo (gerado pelo backend no momento da criação)
ALTER TABLE dbo.ticket
    ADD protocolo NVARCHAR(20) NULL;

-- 2. Índice único para protocolo (ignora NULLs de registros antigos)
CREATE UNIQUE INDEX UQ_ticket_protocolo ON dbo.ticket(protocolo)
    WHERE protocolo IS NOT NULL;

-- 3. Adicionar flag de confirmação de envio de e-mail
--    NULL = não tentou ainda, 1 = enviado, 0 = falhou
ALTER TABLE dbo.ticket
    ADD email_enviado BIT NULL;

-- =============================================================================
-- Observações:
--
-- • Tickets existentes ficam com protocolo = NULL e email_enviado = NULL.
--   Isso é seguro — o frontend exibe "—" quando protocolo é nulo.
--
-- • O protocolo é gerado no backend com formato SC-YYYYMMDD-XXXXXXXX
--   (ano+mês+dia + 8 hex aleatórios), garantindo unicidade sem sequência.
--
-- • email_enviado = NULL indica ticket antigo (sem tentativa registrada).
--   email_enviado = 1 = e-mail enviado com sucesso.
--   email_enviado = 0 = falha no envio (ticket salvo, e-mail não chegou).
--
-- • Nenhum dado existente é removido ou alterado por este script.
-- =============================================================================
