package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Model;

import java.util.List;

/**
 * ModelService接口用于封装ModelService相关的领域职责（所在包：com.busgallery.busgallery.service）。
 */
public interface ModelService {

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    Model findById(Long id);

    /**
     * findAll方法用于处理findAll相关的业务逻辑。
     * @return 返回List<Model>类型结果。
     */
    List<Model> findAll();

    /**
     * findByBrand方法用于处理findByBrand相关的业务逻辑。
     * @param brandId brandId参数，详见调用方上下文。
     * @return 返回List<Model>类型结果。
     */
    List<Model> findByBrand(Long brandId);

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param model model参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    Model create(Model model);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param model model参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    Model update(Model model);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    void delete(Long id);
}