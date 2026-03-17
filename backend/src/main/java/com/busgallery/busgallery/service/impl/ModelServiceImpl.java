package com.busgallery.busgallery.service.impl;

import com.busgallery.busgallery.entity.Model;
import com.busgallery.busgallery.mapper.BrandMapper;
import com.busgallery.busgallery.mapper.ModelMapper;
import com.busgallery.busgallery.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ModelServiceImpl类用于封装ModelServiceImpl相关的领域职责（所在包：com.busgallery.busgallery.service.impl）。
 */
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelMapper modelMapper;
    private final BrandMapper brandMapper;

    /**
     * findById方法用于处理findById相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    @Override
    public Model findById(Long id) {
        Model model = modelMapper.selectById(id);
        enrich(model);
        return model;
    }

    /**
     * findAll方法用于处理findAll相关的业务逻辑。
     * @return 返回List<Model>类型结果。
     */
    @Override
    public List<Model> findAll() {
        List<Model> models = modelMapper.selectAll();
        models.forEach(this::enrich);
        return models;
    }

    /**
     * findByBrand方法用于处理findByBrand相关的业务逻辑。
     * @param brandId brandId参数，详见调用方上下文。
     * @return 返回List<Model>类型结果。
     */
    @Override
    public List<Model> findByBrand(Long brandId) {
        List<Model> models = modelMapper.selectByBrandId(brandId);
        models.forEach(this::enrich);
        return models;
    }

    /**
     * create方法用于处理create相关的业务逻辑。
     * @param model model参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    @Override
    @Transactional
    public Model create(Model model) {
        modelMapper.insert(model);
        Model saved = modelMapper.selectById(model.getId());
        enrich(saved);
        return saved;
    }

    /**
     * update方法用于处理update相关的业务逻辑。
     * @param model model参数，详见调用方上下文。
     * @return 返回Model类型结果。
     */
    @Override
    @Transactional
    public Model update(Model model) {
        modelMapper.update(model);
        Model saved = modelMapper.selectById(model.getId());
        enrich(saved);
        return saved;
    }

    /**
     * delete方法用于处理delete相关的业务逻辑。
     * @param id id参数，详见调用方上下文。
     * @return 无返回值。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        modelMapper.delete(id);
    }

    /**
     * enrich方法用于处理enrich相关的业务逻辑。
     * @param model model参数，详见调用方上下文。
     * @return 无返回值。
     */
    private void enrich(Model model) {
        if (model == null) {
            return;
        }
        if (model.getBrand() != null && model.getBrand().getId() != null) {
            model.setBrand(brandMapper.selectById(model.getBrand().getId()));
        }
    }
}
