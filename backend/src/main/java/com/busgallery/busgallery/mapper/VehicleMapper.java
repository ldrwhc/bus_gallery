package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * VehicleMapper接口用于封装VehicleMapper相关的领域职责（所在包：com.busgallery.busgallery.mapper）。
 */
@Mapper
public interface VehicleMapper {

    /**
     * selectById方法用于处理selectById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Vehicle类型结果。
     */
    Vehicle selectById(@Param("id") Long id);

    /**
     * selectByPlateNumber方法用于处理selectByPlateNumber相关的业务逻辑。
     * @param plateNumber plateNumber参数，详见调用方上下文。
     * @return 返回Vehicle类型结果。
     */
    Vehicle selectByPlateNumber(@Param("plateNumber") String plateNumber);

    /**
     * selectByRegionId方法用于处理selectByRegionId相关的业务逻辑。
     * @param regionId regionId参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    List<Vehicle> selectByRegionId(@Param("regionId") Long regionId);

    /**
     * selectByCompanyId方法用于处理selectByCompanyId相关的业务逻辑。
     * @param companyId companyId参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    List<Vehicle> selectByCompanyId(@Param("companyId") Long companyId);

    /**
     * selectByModelId方法用于处理selectByModelId相关的业务逻辑。
     * @param modelId modelId参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    List<Vehicle> selectByModelId(@Param("modelId") Long modelId);

    /**
     * selectPage方法用于处理selectPage相关的业务逻辑。
     * @param offset offset参数，详见调用方上下文。
     * @param limit limit参数，详见调用方上下文。
     * @param regionId regionId参数，详见调用方上下文。
     * @param companyId companyId参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param modelId modelId参数，详见调用方上下文。
     * @param keyword keyword参数，详见调用方上下文。
     * @return 返回List<Vehicle>类型结果。
     */
    List<Vehicle> selectPage(@Param("offset") int offset,
                             /**
                              * count方法用于处理count相关的业务逻辑。
                              * @param regionId regionId参数，详见调用方上下文。
                              * @param companyId companyId参数，详见调用方上下文。
                              * @param brandId brandId参数，详见调用方上下文。
                              * @param modelId modelId参数，详见调用方上下文。
                              * @param keyword keyword参数，详见调用方上下文。
                              * @return 返回long类型结果。
                              */
                             @Param("limit") int limit,
                             @Param("regionId") Long regionId,
                             @Param("companyId") Long companyId,
                             @Param("brandId") Long brandId,
                             @Param("modelId") Long modelId,
                             @Param("keyword") String keyword);

    /**
     * count方法用于处理count相关的业务逻辑。
     * @param regionId regionId参数，详见调用方上下文。
     * @param companyId companyId参数，详见调用方上下文。
     * @param brandId brandId参数，详见调用方上下文。
     * @param modelId modelId参数，详见调用方上下文。
     * @param keyword keyword参数，详见调用方上下文。
     * @return 返回long类型结果。
     */
    long count(@Param("regionId") Long regionId,
               /**
                * insert方法用于处理insert相关的业务逻辑。
                * @param vehicle vehicle参数，详见调用方上下文。
                * @return 返回int类型结果。
                */
               @Param("companyId") Long companyId,
               @Param("brandId") Long brandId,
               @Param("modelId") Long modelId,
               @Param("keyword") String keyword);

    /**
     * insert方法用于处理insert相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int insert(Vehicle vehicle);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param vehicle vehicle参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int update(Vehicle vehicle);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int delete(@Param("id") Long id);
}
