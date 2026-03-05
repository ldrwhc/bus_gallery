package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Brand;
import com.busgallery.busgallery.mapper.BrandMapper;
import com.busgallery.busgallery.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandMapper brandMapper;

    @Override
    public Brand findById(Long id) {
        return brandMapper.selectById(id);
    }

    @Override
    public Brand findByName(String name) {
        return brandMapper.selectByName(name);
    }

    @Override
    public List<Brand> findAll() {
        return brandMapper.selectAll();
    }

    @Override
    @Transactional
    public Brand create(Brand brand) {
        brandMapper.insert(brand);
        return brandMapper.selectById(brand.getId());
    }

    @Override
    @Transactional
    public Brand update(Brand brand) {
        brandMapper.update(brand);
        return brandMapper.selectById(brand.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        brandMapper.delete(id);
    }
}