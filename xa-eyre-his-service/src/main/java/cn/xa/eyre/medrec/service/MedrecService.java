package cn.xa.eyre.medrec.service;

import cn.xa.eyre.common.security.DataUtil;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.medrec.domain.*;
import cn.xa.eyre.medrec.mapper.*;
import cn.xa.eyre.pharmacy.domain.DrugPrescMaster;
import cn.xa.eyre.pharmacy.mapper.DrugPrescMasterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
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
    @Autowired
    private PatVisitMapper patVisitMapper;
    @Autowired
    private DrugPrescMasterMapper drugPrescMasterMapper;

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

    public OutpMrYb selectOutpMrYb(OutpMrYb outpMrYb) {
        return outpMrYbMapper.selectOutpMrYb(outpMrYb);
    }

    public PatVisit selectPatVisit(PatVisitKey patVisitKey) {
        return patVisitMapper.selectByPrimaryKey(patVisitKey);
    }

    public DrugPrescMaster selectDrugPrescMaster(DrugPrescMaster drugPrescMaster) {
        Date prescDate = drugPrescMaster.getPrescDate();
        String prescDateStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, prescDate);
        return drugPrescMasterMapper.selectDrugByPrescNoAndPrescDate(drugPrescMaster.getPrescNo(), prescDateStr);
    }
}
