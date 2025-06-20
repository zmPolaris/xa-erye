package cn.xa.eyre.medrec.service;

import cn.xa.eyre.medrec.domain.PatMasterIndex;
import cn.xa.eyre.medrec.mapper.PatMasterIndexMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedrecService {
    @Autowired
    private PatMasterIndexMapper patMasterIndexMapper;

    public PatMasterIndex selectPatMasterIndex(String patientId) {
        return patMasterIndexMapper.selectByPrimaryKey(patientId);
    }

    public List<PatMasterIndex> selectPatMasterIndexList(Integer num) {
        return patMasterIndexMapper.selectPatMasterIndexList(num);
    }
}
