package com.busgallery.busgallery.service.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StorageObject类用于封装StorageObject相关的领域职责（所在包：com.busgallery.busgallery.service.storage）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageObject {

    /**
     * MinIO 中的对象名（含路径）
     */
    private String objectName;

    /**
     * 原图 URL
     */
    private String url;

    /**
     * 缩略图 URL（目前与原图一致，可后续扩展生成缩略图）
     */
    private String thumbnailUrl;
}