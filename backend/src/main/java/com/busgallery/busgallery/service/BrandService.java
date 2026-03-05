package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Brand;

import java.util.List;

public interface BrandService {

    Brand findById(Long id);

    Brand findByName(String name);

    List<Brand> findAll();

    Brand create(Brand brand);

    Brand update(Brand brand);

    void delete(Long id);
}