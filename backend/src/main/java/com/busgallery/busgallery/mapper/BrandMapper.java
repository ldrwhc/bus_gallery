package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Brand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BrandMapper {

    Brand selectById(@Param("id") Long id);

    Brand selectByName(@Param("name") String name);

    List<Brand> selectAll();

    int insert(Brand brand);

    int update(Brand brand);

    int delete(@Param("id") Long id);
}