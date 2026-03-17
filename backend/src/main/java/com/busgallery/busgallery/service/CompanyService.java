package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Company;

import java.util.List;

/**
 * CompanyService接口用于封装CompanyService相关的领域职责（所在包：com.busgallery.busgallery.service）。
 */
public interface CompanyService {

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    Company findById(Long id);

    /**
     * findAll方法用于处理findAll相关的业务逻辑。
     * @return 返回List<Company>类型结果。
     */
    List<Company> findAll();

    /**
     * findByRegion方法用于处理findByRegion相关的业务逻辑。
     * @param regionId regionId参数，详见调用方上下文。
     * @return 返回List<Company>类型结果。
     */
    List<Company> findByRegion(Long regionId);

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    Company create(Company company);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    Company update(Company company);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    void delete(Long id);
}