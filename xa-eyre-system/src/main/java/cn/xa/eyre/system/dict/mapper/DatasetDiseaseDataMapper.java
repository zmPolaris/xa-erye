package cn.xa.eyre.system.dict.mapper;

import cn.xa.eyre.system.dict.domain.DatasetDiseaseData;

public interface DatasetDiseaseDataMapper {
    int insert(DatasetDiseaseData record);

    int insertSelective(DatasetDiseaseData record);
}