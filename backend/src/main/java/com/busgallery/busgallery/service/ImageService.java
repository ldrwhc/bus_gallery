package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ImageService接口用于封装ImageService相关的领域职责（所在包：com.busgallery.busgallery.service）。
 */
public interface ImageService {

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Image类型结果。
     */
    Image findById(Long id);

    /**
     * listByVehicle方法用于处理listByVehicle相关的业务逻辑。
     * @param vehicleId vehicleId参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    List<Image> listByVehicle(Long vehicleId);

    /**
     * listLatest方法用于处理listLatest相关的业务逻辑。
     * @param limit limit参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    List<Image> listLatest(int limit);

    /**
     * listByUploader方法用于处理listByUploader相关的业务逻辑。
     * @param uploaderId uploaderId参数，详见调用方上下文。
     * @param page page参数，详见调用方上下文。
     * @param size size参数，详见调用方上下文。
     * @return 返回List<Image>类型结果。
     */
    List<Image> listByUploader(Long uploaderId, int page, int size);

    /**
     * countByUploader方法用于处理countByUploader相关的业务逻辑。
     * @param uploaderId uploaderId参数，详见调用方上下文。
     * @return 返回long类型结果。
     */
    long countByUploader(Long uploaderId);

    /**
     * uploadAndSave方法用于处理uploadAndSave相关的业务逻辑。
     * @param file file参数，详见调用方上下文。
     * @param metadata metadata参数，详见调用方上下文。
     * @return 返回Image类型结果。
     */
    Image uploadAndSave(MultipartFile file, Image metadata);

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param image image参数，详见调用方上下文。
     * @return 返回Image类型结果。
     */
    Image update(Image image);

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    void delete(Long id);
}
