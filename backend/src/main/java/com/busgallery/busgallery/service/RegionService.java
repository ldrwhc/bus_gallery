package com.busgallery.busgallery.service;

import com.busgallery.busgallery.entity.Region;

import java.util.List;

public interface RegionService {

    Region findById(Long id);

    List<Region> findAll();

    List<Region> findChildren(Long parentId);

    Region create(Region region);

    Region update(Region region);

    void delete(Long id);
}