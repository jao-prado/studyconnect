package com.itb.inf3em.studyconnect.model.services;

/**
 * Evento publicado após a transação de aprovação/reprovação ser confirmada no banco.
 * O listener usa @TransactionalEventListener(phase = AFTER_COMMIT) para garantir
 * que o e-mail só é enviado depois que a alteração está persistida.
 */
public record SolicitacaoAnalisadaEvent(
        String emailDestinatario,
        String nomeUsuario,
        boolean aprovado,
        String motivoReprovacao   // null quando aprovado
) {}
