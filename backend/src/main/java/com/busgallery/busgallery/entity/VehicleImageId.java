package com.busgallery.busgallery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

/**
 * VehicleImageId类用于封装VehicleImageId相关的领域职责（所在包：com.busgallery.busgallery.entity）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class VehicleImageId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "image_id")
    private Long imageId;
}