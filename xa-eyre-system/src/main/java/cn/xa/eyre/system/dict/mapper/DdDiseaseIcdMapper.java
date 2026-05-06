package cn.xa.eyre.system.dict.mapper;

import cn.xa.eyre.system.dict.domain.DdDiseaseIcd;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DdDiseaseIcdMapper {
    int deleteByPrimaryKey(Long id);

    int insert(DdDiseaseIcd record);

    int insertSelective(DdDiseaseIcd record);

    DdDiseaseIcd selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DdDiseaseIcd record);

    int updateByPrimaryKey(DdDiseaseIcd record);

    List<DdDiseaseIcd> selectAll();

    DdDiseaseIcd selectByCode(@Param("code") String code);
}