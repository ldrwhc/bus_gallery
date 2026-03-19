package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Region;
import com.busgallery.busgallery.mapper.RegionMapper;
import com.busgallery.busgallery.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RegionServiceImpl类用于封装RegionServiceImpl相关的领域职责（所在包：com.busgallery.busgallery.service.impl）。
 */
@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionMapper regionMapper;

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    @Override
    public Region findById(Long id) {
        return regionMapper.selectById(id);
    }

    /**
     * findAll方法用于处理findAll相关的业务逻辑。
     * @return 返回List<Region>类型结果。
     */
    @Override
    public List<Region> findAll() {
        return regionMapper.selectAll();
    }

    /**
     * findChildren方法用于处理findChildren相关的业务逻辑。
     * @param parentId parentId参数，详见调用方上下文。
     * @return 返回List<Region>类型结果。
     */
    @Override
    public List<Region> findChildren(Long parentId) {
        return regionMapper.selectByParentId(parentId);
    }

    @Override
    public List<Region> findByProvinceId(Long provinceId) {
        return regionMapper.selectByProvinceId(provinceId);
    }

    @Override
    public Region findProvinceByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return regionMapper.selectProvinceByName(name.trim());
    }

    @Override
    public Region findCityByNameAndProvince(String cityName, Long provinceId) {
        if (!StringUtils.hasText(cityName)) {
            return null;
        }
        return regionMapper.selectCityByNameAndProvince(cityName.trim(), provinceId);
    }

    @Override
    public Long resolveProvinceId(Long regionId) {
        if (regionId == null) {
            return null;
        }
        Region region = regionMapper.selectById(regionId);
        if (region == null) {
            return null;
        }
        if (region.getProvinceId() != null) {
            return region.getProvinceId();
        }
        Set<Long> visited = new HashSet<>();
        Region cursor = region;
        while (cursor != null && cursor.getId() != null && visited.add(cursor.getId())) {
            if (cursor.getParentId() == null || (cursor.getLevel() != null && cursor.getLevel() <= 1)) {
                return cursor.getId();
            }
            if (cursor.getProvinceId() != null) {
                return cursor.getProvinceId();
            }
            cursor = regionMapper.selectById(cursor.getParentId());
        }
        return regionId;
    }

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param region region参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    @Override
    @Transactional
    public Region create(Region region) {
        normalizeRegion(region);
        regionMapper.insert(region);
        if ("PROVINCE".equals(region.getRegionType()) && region.getProvinceId() == null && region.getId() != null) {
            region.setProvinceId(region.getId());
            regionMapper.update(region);
        }
        return regionMapper.selectById(region.getId());
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param region region参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    @Override
    @Transactional
    public Region update(Region region) {
        normalizeRegion(region);
        regionMapper.update(region);
        if ("PROVINCE".equals(region.getRegionType()) && region.getProvinceId() == null && region.getId() != null) {
            region.setProvinceId(region.getId());
            regionMapper.update(region);
        }
        return regionMapper.selectById(region.getId());
    }

    private void normalizeRegion(Region region) {
        if (region == null) {
            return;
        }
        if (region.getParentId() == null) {
            region.setLevel(region.getLevel() == null ? 1 : Math.min(region.getLevel(), 1));
            region.setRegionType("PROVINCE");
            if (region.getId() != null && region.getProvinceId() == null) {
                region.setProvinceId(region.getId());
            }
            return;
        }
        region.setLevel(region.getLevel() == null ? 2 : Math.max(region.getLevel(), 2));
        region.setRegionType("CITY");
        Long provinceId = resolveProvinceId(region.getParentId());
        region.setProvinceId(provinceId != null ? provinceId : region.getParentId());
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        regionMapper.delete(id);
    }
}
