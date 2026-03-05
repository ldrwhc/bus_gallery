package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Region;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RegionMapper {

    Region selectById(@Param("id") Long id);

    Region selectByName(@Param("name") String name);

    Region selectByNameAndParent(@Param("name") String name,
                                 @Param("parentId") Long parentId);

    List<Region> selectAll();

    List<Region> selectByParentId(@Param("parentId") Long parentId);

    int insert(Region region);

    int update(Region region);

    int delete(@Param("id") Long id);
}