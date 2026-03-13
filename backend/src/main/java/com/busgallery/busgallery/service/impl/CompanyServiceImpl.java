package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Region;
import com.busgallery.busgallery.mapper.CompanyMapper;
import com.busgallery.busgallery.mapper.RegionMapper;
import com.busgallery.busgallery.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyMapper companyMapper;
    private final RegionMapper regionMapper;

    @Override
    public Company findById(Long id) {
        return enrich(companyMapper.selectById(id));
    }

    @Override
    public List<Company> findAll() {
        return enrich(companyMapper.selectAll());
    }

    @Override
    public List<Company> findByRegion(Long regionId) {
        return enrich(companyMapper.selectByRegionId(regionId));
    }

    @Override
    @Transactional
    public Company create(Company company) {
        companyMapper.insert(company);
        return enrich(companyMapper.selectById(company.getId()));
    }

    @Override
    @Transactional
    public Company update(Company company) {
        companyMapper.update(company);
        return enrich(companyMapper.selectById(company.getId()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        companyMapper.delete(id);
    }

    private Company enrich(Company company) {
        if (company == null) {
            return null;
        }
        if (company.getRegion() != null && company.getRegion().getId() != null) {
            Region region = regionMapper.selectById(company.getRegion().getId());
            company.setRegion(region);
        }
        return company;
    }

    private List<Company> enrich(List<Company> companies) {
        if (companies == null) {
            return null;
        }
        companies.forEach(this::enrich);
        return companies;
    }
}
