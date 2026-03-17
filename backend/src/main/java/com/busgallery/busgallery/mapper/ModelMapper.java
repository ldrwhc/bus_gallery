package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Model;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ModelMapper接口用于封装ModelMapper相关的领域职责（所在包：com.busgallery.busgallery.mapper）。
 */
@Mapper
public interface ModelMapper {

    /**
     * selectById方法用于处理selectById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    Model selectById(@Param("id") Long id);

    /**
     * selectByBrandAndName方法用于处理selectByBrandAndName相关的业务逻辑。
     * @param brandId brandId参数，详见调用方上下文。
     * @param name name参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    Model selectByBrandAndName(@Param("brandId") Long brandId,
                               /**
                                * selectByBrandId方法用于处理selectByBrandId相关的业务逻辑。
                                * @param brandId brandId参数，详见调用方上下文。
                                * @return 返回List<Model>类型结果。
                                */
                               @Param("name") String name);

    /**
     * selectByBrandId方法用于处理selectByBrandId相关的业务逻辑。
     * @param brandId brandId参数，详见调用方上下文。
     * @return 返回List<Model>类型结果。
     */
    List<Model> selectByBrandId(@Param("brandId") Long brandId);

    /**
     * selectAll方法用于处理selectAll相关的业务逻辑。
     * @return 返回List<Model>类型结果。
     */
    List<Model> selectAll();

    /**
     * insert方法用于处理insert相关的业务逻辑。
     * @param model model参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int insert(Model model);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param model model参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int update(Model model);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int delete(@Param("id") Long id);
}