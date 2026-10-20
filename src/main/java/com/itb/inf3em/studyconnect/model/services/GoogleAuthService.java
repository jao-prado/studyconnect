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

        Usuario usuario = usuarioRepository.findByGoogleId(googleId)
                .orElseGet(() -> usuarioRepository.findByEmail(email.toLowerCase())
                        .orElseGet(() -> criarUsuarioGoogle(nome, email, googleId, fotoUrl)));

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
            return mfaService.iniciarDesafio(usuario);
        }

        return new LoginResponseDTO(
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
