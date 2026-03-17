package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Brand;
import com.busgallery.busgallery.mapper.BrandMapper;
import com.busgallery.busgallery.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * BrandServiceImpl类用于封装BrandServiceImpl相关的领域职责（所在包：com.busgallery.busgallery.service.impl）。
 */
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandMapper brandMapper;

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Brand类型结果。
     */
    @Override
    public Brand findById(Long id) {
        return brandMapper.selectById(id);
    }

    /**
     * findByName方法用于处理findByName相关的业务逻辑。
     * @param name name参数，详见调用方上下文。
     * @return 返回Brand类型结果。
     */
    @Override
    public Brand findByName(String name) {
        return brandMapper.selectByName(name);
    }

    /**
     * findAll方法用于处理findAll相关的业务逻辑。
     * @return 返回List<Brand>类型结果。
     */
    @Override
    public List<Brand> findAll() {
        return brandMapper.selectAll();
    }

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param brand brand参数，详见调用方上下文。
     * @return 返回Brand类型结果。
     */
    @Override
    @Transactional
    public Brand create(Brand brand) {
        brandMapper.insert(brand);
        return brandMapper.selectById(brand.getId());
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param brand brand参数，详见调用方上下文。
     * @return 返回Brand类型结果。
     */
    @Override
    @Transactional
    public Brand update(Brand brand) {
        brandMapper.update(brand);
        return brandMapper.selectById(brand.getId());
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        brandMapper.delete(id);
    }
}