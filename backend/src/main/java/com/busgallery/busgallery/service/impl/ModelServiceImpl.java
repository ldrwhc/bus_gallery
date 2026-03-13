package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Model;
import com.busgallery.busgallery.mapper.BrandMapper;
import com.busgallery.busgallery.mapper.ModelMapper;
import com.busgallery.busgallery.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelMapper modelMapper;
    private final BrandMapper brandMapper;

    @Override
    public Model findById(Long id) {
        Model model = modelMapper.selectById(id);
        enrich(model);
        return model;
    }

    @Override
    public List<Model> findAll() {
        List<Model> models = modelMapper.selectAll();
        models.forEach(this::enrich);
        return models;
    }

    @Override
    public List<Model> findByBrand(Long brandId) {
        List<Model> models = modelMapper.selectByBrandId(brandId);
        models.forEach(this::enrich);
        return models;
    }

    @Override
    @Transactional
    public Model create(Model model) {
        modelMapper.insert(model);
        Model saved = modelMapper.selectById(model.getId());
        enrich(saved);
        return saved;
    }

    @Override
    @Transactional
    public Model update(Model model) {
        modelMapper.update(model);
        Model saved = modelMapper.selectById(model.getId());
        enrich(saved);
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        modelMapper.delete(id);
    }

    private void enrich(Model model) {
        if (model == null) {
            return;
        }
        if (model.getBrand() != null && model.getBrand().getId() != null) {
            model.setBrand(brandMapper.selectById(model.getBrand().getId()));
        }
    }
}
