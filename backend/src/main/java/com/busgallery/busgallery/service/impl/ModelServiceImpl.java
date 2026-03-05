package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Model;
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

    @Override
    public Model findById(Long id) {
        return modelMapper.selectById(id);
    }

    @Override
    public List<Model> findAll() {
        return modelMapper.selectAll();
    }

    @Override
    public List<Model> findByBrand(Long brandId) {
        return modelMapper.selectByBrandId(brandId);
    }

    @Override
    @Transactional
    public Model create(Model model) {
        modelMapper.insert(model);
        return modelMapper.selectById(model.getId());
    }

    @Override
    @Transactional
    public Model update(Model model) {
        modelMapper.update(model);
        return modelMapper.selectById(model.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        modelMapper.delete(id);
    }
}