package com.busgallery.busgallery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vehicle类用于封装Vehicle相关的领域职责（所在包：com.busgallery.busgallery.entity）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"vehicleConfig", "vehicleImages"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "vehicle") // 表名正确
public class Vehicle {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 车牌号 */
    @Column(name = "plate_number", length = 64)
    private String plateNumber;

    /** 自编号 */
    @Column(name = "custom_number", length = 64)
    private String customNumber;

    /** 车型（SQL 设为 NOT NULL，显式声明 nullable = false） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false) // 补充 nullable = false（匹配SQL）
    private Model model;

    /** 所属公司（SQL 允许为 null） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id") // 保持 nullable = true（匹配SQL的DEFAULT NULL）
    private Company company;

    /** 所属地区（SQL 允许为 null） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id") // 保持 nullable = true（匹配SQL的DEFAULT NULL）
    private Region region;

    /** 出厂日期 */
    @Column(name = "factory_date")
    private LocalDate factoryDate;

    /** 上线日期 */
    @Column(name = "launch_date")
    private LocalDate launchDate;

    /**
     * 是否空调车（匹配SQL的TINYINT方法用于处理是否空调车（匹配SQL的TINYINT相关的业务逻辑。
     * @param "air_conditioned" "air_conditioned"参数，详见调用方上下文。
     * @return 返回/**类型结果。
     */
    /** 是否空调车（匹配SQL的TINYINT(1)类型） */
    @Column(name = "air_conditioned")
    private Boolean airConditioned = Boolean.FALSE;

    /** 其他来源/备注 */
    @Column(name = "source", length = 256)
    private String source;

    @Column(name = "remark", columnDefinition = "text")
    private String remark;

    /** 匹配 SQL 的创建时间字段（字段名一致） */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 匹配 SQL 的更新时间字段（字段名一致） */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private VehicleConfig vehicleConfig;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VehicleImage> vehicleImages = new ArrayList<>();
}