package cn.xa.eyre.oracle.medrec.mapper;

import cn.xa.eyre.oracle.medrec.domain.PatMasterIndex;

public interface PatMasterIndexMapper {
    int deleteByPrimaryKey(String patientId);

    int insert(PatMasterIndex record);

    int insertSelective(PatMasterIndex record);

    PatMasterIndex selectByPrimaryKey(String patientId);

    int updateByPrimaryKeySelective(PatMasterIndex record);

    int updateByPrimaryKey(PatMasterIndex record);
}