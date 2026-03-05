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
@ToString(exclude = {"models"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "brand") // 修正表名：brands → brand
public class Brand {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128, unique = true) // 修正长度：100 → 128（匹配SQL）
    private String name;

    /** 品牌中文（SQL 中 chn_name 字段） */
    @Column(name = "chn_name", length = 200) // 补充缺失字段
    private String chnName;

    /** 品牌所属国家/地区，可选 */
    @Column(length = 100)
    private String country;

    @Column(length = 255)
    private String description;

    /** 补充 SQL 中的创建时间字段 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 补充 SQL 中的更新时间字段 */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "brand")
    private List<Model> models = new ArrayList<>();
}