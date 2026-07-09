package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.BusRoute;

import java.util.List;

public interface BusRouteService {

    BusRoute findById(Long id);

    List<BusRoute> queryPage(int offset, int limit, Long regionId, Long companyId,
                             String routeType, String keyword, Boolean isActive);

    long count(Long regionId, Long companyId, String routeType, String keyword, Boolean isActive);

    List<BusRoute> searchByKeyword(String keyword, int limit);

    BusRoute create(BusRoute route, Long regionId, Long companyId, Long parentRouteId);

    BusRoute update(BusRoute route, Long regionId, Long companyId, Long parentRouteId);

    void delete(Long id);
}
