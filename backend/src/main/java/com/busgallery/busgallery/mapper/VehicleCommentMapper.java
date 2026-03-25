package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.VehicleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VehicleCommentMapper {

    VehicleComment selectById(@Param("id") Long id);

    List<VehicleComment> selectPage(@Param("offset") int offset,
                                    @Param("limit") int limit);

    long countAll();

    List<VehicleComment> selectByVehicleId(@Param("vehicleId") Long vehicleId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    long countByVehicleId(@Param("vehicleId") Long vehicleId);

    int insert(VehicleComment comment);

    int updateDisplayNameByUserId(@Param("userId") Long userId,
                                  @Param("displayName") String displayName);

    int deleteById(@Param("id") Long id);

    int deleteByVehicleId(@Param("vehicleId") Long vehicleId);
}
