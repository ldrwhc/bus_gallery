package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Model;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ModelMapper {

    Model selectById(@Param("id") Long id);

    Model selectByBrandAndName(@Param("brandId") Long brandId,
                               @Param("name") String name);

    List<Model> selectByBrandId(@Param("brandId") Long brandId);

    List<Model> selectAll();

    int insert(Model model);

    int update(Model model);

    int delete(@Param("id") Long id);
}