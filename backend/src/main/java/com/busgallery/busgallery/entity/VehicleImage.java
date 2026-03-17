package com.busgallery.busgallery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * VehicleImage类用于封装VehicleImage相关的领域职责（所在包：com.busgallery.busgallery.entity）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"vehicle", "image"})
@Entity
@Table(name = "vehicle_image") // 修正表名：vehicle_images → vehicle_image
public class VehicleImage {
    @EmbeddedId
    private VehicleImageId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("vehicleId")
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("imageId")
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    /** 补充 SQL 中的是否封面图字段 */
    @Column(name = "is_cover", nullable = false)
    private Boolean isCover = Boolean.FALSE;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 补充 SQL 中的创建时间字段 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}