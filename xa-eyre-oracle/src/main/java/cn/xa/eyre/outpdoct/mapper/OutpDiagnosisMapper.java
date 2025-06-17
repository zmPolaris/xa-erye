package cn.xa.eyre.outpdoct.mapper;

import cn.xa.eyre.outpdoct.domain.OutpDiagnosis;
import cn.xa.eyre.outpdoct.domain.OutpDiagnosisKey;

public interface OutpDiagnosisMapper {
    int deleteByPrimaryKey(OutpDiagnosisKey key);

    int insert(OutpDiagnosis record);

    int insertSelective(OutpDiagnosis record);

    OutpDiagnosis selectByPrimaryKey(OutpDiagnosisKey key);

    int updateByPrimaryKeySelective(OutpDiagnosis record);

    int updateByPrimaryKey(OutpDiagnosis record);
}