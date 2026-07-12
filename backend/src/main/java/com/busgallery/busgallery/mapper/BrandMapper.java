package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Brand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * BrandMapper接口用于封装BrandMapper相关的领域职责（所在包：com.busgallery.busgallery.mapper）。
 */
@Mapper
public interface BrandMapper {

    /**
     * selectById方法用于处理selectById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Brand类型结果。
     */
    Brand selectById(@Param("id") Long id);

    /**
     * selectByName方法用于处理selectByName相关的业务逻辑。
     * @param name name参数，详见调用方上下文。
     * @return 返回Brand类型结果。
     */
    Brand selectByName(@Param("name") String name);

    /**
     * selectByChnName — 按中文全称查找品牌。
     */
    Brand selectByChnName(@Param("chnName") String chnName);

    /**
     * selectAll方法用于处理selectAll相关的业务逻辑。
     * @return 返回List<Brand>类型结果。
     */
    List<Brand> selectAll();

    /**
     * insert方法用于处理insert相关的业务逻辑。
     * @param brand brand参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int insert(Brand brand);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param brand brand参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int update(Brand brand);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int delete(@Param("id") Long id);
}