package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Image;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ImageMapper接口用于封装ImageMapper相关的领域职责（所在包：com.busgallery.busgallery.mapper）。
 */
@Mapper
public interface ImageMapper {

    /**
     * selectById方法用于处理selectById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Image类型结果。
     */
    Image selectById(@Param("id") Long id);

    /**
     * selectByVehicleId方法用于处理selectByVehicleId相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    List<Image> selectByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * selectLatest方法用于处理selectLatest相关的业务逻辑。
     * @param limit limit参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    List<Image> selectLatest(@Param("limit") int limit);

    /**
     * selectByUploader方法用于处理selectByUploader相关的业务逻辑。
     * @param uploaderId uploaderId参数，详见调用方上下文。
     * @param offset offset参数，详见调用方上下文。
     * @param limit limit参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    List<Image> selectByUploader(@Param("uploaderId") Long uploaderId,
                                 /**
                                  * countByUploader方法用于处理countByUploader相关的业务逻辑。
                                  * @param uploaderId uploaderId参数，详见调用方上下文。
                                  * @return 返回long类型结果。
                                  */
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    /**
     * countByUploader方法用于处理countByUploader相关的业务逻辑。
     * @param uploaderId uploaderId参数，详见调用方上下文。
     * @return 返回long类型结果。
     */
    long countByUploader(@Param("uploaderId") Long uploaderId);

    /**
     * insert方法用于处理insert相关的业务逻辑。
     * @param image image参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int insert(Image image);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param image image参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int update(Image image);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回int类型结果。
     */
    int delete(@Param("id") Long id);
}
