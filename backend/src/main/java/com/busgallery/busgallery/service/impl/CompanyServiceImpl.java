package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.mapper.CompanyMapper;
import com.busgallery.busgallery.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyMapper companyMapper;

    @Override
    public Company findById(Long id) {
        return companyMapper.selectById(id);
    }

    @Override
    public List<Company> findAll() {
        return companyMapper.selectAll();
    }

    @Override
    public List<Company> findByRegion(Long regionId) {
        return companyMapper.selectByRegionId(regionId);
    }

    @Override
    @Transactional
    public Company create(Company company) {
        companyMapper.insert(company);
        return companyMapper.selectById(company.getId());
    }

    @Override
    @Transactional
    public Company update(Company company) {
        companyMapper.update(company);
        return companyMapper.selectById(company.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        companyMapper.delete(id);
    }
}