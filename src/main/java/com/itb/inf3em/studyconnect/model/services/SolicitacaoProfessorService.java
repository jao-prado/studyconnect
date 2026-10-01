package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.SolicitacaoProfessorDTO;
import com.itb.inf3em.studyconnect.model.entity.SolicitacaoProfessor;
import com.itb.inf3em.studyconnect.model.entity.StatusSolicitacao;
import com.itb.inf3em.studyconnect.model.entity.TipoComprovante;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.repository.SolicitacaoProfessorRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.security.AuthenticatedUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SolicitacaoProfessorService {

    private static final int URL_MAX_LENGTH = 2000;

    private final SolicitacaoProfessorRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SolicitacaoProfessorService(SolicitacaoProfessorRepository solicitacaoRepository,
                                       UsuarioRepository usuarioRepository,
                                       ApplicationEventPublisher eventPublisher) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Cria solicitação vinculada ao usuário autenticado.
     * usuarioId, status, dataAnalise e motivoReprovacao do body são ignorados — identidade e estado vêm do backend.
     */
    @Transactional
    public SolicitacaoProfessorDTO criar(Map<String, Object> body, AuthenticatedUser caller) {
        // Identidade sempre do JWT
        Usuario usuario = usuarioRepository.findById(caller.usuarioId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));

        // Proteção contra múltiplas solicitações pendentes
        if (solicitacaoRepository.existsByUsuarioIdAndStatus(usuario.getId(), StatusSolicitacao.PENDENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já possui uma solicitação pendente de análise.");
        }

        // tipoComprovante — obrigatório e deve ser um valor válido do enum
        String tipoRaw = body.getOrDefault("tipoComprovante", "").toString().trim();
        if (tipoRaw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipoComprovante é obrigatório.");
        }
        TipoComprovante tipoComprovante;
        try {
            tipoComprovante = TipoComprovante.valueOf(tipoRaw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tipoComprovante inválido. Valores aceitos: CERTIFICADO, VINCULO_INSTITUCIONAL, EMAIL_INSTITUCIONAL, OUTRO.");
        }

        // comprovanteUrl — obrigatório, tamanho e formato básico de URL
        String url = body.getOrDefault("comprovanteUrl", "").toString().trim();
        if (url.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "comprovanteUrl é obrigatório.");
        }
        if (url.length() > URL_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "comprovanteUrl excede o tamanho máximo de " + URL_MAX_LENGTH + " caracteres.");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "comprovanteUrl deve ser uma URL válida iniciando com http:// ou https://.");
        }

        SolicitacaoProfessor solicitacao = new SolicitacaoProfessor();
        solicitacao.setUsuario(usuario);
        solicitacao.setTipoComprovante(tipoComprovante);
        solicitacao.setComprovanteUrl(url);
        solicitacao.setStatus(StatusSolicitacao.PENDENTE); // sempre PENDENTE — nunca do body

        return new SolicitacaoProfessorDTO(solicitacaoRepository.save(solicitacao));
    }

    /**
     * Lista solicitações do próprio usuário autenticado.
     */
    @Transactional(readOnly = true)
    public List<SolicitacaoProfessorDTO> listarMinhas(AuthenticatedUser caller) {
        return solicitacaoRepository.findByUsuarioIdOrderByDataSolicitacaoDesc(caller.usuarioId())
                .stream().map(SolicitacaoProfessorDTO::new).toList();
    }

    // ── Operações ADMIN ──────────────────────────────────────────────────────

    /**
     * Lista todas as solicitações. Filtro por status é opcional.
     * status=null → retorna todas.
     */
    @Transactional(readOnly = true)
    public List<SolicitacaoProfessorDTO> listarTodas(String statusParam) {
        if (statusParam != null && !statusParam.isBlank()) {
            StatusSolicitacao status;
            try {
                status = StatusSolicitacao.valueOf(statusParam.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Status inválido. Valores aceitos: PENDENTE, APROVADO, REPROVADO.");
            }
            return solicitacaoRepository.findByStatusOrderByDataSolicitacaoDesc(status)
                    .stream().map(SolicitacaoProfessorDTO::new).toList();
        }
        return solicitacaoRepository.findAllByOrderByDataSolicitacaoDesc()
                .stream().map(SolicitacaoProfessorDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public SolicitacaoProfessorDTO buscarPorId(Long id) {
        return new SolicitacaoProfessorDTO(encontrarOuErro(id));
    }

    @Transactional
    public SolicitacaoProfessorDTO aprovar(Long id) {
        SolicitacaoProfessor solicitacao = encontrarOuErro(id);
        verificarPendente(solicitacao);

        Usuario usuario = solicitacao.getUsuario();
        usuario.setTipoUsuario(TipoUsuario.PROFESSOR);
        usuarioRepository.save(usuario);

        solicitacao.setStatus(StatusSolicitacao.APROVADO);
        solicitacao.setDataAnalise(LocalDateTime.now());
        SolicitacaoProfessorDTO resultado = new SolicitacaoProfessorDTO(solicitacaoRepository.save(solicitacao));

        // Publica evento — e-mail enviado APÓS commit, falha no e-mail não desfaz a aprovação
        eventPublisher.publishEvent(new SolicitacaoAnalisadaEvent(
                usuario.getEmail(), usuario.getNome(), true, null));

        return resultado;
    }

    @Transactional
    public SolicitacaoProfessorDTO reprovar(Long id, String motivoReprovacao) {
        if (motivoReprovacao == null || motivoReprovacao.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "motivoReprovacao é obrigatório.");
        }

        SolicitacaoProfessor solicitacao = encontrarOuErro(id);
        verificarPendente(solicitacao);

        solicitacao.setStatus(StatusSolicitacao.REPROVADO);
        solicitacao.setDataAnalise(LocalDateTime.now());
        solicitacao.setMotivoReprovacao(motivoReprovacao.trim());
        SolicitacaoProfessorDTO resultado = new SolicitacaoProfessorDTO(solicitacaoRepository.save(solicitacao));

        // Publica evento — e-mail enviado APÓS commit, falha no e-mail não desfaz a reprovação
        eventPublisher.publishEvent(new SolicitacaoAnalisadaEvent(
                solicitacao.getUsuario().getEmail(),
                solicitacao.getUsuario().getNome(),
                false,
                motivoReprovacao.trim()));

        return resultado;
    }

    private SolicitacaoProfessor encontrarOuErro(Long id) {
        return solicitacaoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));
    }

    private void verificarPendente(SolicitacaoProfessor solicitacao) {
        if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta solicitação já foi analisada.");
        }
    }
}
