package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Region;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * RegionMapper接口用于封装RegionMapper相关的领域职责（所在包：com.busgallery.busgallery.mapper）。
 */
@Mapper
public interface RegionMapper {

    /**
     * selectById方法用于处理selectById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    Region selectById(@Param("id") Long id);

    /**
     * selectByName方法用于处理selectByName相关的业务逻辑。
     * @param name name参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    Region selectByName(@Param("name") String name);

    /**
     * selectByNameAndParent方法用于处理selectByNameAndParent相关的业务逻辑。
     * @param name name参数，详见调用方上下文。
     * @param parentId parentId参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    Region selectByNameAndParent(@Param("name") String name,
                                 /**
                                  * selectAll方法用于处理selectAll相关的业务逻辑。
                                  * @return 返回List<Region>类型结果。
                                  */
                                 @Param("parentId") Long parentId);

    /**
     * selectAll方法用于处理selectAll相关的业务逻辑。
     * @return 返回List<Region>类型结果。
     */
    List<Region> selectAll();

    /**
     * selectByParentId方法用于处理selectByParentId相关的业务逻辑。
     * @param parentId parentId参数，详见调用方上下文。
     * @return 返回List<Region>类型结果。
     */
    List<Region> selectByParentId(@Param("parentId") Long parentId);

    List<Region> selectByProvinceId(@Param("provinceId") Long provinceId);

    Region selectProvinceByName(@Param("name") String name);

    Region selectCityByNameAndProvince(@Param("name") String name,
                                       @Param("provinceId") Long provinceId);

    /**
     * insert方法用于处理insert相关的业务逻辑。
     * @param region region参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int insert(Region region);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param region region参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int update(Region region);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int delete(@Param("id") Long id);
    /**
     * FULLTEXT search on region name.
     * UPGRADE(ES): Replace with Elasticsearch query when server memory allows.
     */
    List<Region> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

}
