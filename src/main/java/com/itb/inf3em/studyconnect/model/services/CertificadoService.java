package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.dto.CertificadoDTO;
import com.itb.inf3em.studyconnect.model.entity.Certificado;
import com.itb.inf3em.studyconnect.model.entity.Usuario;
import com.itb.inf3em.studyconnect.model.repository.CertificadoRepository;
import com.itb.inf3em.studyconnect.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CertificadoService {

    @Autowired
    private CertificadoRepository certificadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<Certificado> findAll() {
        return certificadoRepository.findAll();
    }

    public Certificado findById(Long id) {
        return certificadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificado não encontrado com o id: " + id));
    }

    public Certificado save(CertificadoDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario não encontrado com o id: " + dto.getUsuarioId()));

        Certificado cert = new Certificado();
        cert.setNome(dto.getNome());
        cert.setDescricao(dto.getDescricao());
        cert.setNivel(dto.getNivel());
        cert.setUsuario(usuario);
        cert.setDataEmissao(dto.getDataEmissao());
        cert.setAtivo(dto.isAtivo());

        return certificadoRepository.save(cert);
    }

    public void delete(Long id) {
        Certificado cert = findById(id);
        certificadoRepository.delete(cert);
    }
}