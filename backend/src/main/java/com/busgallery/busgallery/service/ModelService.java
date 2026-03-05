package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Model;

import java.util.List;

public interface ModelService {

    Model findById(Long id);

    List<Model> findAll();

    List<Model> findByBrand(Long brandId);

    Model create(Model model);

    Model update(Model model);

    void delete(Long id);
}