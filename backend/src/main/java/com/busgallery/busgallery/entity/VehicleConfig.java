package com.busgallery.busgallery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"vehicle"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "vehicle_config") // 修正表名：vehicle_configs → vehicle_config
public class VehicleConfig {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, unique = true)
    private Vehicle vehicle;

    /** 品牌（SQL 允许为 null，修正 nullable = false → 移除） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id") // 移除 nullable = false（匹配SQL的DEFAULT NULL）
    private Brand brand;

    /** 车型（SQL 允许为 null，修正 nullable = false → 移除） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id") // 移除 nullable = false（匹配SQL的DEFAULT NULL）
    private Model model;

    /** 电机型号 */
    @Column(length = 128) // 修正长度：100 → 128（匹配SQL）
    private String motor;

    /** 发动机型号 */
    @Column(length = 128) // 修正长度：100 → 128（匹配SQL）
    private String engine;

    /** 燃料类型（修正长度：100 → 64，匹配SQL） */
    @Column(name = "fuel_type", length = 64)
    private String fuelType;

    /** 踏步配置（修正长度：100 → 64，匹配SQL） */
    @Column(name = "step_type", length = 64)
    private String stepType;

    /** 悬挂形式 */
    @Column(length = 128) // 修正长度：100 → 128（匹配SQL）
    private String suspension;

    /** 车桥 */
    @Column(length = 128) // 修正长度：100 → 128（匹配SQL）
    private String axle;

    /** 其他配置描述 */
    @Column(name = "other_configs", columnDefinition = "text")
    private String otherConfigs;

    /** 补充 SQL 中的创建时间字段 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 补充 SQL 中的更新时间字段 */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}