package com.busgallery.busgallery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model类用于封装Model相关的领域职责（所在包：com.busgallery.busgallery.entity）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"vehicles"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "model") // 修正表名：models → model
public class Model {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 车型/型号名（修正长度：100 → 128，匹配SQL） */
    @Column(nullable = false, length = 128)
    private String name;

    /** 补充 SQL 中的厂内型号/代码字段 */
    @Column(name = "model_code", length = 64)
    private String modelCode;

    @Column(length = 255)
    private String description;

    /** 补充 SQL 中的发布年份字段 */
    @Column(name = "release_year")
    private Integer releaseYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @OneToMany(mappedBy = "model")
    private List<Vehicle> vehicles = new ArrayList<>();

    /** 补充 SQL 中的创建时间字段 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 补充 SQL 中的更新时间字段 */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}