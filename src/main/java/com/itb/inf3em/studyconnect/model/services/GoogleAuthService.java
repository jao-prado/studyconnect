package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.LoginResponseDTO;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@Service
public class GoogleAuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final MfaService mfaService;
    private final GoogleIdTokenValidator tokenValidator;

    @Autowired
    public GoogleAuthService(UsuarioRepository usuarioRepository,
                             JwtService jwtService,
                             MfaService mfaService,
                             GoogleIdTokenValidator tokenValidator) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService        = jwtService;
        this.mfaService        = mfaService;
        this.tokenValidator    = tokenValidator;
    }

    @Transactional
    public LoginResponseDTO autenticar(String idToken) {
        Map<String, Object> payload = verificarToken(idToken);

        String googleId = (String) payload.get("sub");
        String email    = (String) payload.get("email");
        String nome     = (String) payload.get("name");
        String fotoUrl  = (String) payload.get("picture");

        if (googleId == null || email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google invalido.");
        }

        var existente = usuarioRepository.findByGoogleId(googleId)
                .or(() -> usuarioRepository.findByEmail(email.toLowerCase()));
        boolean contaCriada = existente.isEmpty();
        Usuario usuario = existente.orElseGet(() -> criarUsuarioGoogle(nome, email, googleId, fotoUrl));

        if (usuario.getGoogleId() != null && !usuario.getGoogleId().equals(googleId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta Google não corresponde ao usuário.");
        }

        // Atualiza googleId e foto se ainda nao estiver salvo
        boolean atualizado = false;
        if (usuario.getGoogleId() == null) {
            usuario.setGoogleId(googleId);
            atualizado = true;
        }
        if (fotoUrl != null && !fotoUrl.equals(usuario.getFotoUrl())) {
            usuario.setFotoUrl(fotoUrl);
            atualizado = true;
        }
        if (atualizado) {
            usuarioRepository.save(usuario);
        }

        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta suspensa. Entre em contato com o suporte.");
        }

        // ADMIN sempre exige MFA; os demais usuarios tambem quando habilitado.
        if (usuario.getTipoUsuario() == TipoUsuario.ADMIN || usuario.isMfaHabilitado()) {
            LoginResponseDTO response = contaCriada
                    ? mfaService.iniciarDesafio(usuario, true)
                    : mfaService.iniciarDesafio(usuario);
            if (contaCriada) response.setOnboardingRequired(true);
            return response;
        }

        LoginResponseDTO resposta = new LoginResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTipoUsuario().name(),
                usuario.getFotoUrl(),
                usuario.getEmail(),
                usuario.isAtivo(),
                jwtService.generateToken(usuario),
                jwtService.getExpirationSeconds(),
                usuario.isMfaHabilitado()
        );
        resposta.setOnboardingRequired(contaCriada);
        return resposta;
    }

    public void validarIdentidadeGoogle(Long usuarioId, String idToken) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        Map<String, Object> payload = verificarToken(idToken);
        Object subject = payload.get("sub");
        if (usuario.getGoogleId() == null || !usuario.getGoogleId().equals(subject)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "A conta Google não corresponde à sessão autenticada.");
        }
        Object issuedAtClaim = payload.get("iat");
        if (!(issuedAtClaim instanceof Number issuedAt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Reautentique-se com o Google para confirmar esta operação.");
        }
        long ageSeconds = Instant.now().getEpochSecond() - issuedAt.longValue();
        if (ageSeconds < -60 || ageSeconds > 600) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Reautentique-se com o Google para confirmar esta operação.");
        }
    }

    public Map<String, Object> verificarToken(String idToken) {
        return tokenValidator.verify(idToken);
    }

    private Usuario criarUsuarioGoogle(String nome, String email, String googleId, String fotoUrl) {
        Usuario u = new Usuario();
        u.setNome(nome != null ? nome : email.split("@")[0]);
        u.setEmail(email.toLowerCase());
        u.setGoogleId(googleId);
        u.setFotoUrl(fotoUrl);
        u.setTipoUsuario(TipoUsuario.ALUNO);
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }
}
