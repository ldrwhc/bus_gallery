package com.busgallery.busgallery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"vehicles"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "company") // 修正表名：companies → company
public class Company {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128) // 修正长度：100 → 128（匹配SQL）
    private String name;

    @Column(length = 255)
    private String description;

    /** 补充 SQL 中的 logo_url 字段 */
    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    /** 所属地区（SQL 允许为 null，修正 nullable = false → 移除） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id") // 移除 nullable = false（匹配SQL的DEFAULT NULL）
    private Region region;

    /** 补充 SQL 中的创建时间字段 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 补充 SQL 中的更新时间字段 */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "company")
    private List<Vehicle> vehicles = new ArrayList<>();
}