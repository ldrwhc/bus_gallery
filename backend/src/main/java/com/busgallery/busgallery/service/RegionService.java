package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Region;

import java.util.List;

/**
 * RegionService接口用于封装RegionService相关的领域职责（所在包：com.busgallery.busgallery.service）。
 */
public interface RegionService {

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    Region findById(Long id);

    /**
     * findAll方法用于处理findAll相关的业务逻辑。
     * @return 返回List<Region>类型结果。
     */
    List<Region> findAll();

    /**
     * findChildren方法用于处理findChildren相关的业务逻辑。
     * @param parentId parentId参数，详见调用方上下文。
     * @return 返回List<Region>类型结果。
     */
    List<Region> findChildren(Long parentId);

    List<Region> findByProvinceId(Long provinceId);

    Region findProvinceByName(String name);

    Region findCityByNameAndProvince(String cityName, Long provinceId);

    Long resolveProvinceId(Long regionId);

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param region region参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    Region create(Region region);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param region region参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    Region update(Region region);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    void delete(Long id);
}
