package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.LoginRequestDTO;
import com.itb.inf3em.studyconnect.model.dto.LoginResponseDTO;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    public LoginResponseDTO login(LoginRequestDTO request) {
        long t0 = System.currentTimeMillis();

        // findByEmail usa o indice UNIQUE do email: uma unica query, sem ORDER BY, sem List
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos"));

        log.info("[AUTH] query DB: {}ms", System.currentTimeMillis() - t0);

        if (!usuario.getSenha().equals(request.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos");
        }

        log.info("[AUTH] login total: {}ms", System.currentTimeMillis() - t0);

        return new LoginResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTipoUsuario().name()
        );
    }
}