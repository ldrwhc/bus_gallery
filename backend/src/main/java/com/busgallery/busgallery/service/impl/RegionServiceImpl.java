package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Region;
import com.busgallery.busgallery.mapper.RegionMapper;
import com.busgallery.busgallery.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param region region参数，详见调用方上下文。
     * @return 返回Region类型结果。
     */
    @Override
    @Transactional
    public Region create(Region region) {
        regionMapper.insert(region);
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
        regionMapper.update(region);
        return regionMapper.selectById(region.getId());
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