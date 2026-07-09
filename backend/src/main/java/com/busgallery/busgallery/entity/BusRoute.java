package com.busgallery.busgallery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "bus_route")
public class BusRoute {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_number", length = 32, nullable = false)
    private String routeNumber;

    @Column(name = "route_name", length = 200)
    private String routeName;

    @Column(name = "sub_type", length = 16)
    private String subType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_route_id")
    private BusRoute parentRoute;

    @Column(name = "start_stop", length = 128)
    private String startStop;

    @Column(name = "end_stop", length = 128)
    private String endStop;

    @Column(name = "down_start_stop", length = 128)
    private String downStartStop;

    @Column(name = "down_end_stop", length = 128)
    private String downEndStop;

    @Column(name = "is_loop")
    private Boolean isLoop = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "route_type", length = 32, nullable = false)
    private String routeType;

    @Column(name = "line_length_km", precision = 7, scale = 2)
    private BigDecimal lineLengthKm;

    @Column(name = "ticket_type", length = 32)
    private String ticketType;

    @Column(name = "ticket_price", length = 64)
    private String ticketPrice;

    @Column(name = "operating_hours", length = 128)
    private String operatingHours;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "first_operated")
    private LocalDate firstOperated;

    @Column(name = "last_operated")
    private LocalDate lastOperated;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
