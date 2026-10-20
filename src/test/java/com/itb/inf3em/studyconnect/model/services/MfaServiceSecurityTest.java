package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.MfaCodeRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import com.itb.inf3em.studyconnect.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfaServiceSecurityTest {

    @Test
    void usuarioInativoNaoRecebeJwtMesmoComDesafioMfa() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        MfaCodeRepository mfaCodeRepository = mock(MfaCodeRepository.class);
        JwtService jwtService = mock(JwtService.class);
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmail("inativo@example.com");
        usuario.setAtivo(false);
        when(usuarioRepository.findByEmail("inativo@example.com")).thenReturn(Optional.of(usuario));

        MfaService service = new MfaService();
        ReflectionTestUtils.setField(service, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(service, "mfaCodeRepository", mfaCodeRepository);
        ReflectionTestUtils.setField(service, "jwtService", jwtService);

        assertThatThrownBy(() -> service.validarCodigo("inativo@example.com", "123456"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        verify(jwtService, never()).generateToken(usuario);
        verify(mfaCodeRepository, never()).findByUsuarioIdAndPurpose(10L, MfaService.PURPOSE_LOGIN);
    }
}
