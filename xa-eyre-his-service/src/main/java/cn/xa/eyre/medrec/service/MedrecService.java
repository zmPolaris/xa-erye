package cn.xa.eyre.medrec.service;

import cn.xa.eyre.medrec.domain.*;
import cn.xa.eyre.medrec.mapper.DiagnosisMapper;
import cn.xa.eyre.medrec.mapper.DiagnosticCategoryMapper;
import cn.xa.eyre.medrec.mapper.OutpMrYbMapper;
import cn.xa.eyre.medrec.mapper.PatMasterIndexMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedrecService {
    @Autowired
    private PatMasterIndexMapper patMasterIndexMapper;
    @Autowired
    private DiagnosticCategoryMapper diagnosticCategoryMapper;
    @Autowired
    private DiagnosisMapper diagnosisMapper;
    @Autowired
    private OutpMrYbMapper outpMrYbMapper;

    public PatMasterIndex selectPatMasterIndex(String patientId) {
        return patMasterIndexMapper.selectByPrimaryKey(patientId);
    }

    public List<PatMasterIndex> selectPatMasterIndexList(Integer num) {
        return patMasterIndexMapper.selectPatMasterIndexList(num);
    }

    public DiagnosticCategory selectDiagnosticCategory(DiagnosticCategoryKey diagnosticCategoryKey) {
        return diagnosticCategoryMapper.selectByPrimaryKey(diagnosticCategoryKey);
    }

    public Diagnosis selectDiagnosis(DiagnosisKey diagnosisKey) {
        return diagnosisMapper.selectByPrimaryKey(diagnosisKey);
    }

    public OutpMrYb selectOutpMrYb(OutpMrYbKey outpMrYbKey) {
        return outpMrYbMapper.selectByPrimaryKey(outpMrYbKey);
    }
}
