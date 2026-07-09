package com.busgallery.busgallery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "vehicle_route")
public class VehicleRoute {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private BusRoute route;

    @Column(name = "is_current")
    private Boolean isCurrent = Boolean.TRUE;

    @Column(name = "remark", length = 256)
    private String remark;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
