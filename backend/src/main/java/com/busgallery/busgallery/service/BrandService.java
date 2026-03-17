package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Brand;

import java.util.List;

/**
 * BrandService接口用于封装BrandService相关的领域职责（所在包：com.busgallery.busgallery.service）。
 */
public interface BrandService {

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Brand类型结果。
     */
    Brand findById(Long id);

    /**
     * findByName方法用于处理findByName相关的业务逻辑。
     * @param name name参数，详见调用方上下文。
     * @return 返回Brand类型结果。
     */
    Brand findByName(String name);

    /**
     * findAll方法用于处理findAll相关的业务逻辑。
     * @return 返回List<Brand>类型结果。
     */
    List<Brand> findAll();

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param brand brand参数，详见调用方上下文。
     * @return 返回Brand类型结果。
     */
    Brand create(Brand brand);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param brand brand参数，详见调用方上下文。
     * @return 返回Brand类型结果。
     */
    Brand update(Brand brand);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    void delete(Long id);
}