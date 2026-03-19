package com.busgallery.busgallery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Region类用于封装Region相关的领域职责（所在包：com.busgallery.busgallery.entity）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"children", "companies"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "region") // 修正表名：regions → region
public class Region {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 地区名称（修正长度：100 → 128，匹配SQL） */
    @Column(nullable = false, length = 128)
    private String name;

    /** 父级地区（若只维护市级可为空） */
    @Column(name = "parent_id")
    private Long parentId;

    /** 所属省级ID：省本身等于自身ID，市级指向所属省 */
    @Column(name = "province_id")
    private Long provinceId;

    /** 行政级别：1-省、2-市、3-区县...（匹配SQL的TINYINT类型） */
    private Integer level;

    /** 地区类型：PROVINCE / CITY */
    @Column(name = "region_type", length = 16)
    private String regionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Region parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Region> children = new ArrayList<>();

    @OneToMany(mappedBy = "region")
    private List<Company> companies = new ArrayList<>();

    /** 补充 SQL 中的创建时间字段 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 补充 SQL 中的更新时间字段 */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
