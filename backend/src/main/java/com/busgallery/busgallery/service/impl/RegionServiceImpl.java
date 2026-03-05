package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Region;
import com.busgallery.busgallery.mapper.RegionMapper;
import com.busgallery.busgallery.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionMapper regionMapper;

    @Override
    public Region findById(Long id) {
        return regionMapper.selectById(id);
    }

    @Override
    public List<Region> findAll() {
        return regionMapper.selectAll();
    }

    @Override
    public List<Region> findChildren(Long parentId) {
        return regionMapper.selectByParentId(parentId);
    }

    @Override
    @Transactional
    public Region create(Region region) {
        regionMapper.insert(region);
        return regionMapper.selectById(region.getId());
    }

    @Override
    @Transactional
    public Region update(Region region) {
        regionMapper.update(region);
        return regionMapper.selectById(region.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        regionMapper.delete(id);
    }
}