# Deploy dos novos fluxos de autenticação

## Contratos adicionados

- `POST /api/v1/auth/register`: cria uma conta de aluno e devolve um token opaco de conclusão de cadastro. O token é válido por 30 minutos, associado ao ID recém-criado e armazenado somente como hash na tabela `mfa_code`.
- `POST /api/v1/auth/verify-email`: mantém a resposta anterior para clientes que enviam apenas `email` e `code`. O Mobile envia também `registrationToken`; nesse caso, a verificação devolve uma sessão JWT ou um desafio MFA pendente e informa `onboardingRequired`.
- `GET /api/v1/auth/account-security`: sessão obrigatória; informa apenas `hasPassword`, `hasGoogle` e `mfaEnabled` do usuário autenticado.
- `POST /api/v1/auth/account-deletion/challenge`: sessão obrigatória; recebe exatamente uma prova recente (`senha` ou `googleIdToken`). O Backend valida assinatura, emissor, audiência, sujeito da conta e `iat` do token Google (até 10 minutos). Só envia código por e-mail se MFA for exigido.
- `DELETE /api/v1/auth/account-deletion`: sessão obrigatória; repete a prova de identidade e valida o código MFA quando necessário. O usuário-alvo é sempre o principal autenticado, não um ID enviado pelo cliente.

As rotas preexistentes de registro, verificação de e-mail e exclusão permanecem disponíveis. A resposta de login Google inclui o campo aditivo `onboardingRequired`, verdadeiro somente na criação de uma nova conta Google.

## Banco de dados

Não há alteração estrutural nem migração SQL. O token temporário de cadastro reutiliza `mfa_code`, já usado pelos fluxos MFA e de exclusão. A finalidade `REGISTRATION` cabe na coluna existente `purpose` (20 caracteres). Confirme que a implantação utiliza a tabela `mfa_code`, necessária também aos fluxos MFA já existentes.

## Implantar no Render

1. Envie para o branch conectado ao serviço do Backend as alterações do diretório deste repositório.
2. No Render, abra o serviço Spring Boot e use **Manual Deploy → Deploy latest commit** caso o deploy automático não esteja habilitado.
3. Aguarde o serviço ficar **Live** e verifique os logs de inicialização. Não publique segredos nem tokens nos logs ou em chamados.
4. Confirme a disponibilidade do serviço pela rota pública de saúde já configurada, `GET /health`.
5. Em ambiente de teste, valide um cadastro novo: `POST /api/v1/auth/register`, receba o código por e-mail e envie `email`, `code` e `registrationToken` em `POST /api/v1/auth/verify-email`. A resposta deve conter um JWT ou `mfaPendente: true`; nunca use o token temporário como JWT.
6. Com uma sessão de teste, confira `GET /api/v1/auth/account-security`. Para exclusão, teste primeiro a senha incorreta, depois a prova válida e o comportamento de MFA. Não execute a exclusão de contas reais como teste.
7. Atualize o Mobile para a versão que consome esses contratos e confirme no ambiente de homologação o fluxo tradicional, o Google e a exclusão antes da publicação.

## Compatibilidade e versão implantada

O fluxo antigo de verificação por e-mail, sem `registrationToken`, continua respondendo com a mensagem de sucesso existente, preservando o consumidor Web. A exclusão Google agora exige um `googleIdToken` recente; o identificador Google armazenado, sozinho, não autoriza a operação.

O repositório local não comprova qual commit está ativo no Render. Considere os novos contratos disponíveis em produção somente depois de confirmar o commit implantado no painel e executar as verificações de homologação acima.
