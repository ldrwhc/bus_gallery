package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Company;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CompanyMapper接口用于封装CompanyMapper相关的领域职责（所在包：com.busgallery.busgallery.mapper）。
 */
@Mapper
public interface CompanyMapper {

    /**
     * selectById方法用于处理selectById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    Company selectById(@Param("id") Long id);

    /**
     * selectByName方法用于处理selectByName相关的业务逻辑。
     * @param name name参数，详见调用方上下文。
     * @return 返回Company类型结果。
     */
    Company selectByName(@Param("name") String name);

    /**
     * selectByRegionId方法用于处理selectByRegionId相关的业务逻辑。
     * @param regionId regionId参数，详见调用方上下文。
     * @return 返回List<Company>类型结果。
     */
    List<Company> selectByRegionId(@Param("regionId") Long regionId);

    /**
     * selectAll方法用于处理selectAll相关的业务逻辑。
     * @return 返回List<Company>类型结果。
     */
    List<Company> selectAll();

    /**
     * insert方法用于处理insert相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int insert(Company company);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param company company参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int update(Company company);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int delete(@Param("id") Long id);
}