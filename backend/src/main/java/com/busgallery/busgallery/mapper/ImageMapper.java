package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Image;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageMapper {

    Image selectById(@Param("id") Long id);

    List<Image> selectByVehicleId(@Param("vehicleId") Long vehicleId);

    List<Image> selectLatest(@Param("limit") int limit);

    int insert(Image image);

    int update(Image image);

    int delete(@Param("id") Long id);
}