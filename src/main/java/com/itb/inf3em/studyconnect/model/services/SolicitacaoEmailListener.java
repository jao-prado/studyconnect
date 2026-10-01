package com.itb.inf3em.studyconnect.model.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SolicitacaoEmailListener {

    private static final Logger log = LoggerFactory.getLogger(SolicitacaoEmailListener.class);

    private final EmailService emailService;

    public SolicitacaoEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Executado SOMENTE após o commit da transação.
     * Se o e-mail falhar, a aprovação/reprovação já está persistida — não é desfeita.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSolicitacaoAnalisada(SolicitacaoAnalisadaEvent event) {
        try {
            if (event.aprovado()) {
                String assunto = "Sua solicitação de professor foi aprovada — StudyConnect";
                String corpo = "Olá, " + event.nomeUsuario() + "!\n\n"
                        + "Sua solicitação de professor foi aprovada.\n"
                        + "Sua conta foi promovida para PROFESSOR. Faça login novamente para acessar o painel do professor.\n\n"
                        + "Equipe StudyConnect";
                emailService.sendSimpleEmail(event.emailDestinatario(), assunto, corpo);
            } else {
                String assunto = "Atualização sobre sua solicitação de professor — StudyConnect";
                String corpo = "Olá, " + event.nomeUsuario() + "!\n\n"
                        + "Sua solicitação de professor foi analisada e não foi aprovada.\n\n"
                        + "Motivo: " + event.motivoReprovacao() + "\n\n"
                        + "Se tiver dúvidas, entre em contato com o suporte.\n\n"
                        + "Equipe StudyConnect";
                emailService.sendSimpleEmail(event.emailDestinatario(), assunto, corpo);
            }
        } catch (Exception ex) {
            // Falha no e-mail é apenas logada — a operação no banco já foi confirmada
            log.error("[SolicitacaoEmailListener] Falha ao enviar e-mail para {}: {}",
                    event.emailDestinatario(), ex.getMessage(), ex);
        }
    }
}
