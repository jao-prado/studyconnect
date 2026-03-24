package com.itb.inf3em.studyconnect.model.services;

import com.itb.inf3em.studyconnect.model.entity.Material;
import com.itb.inf3em.studyconnect.model.repository.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaterialService {

    @Autowired
    private MaterialRepository materialRepository;

    public List<Material> findAll() {
        return materialRepository.findAll();
    }

    public Material save(Material material) {
        material.setAtivo(true);
        return materialRepository.save(material);
    }

    public Material findById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material não encontrado com o id: " + id));
    }

    public Material update(long id, Material material) {

        Material materialExistente = findById(id);

        materialExistente.setTitulo(material.getTitulo());
        materialExistente.setDescricao(material.getDescricao());
        materialExistente.setCategoria(material.getCategoria());
        materialExistente.setAtivo(material.isAtivo());

        return materialRepository.save(materialExistente);
    }

    public void delete(long id) {

        Material materialExistente = findById(id);

        materialRepository.delete(materialExistente);
    }
}