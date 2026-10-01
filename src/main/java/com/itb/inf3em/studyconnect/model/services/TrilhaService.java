package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.entity.Trilha;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.MatriculaTrilhaRepository;
import com.itb.inf3em.studyconnect.model.repository.TrilhaRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.security.AuthenticatedUser;
import com.itb.inf3em.studyconnect.security.CurrentUser;
import com.itb.inf3em.studyconnect.security.TrilhaAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;

@Service
public class TrilhaService {

    private static final Logger log = LoggerFactory.getLogger(TrilhaService.class);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem 0/O/1/I
    private static final int CODE_LEN = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TrilhaRepository trilhaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaTrilhaRepository matriculaRepository;
    private final TrilhaAuthorization trilhaAuthorization;
    private final CurrentUser currentUser;

    public TrilhaService(TrilhaRepository trilhaRepository,
                         UsuarioRepository usuarioRepository,
                         MatriculaTrilhaRepository matriculaRepository,
                         TrilhaAuthorization trilhaAuthorization,
                         CurrentUser currentUser) {
        this.trilhaRepository  = trilhaRepository;
        this.usuarioRepository = usuarioRepository;
        this.matriculaRepository = matriculaRepository;
        this.trilhaAuthorization = trilhaAuthorization;
        this.currentUser = currentUser;
    }

    /**
     * Lista trilhas visíveis para o usuário autenticado:
     *  - ADMIN/PROFESSOR: todas as suas próprias trilhas (+ públicas de outros)
     *  - ALUNO: públicas + privadas em que tem matrícula ativa
     */
    public List<Trilha> getAllTrilhas() {
        AuthenticatedUser user = currentUser.require();
        if (user.tipoUsuario() == TipoUsuario.ALUNO) {
            return trilhaRepository.findVisiveisParaAluno(user.usuarioId());
        }
        // Professor/Admin vê todas as públicas + suas próprias privadas
        if (user.tipoUsuario() == TipoUsuario.PROFESSOR) {
            return trilhaRepository.findAll().stream()
                    .filter(t -> "PUBLICA".equals(t.getTipo())
                            || t.getProfessorId().equals(user.usuarioId()))
                    .toList();
        }
        // ADMIN vê tudo
        return trilhaRepository.findAll();
    }

    /**
     * Busca trilha por ID com verificação de autorização para trilhas privadas.
     * Nunca retorna dados de trilha privada para usuário não autorizado.
     */
    public Trilha getTrilhaById(Long id) {
        Trilha trilha = trilhaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trilha não encontrada com o id: " + id));

        if ("PRIVADA".equals(trilha.getTipo())) {
            AuthenticatedUser user = currentUser.require();
            boolean autorizado = user.tipoUsuario() == TipoUsuario.ADMIN
                    || trilha.getProfessorId().equals(user.usuarioId())
                    || matriculaRepository.existsByAlunoIdAndTrilhaIdAndAtivoTrue(user.usuarioId(), id);
            if (!autorizado) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acesso negado. Esta trilha é privada.");
            }
        }
        return trilha;
    }

    public List<Trilha> getTrilhasByProfessor(Long professorId) {
        AuthenticatedUser user = currentUser.require();
        // Professor só vê as próprias; Admin vê de qualquer um
        if (user.tipoUsuario() == TipoUsuario.PROFESSOR && !user.usuarioId().equals(professorId)) {
            throw new AccessDeniedException("Voce nao tem permissao para listar trilhas de outro professor.");
        }
        return trilhaRepository.findByProfessorId(professorId);
    }

    public Trilha createTrilha(Trilha trilha) {
        AuthenticatedUser authenticatedUser = trilhaAuthorization.requireProfessorOrAdmin();
        trilha.setProfessorId(authenticatedUser.usuarioId());

        Usuario professor = usuarioRepository.findById(authenticatedUser.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Professor não encontrado com o id: " + trilha.getProfessorId()));

        if (trilha.getNome() == null || trilha.getNome().trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome da trilha é obrigatório.");
        if (trilha.getTipo() == null || trilha.getTipo().trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo da trilha é obrigatório.");
        if (trilha.getNivel() == null || trilha.getNivel().trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nível da trilha é obrigatório.");

        trilha.setProfessorNome(professor.getNome());

        // Gera código apenas para trilhas privadas; ignora qualquer código enviado pelo frontend
        trilha.setCodigoAcesso("PRIVADA".equals(trilha.getTipo()) ? gerarCodigo() : null);

        try {
            return trilhaRepository.save(trilha);
        } catch (DataIntegrityViolationException ex) {
            log.error("Erro de integridade ao salvar trilha", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível salvar a trilha devido a dados inválidos ou conflitantes.");
        }
    }

    public Trilha updateTrilha(Long id, Trilha trilhaUpdate) {
        Trilha trilhaExistente = trilhaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trilha não encontrada com o id: " + id));
        trilhaAuthorization.requireCanManage(trilhaExistente);

        if (trilhaUpdate.getNome() != null && !trilhaUpdate.getNome().trim().isEmpty())
            trilhaExistente.setNome(trilhaUpdate.getNome());
        if (trilhaUpdate.getDescricao() != null)
            trilhaExistente.setDescricao(trilhaUpdate.getDescricao());
        if (trilhaUpdate.getNivel() != null)
            trilhaExistente.setNivel(trilhaUpdate.getNivel());
        if (trilhaUpdate.getDisciplina() != null)
            trilhaExistente.setDisciplina(trilhaUpdate.getDisciplina());

        // Mudança de tipo: gera/remove código conforme necessário
        if (trilhaUpdate.getTipo() != null && !trilhaUpdate.getTipo().equals(trilhaExistente.getTipo())) {
            trilhaExistente.setTipo(trilhaUpdate.getTipo());
            if ("PRIVADA".equals(trilhaUpdate.getTipo()) && trilhaExistente.getCodigoAcesso() == null) {
                trilhaExistente.setCodigoAcesso(gerarCodigo());
            } else if ("PUBLICA".equals(trilhaUpdate.getTipo())) {
                trilhaExistente.setCodigoAcesso(null);
            }
        }

        return trilhaRepository.save(trilhaExistente);
    }

    public void deleteTrilha(Long id) {
        Trilha trilha = trilhaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trilha não encontrada com o id: " + id));
        trilhaAuthorization.requireCanManage(trilha);
        trilhaRepository.delete(trilha);
    }

    /**
     * Retorna o código de acesso de uma trilha privada.
     * Apenas o professor responsável ou admin pode chamar.
     */
    public String getCodigo(Long trilhaId) {
        Trilha trilha = trilhaRepository.findById(trilhaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trilha não encontrada."));
        trilhaAuthorization.requireCanManage(trilha);
        if (!"PRIVADA".equals(trilha.getTipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta trilha é pública e não possui código de acesso.");
        }
        return trilha.getCodigoAcesso();
    }

    /**
     * Regenera o código de acesso de uma trilha privada.
     * Apenas o professor responsável ou admin pode chamar.
     */
    public String regenerarCodigo(Long trilhaId) {
        Trilha trilha = trilhaRepository.findById(trilhaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trilha não encontrada."));
        trilhaAuthorization.requireCanManage(trilha);
        if (!"PRIVADA".equals(trilha.getTipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta trilha é pública e não possui código de acesso.");
        }
        String novoCodigo = gerarCodigo();
        trilha.setCodigoAcesso(novoCodigo);
        trilhaRepository.save(trilha);
        log.info("[Trilha] Código regenerado para trilhaId={}", trilhaId);
        return novoCodigo;
    }

    /**
     * Aluno informa apenas o código; o backend resolve a trilha internamente.
     */
    public void concederAcessoPorCodigo(String codigoInformado) {
        if (codigoInformado == null || codigoInformado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de acesso obrigatório.");
        }
        Trilha trilha = trilhaRepository.findByCodigoAcesso(codigoInformado.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhuma trilha encontrada com este código."));
        concederAcesso(trilha.getId(), codigoInformado);
    }

    /**
     * Valida o código de acesso e registra a matrícula do aluno na trilha privada.
     * Identificação do aluno exclusivamente pelo JWT — não aceita usuarioId no body.
     */
    public void concederAcesso(Long trilhaId, String codigoInformado) {
        AuthenticatedUser user = currentUser.require();
        if (user.tipoUsuario() != TipoUsuario.ALUNO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Apenas alunos podem solicitar acesso por código.");
        }

        Trilha trilha = trilhaRepository.findById(trilhaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trilha não encontrada."));

        if ("PUBLICA".equals(trilha.getTipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta trilha é pública. Use o endpoint de matrícula diretamente.");
        }

        // Já tem acesso?
        if (matriculaRepository.existsByAlunoIdAndTrilhaIdAndAtivoTrue(user.usuarioId(), trilhaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já possui acesso a esta trilha.");
        }

        if (codigoInformado == null || codigoInformado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de acesso obrigatório.");
        }

        // Comparação case-insensitive e sem espaços
        if (!codigoInformado.trim().equalsIgnoreCase(trilha.getCodigoAcesso())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código de acesso inválido.");
        }

        // Registra matrícula (reativa se existir inativa)
        matriculaRepository.findByAlunoIdAndTrilhaId(user.usuarioId(), trilhaId).ifPresentOrElse(m -> {
            m.setAtivo(true);
            matriculaRepository.save(m);
        }, () -> matriculaRepository.save(
                new com.itb.inf3em.studyconnect.model.entity.MatriculaTrilha(user.usuarioId(), trilhaId)));

        log.info("[Trilha] Acesso concedido: alunoId={} trilhaId={}", user.usuarioId(), trilhaId);
    }

    // ── privado ───────────────────────────────────────────────────────────────

    private String gerarCodigo() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHARS.charAt(SECURE_RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
