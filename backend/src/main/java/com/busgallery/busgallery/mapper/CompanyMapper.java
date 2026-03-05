package com.busgallery.busgallery.mapper;

import com.busgallery.busgallery.entity.Company;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompanyMapper {

    Company selectById(@Param("id") Long id);

    Company selectByName(@Param("name") String name);

    List<Company> selectByRegionId(@Param("regionId") Long regionId);

    List<Company> selectAll();

    int insert(Company company);

    int update(Company company);

    int delete(@Param("id") Long id);
}