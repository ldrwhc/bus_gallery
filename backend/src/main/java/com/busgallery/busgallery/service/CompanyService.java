package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Company;

import java.util.List;

public interface CompanyService {

    Company findById(Long id);

    List<Company> findAll();

    List<Company> findByRegion(Long regionId);

    Company create(Company company);

    Company update(Company company);

    void delete(Long id);
}