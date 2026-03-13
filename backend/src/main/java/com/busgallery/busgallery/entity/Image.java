package com.busgallery.busgallery.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "image")
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_name", length = 512, nullable = false)
    private String objectName;

    @Column(name = "url", length = 1024, nullable = false)
    private String url;

    @Column(name = "thumbnail_url", length = 1024)
    private String thumbnailUrl;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    // 核心修正1：保留原 mimeType 属性，映射到数据库的 content_type 字段
    @Column(name = "content_type", length = 128)
    private String mimeType; // 不再用 contentType，保留旧字段名兼容业务代码

    @Column(name = "hash", length = 128)
    private String hash;

    @Column(name = "uploader", length = 128)
    private String uploadUser;

    @Column(name = "uploader_id")
    private Long uploaderId;

    @Column(name = "uploader_username", length = 64)
    private String uploaderUsername;

    @Column(name = "uploader_display_name", length = 128)
    private String uploaderDisplayName;

    @Column(name = "exif_json", columnDefinition = "TEXT")
    private String exifJson;

    @Transient
    private Long vehicleId;

    @Transient
    private String title;

    @Transient
    private String description;

    @Transient
    private java.util.Map<String, String> exif;

    // 核心修正2：保留原 createTime 属性，映射到数据库的 created_at 字段
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createTime; // 不再用 createdAt，保留旧字段名兼容业务代码

    // 可选：新增兼容方法（如果需要同时支持新旧字段名）
    public String getContentType() {
        return this.mimeType;
    }

    public void setContentType(String contentType) {
        this.mimeType = contentType;
    }

    public LocalDateTime getCreatedAt() {
        return this.createTime;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createTime = createdAt;
    }
}
