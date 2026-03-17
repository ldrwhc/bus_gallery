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

/**
 * CompanyServiceImpl类用于封装CompanyServiceImpl相关的领域职责（所在包：com.busgallery.busgallery.service.impl）。
 */
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyMapper companyMapper;
    private final RegionMapper regionMapper;

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    @Override
    public Company findById(Long id) {
        return enrich(companyMapper.selectById(id));
    }

    /**
     * findAll方法用于处理findAll相关的业务逻辑。
     * @return 返回List<Company>类型结果。
     */
    @Override
    public List<Company> findAll() {
        return enrich(companyMapper.selectAll());
    }

    /**
     * findByRegion方法用于处理findByRegion相关的业务逻辑。
     * @param regionId regionId参数，详见调用方上下文。
     * @return 返回List<Company>类型结果。
     */
    @Override
    public List<Company> findByRegion(Long regionId) {
        return enrich(companyMapper.selectByRegionId(regionId));
    }

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    @Override
    @Transactional
    public Company create(Company company) {
        companyMapper.insert(company);
        return enrich(companyMapper.selectById(company.getId()));
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    @Override
    @Transactional
    public Company update(Company company) {
        companyMapper.update(company);
        return enrich(companyMapper.selectById(company.getId()));
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        companyMapper.delete(id);
    }

    /**
     * enrich方法用于处理enrich相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
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

    /**
     * enrich方法用于处理enrich相关的业务逻辑。
     * @param companies companies参数，详见调用方上下文。
     * @return 返回List<Company>类型结果。
     */
    private List<Company> enrich(List<Company> companies) {
        if (companies == null) {
            return null;
        }
        companies.forEach(this::enrich);
        return companies;
    }
}
