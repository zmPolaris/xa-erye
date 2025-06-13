package cn.xa.eyre.medrec.service;

import cn.xa.eyre.oracle.medrec.domain.PatMasterIndex;
import cn.xa.eyre.oracle.medrec.mapper.PatMasterIndexMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MedrecService {
    @Autowired
    private PatMasterIndexMapper patMasterIndexMapper;

    public PatMasterIndex selectPatMasterIndex(String patientId) {
        return patMasterIndexMapper.selectByPrimaryKey(patientId);
    }
}
