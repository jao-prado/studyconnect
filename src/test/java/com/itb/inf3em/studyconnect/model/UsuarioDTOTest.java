package com.itb.inf3em.studyconnect.model;

import com.itb.inf3em.studyconnect.model.dto.UsuarioDTO;
import com.itb.inf3em.studyconnect.model.entity.TipoUsuario;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioDTOTest {

    @Test
    void incluiFotoUrlSemExporCredenciais() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Aluno");
        usuario.setEmail("aluno@example.com");
        usuario.setTipoUsuario(TipoUsuario.ALUNO);
        usuario.setFotoUrl("https://example.com/foto.jpg");
        usuario.setSenha("hash-nao-deve-sair");
        usuario.setGoogleId("google-id-nao-deve-sair");

        UsuarioDTO dto = new UsuarioDTO(usuario);

        assertThat(dto.getFotoUrl()).isEqualTo("https://example.com/foto.jpg");
        assertThat(UsuarioDTO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("senha", "googleId");
    }
}
