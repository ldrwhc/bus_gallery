package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.BusRoute;
import com.busgallery.busgallery.entity.Company;
import com.busgallery.busgallery.entity.Region;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.mapper.BusRouteMapper;
import com.busgallery.busgallery.service.BusRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusRouteServiceImpl implements BusRouteService {

    private final BusRouteMapper busRouteMapper;

    @Override
    public BusRoute findById(Long id) {
        BusRoute route = busRouteMapper.selectById(id);
        if (route == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "线路不存在");
        }
        return route;
    }

    @Override
    public List<BusRoute> queryPage(int offset, int limit, Long regionId, Long companyId,
                                    String routeType, String keyword, Boolean isActive) {
        return busRouteMapper.selectPage(offset, limit, regionId, companyId, routeType, keyword, isActive);
    }

    @Override
    public long count(Long regionId, Long companyId, String routeType, String keyword, Boolean isActive) {
        return busRouteMapper.count(regionId, companyId, routeType, keyword, isActive);
    }

    @Override
    public List<BusRoute> searchByKeyword(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return busRouteMapper.searchByKeyword(keyword, limit);
    }

    @Override
    @Transactional
    public BusRoute create(BusRoute route, Long regionId, Long companyId, Long parentRouteId) {
        if (regionId != null) {
            Region r = new Region();
            r.setId(regionId);
            route.setRegion(r);
        }
        if (companyId != null) {
            Company c = new Company();
            c.setId(companyId);
            route.setCompany(c);
        }
        if (parentRouteId != null) {
            BusRoute parent = new BusRoute();
            parent.setId(parentRouteId);
            route.setParentRoute(parent);
        }
        busRouteMapper.insert(route);
        return findById(route.getId());
    }

    @Override
    @Transactional
    public BusRoute update(BusRoute route, Long regionId, Long companyId, Long parentRouteId) {
        BusRoute existing = busRouteMapper.selectById(route.getId());
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "线路不存在");
        }
        if (regionId != null) {
            Region r = new Region();
            r.setId(regionId);
            route.setRegion(r);
        } else {
            route.setRegion(null);
        }
        if (companyId != null) {
            Company c = new Company();
            c.setId(companyId);
            route.setCompany(c);
        } else {
            route.setCompany(null);
        }
        if (parentRouteId != null) {
            BusRoute parent = new BusRoute();
            parent.setId(parentRouteId);
            route.setParentRoute(parent);
        } else {
            route.setParentRoute(null);
        }
        busRouteMapper.update(route);
        return findById(route.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        busRouteMapper.delete(id);
    }
}
