package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.BusRoute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusRouteMapper {

    BusRoute selectById(@Param("id") Long id);

    List<BusRoute> selectPage(@Param("offset") int offset,
                              @Param("limit") int limit,
                              @Param("regionId") Long regionId,
                              @Param("companyId") Long companyId,
                              @Param("routeType") String routeType,
                              @Param("keyword") String keyword,
                              @Param("isActive") Boolean isActive);

    long count(@Param("regionId") Long regionId,
               @Param("companyId") Long companyId,
               @Param("routeType") String routeType,
               @Param("keyword") String keyword,
               @Param("isActive") Boolean isActive);

    List<BusRoute> searchByKeyword(@Param("keyword") String keyword,
                                   @Param("limit") int limit);

    int insert(BusRoute route);

    int update(BusRoute route);

    int delete(@Param("id") Long id);
}
