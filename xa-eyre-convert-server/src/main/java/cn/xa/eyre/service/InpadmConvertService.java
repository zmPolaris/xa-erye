package cn.xa.eyre.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.xa.eyre.comm.domain.Users;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.domain.R;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.hisapi.CommFeignClient;
import cn.xa.eyre.hisapi.MedrecFeignClient;
import cn.xa.eyre.hisapi.OutpdoctFeignClient;
import cn.xa.eyre.hub.domain.emrmonitor.EmrAdmissionInfo;
import cn.xa.eyre.hub.service.SynchroEmrMonitorService;
import cn.xa.eyre.hub.staticvalue.HubCodeEnum;
import cn.xa.eyre.inpadm.domain.PatsInHospital;
import cn.xa.eyre.medrec.domain.PatMasterIndex;
import cn.xa.eyre.outpadm.domain.ClinicMaster;
import cn.xa.eyre.outpdoct.domain.OutpMr;
import cn.xa.eyre.system.dict.domain.DatasetDiseaseData;
import cn.xa.eyre.system.dict.domain.DictDisDept;
import cn.xa.eyre.system.dict.domain.DictDiseaseIcd10;
import cn.xa.eyre.system.dict.mapper.DatasetDiseaseDataMapper;
import cn.xa.eyre.system.dict.mapper.DictDisDeptMapper;
import cn.xa.eyre.system.dict.mapper.DictDiseaseIcd10Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InpadmConvertService {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private MedrecFeignClient medrecFeignClient;
    @Autowired
    private SynchroEmrMonitorService synchroEmrMonitorService;
    @Autowired
    private DictDisDeptMapper dictDisDeptMapper;// 转码表
    @Autowired
    private CommFeignClient commFeignClient;
    @Autowired
    private OutpdoctFeignClient outpdoctFeignClient;
    @Autowired
    private DictDiseaseIcd10Mapper dictDiseaseIcd10Mapper;// ICD10转码表
    @Autowired
    private DatasetDiseaseDataMapper datasetDiseaseDataMapper;// 传染病

    public void patsInHospital(DBMessage dbMessage) {
        logger.debug("PATS_IN_HOSPITAL表变更接口");
        logger.debug("PATS_IN_HOSPITAL表变更需调用emrActivityInfo、emrAdmissionInfo同步接口");

        String httpMethod = null;
        PatsInHospital patsInHospital;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")){
            httpMethod = Constants.HTTP_METHOD_DELETE;
            patsInHospital = BeanUtil.toBean(dbMessage.getBeforeData(), PatsInHospital.class);
        }else {
            httpMethod = Constants.HTTP_METHOD_POST;
            patsInHospital = BeanUtil.toBean(dbMessage.getAfterData(), PatsInHospital.class);
        }


        logger.debug("构造emrAdmissionInfo接口数据...");
        R<PatMasterIndex> medrecResult = medrecFeignClient.getMedrec(patsInHospital.getPatientId());
        if (R.SUCCESS == medrecResult.getCode()){
            EmrAdmissionInfo emrAdmissionInfo = new EmrAdmissionInfo();
            // ID使用OUTP_MR表patientId、visitId拼接计算MD5
            String id = DigestUtil.md5Hex(patsInHospital.getPatientId() + patsInHospital.getVisitId());
            emrAdmissionInfo.setId(id);
            emrAdmissionInfo.setPatientId(patsInHospital.getPatientId());
            emrAdmissionInfo.setSerialNumber(String.valueOf(patsInHospital.getVisitId()));
            emrAdmissionInfo.setWardNo(patsInHospital.getWardCode());
            emrAdmissionInfo.setBedNo(String.valueOf(patsInHospital.getBedNo()));
            emrAdmissionInfo.setAdmissionDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, patsInHospital.getAdmissionDateTime()));
            // 治疗医生
            if (StrUtil.isNotBlank(patsInHospital.getDoctorInCharge())){
                R<Users> user = commFeignClient.getUserByName(patsInHospital.getDoctorInCharge());
                if (R.SUCCESS == user.getCode() && user.getData() != null){
                    emrAdmissionInfo.setResidentPhysicianId(user.getData().getUserId());
                    emrAdmissionInfo.setChiefPhysicianId(user.getData().getUserId());
                }
            }

            PatMasterIndex patMasterIndex = medrecResult.getData();
            emrAdmissionInfo.setPatientName(patMasterIndex.getName());
            if (StrUtil.isBlank(patMasterIndex.getIdNo())){
                emrAdmissionInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                emrAdmissionInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                emrAdmissionInfo.setIdCard("-");
            }else {
                emrAdmissionInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                emrAdmissionInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                emrAdmissionInfo.setIdCard(patMasterIndex.getIdNo());
            }

            DictDisDept deptParam = new DictDisDept();
            deptParam.setStatus(Constants.STATUS_NORMAL);
            deptParam.setEmrCode(patsInHospital.getDeptCode());
            DictDisDept dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
            if (dictDisDept == null){
                deptParam.setEmrName(null);
                deptParam.setIsDefault(Constants.IS_DEFAULT);
                dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
            }

            emrAdmissionInfo.setDeptCode(dictDisDept.getHubCode());
            emrAdmissionInfo.setDeptName(dictDisDept.getHubName());

            // 查询7天内门诊信息补充病情
            OutpMr outpMr = new OutpMr();
            outpMr.setPatientId(patsInHospital.getPatientId());
            outpMr.setBeginVisitDate(DateUtils.addDays(patsInHospital.getAdmissionDateTime(), -7));
            outpMr.setEndVisitDate(patsInHospital.getAdmissionDateTime());
            R<List<OutpMr>> mrResult = outpdoctFeignClient.getOutpMrByCondition(outpMr);
            if (R.SUCCESS == mrResult.getCode()){
                outpMr = mrResult.getData().get(0);
                emrAdmissionInfo.setChiefComplaint(outpMr.getIllnessDesc());
                emrAdmissionInfo.setPresentIllnessHis(outpMr.getMedHistory());
                emrAdmissionInfo.setPastIllnessHis(outpMr.getAnamnesis());
                emrAdmissionInfo.setOperationHis(outpMr.getMedicalRecord());
                emrAdmissionInfo.setMaritalHis(outpMr.getMarrital());
                if(StrUtil.isNotBlank(outpMr.getIndividual())){
                    emrAdmissionInfo.setAllergyHis(outpMr.getIndividual());
                }
                emrAdmissionInfo.setMenstrualHis(outpMr.getMenses());
                emrAdmissionInfo.setFamilyHis(outpMr.getFamilyIll());
                emrAdmissionInfo.setPhysicalExamination(outpMr.getBodyExam());
                emrAdmissionInfo.setStudiesSummaryResult(outpMr.getAssistExam());
                // 诊断代码
                List<String> diseases = new ArrayList<>();
                if (StrUtil.isNotBlank(outpMr.getDiagnosisCodeMz1())){
                    DictDiseaseIcd10 dictDiseaseIcd10 = dictDiseaseIcd10Mapper.selectByEmrCode(outpMr.getDiagnosisCodeMz1());
                    if(dictDiseaseIcd10 == null){
                        emrAdmissionInfo.setWmInitalDiagnosisCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                        emrAdmissionInfo.setWmInitalDiagnosisName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                    }else {
                        diseases.add(dictDiseaseIcd10.getHubCode());
                        emrAdmissionInfo.setWmInitalDiagnosisCode(dictDiseaseIcd10.getHubCode());
                        emrAdmissionInfo.setWmInitalDiagnosisName(dictDiseaseIcd10.getHubName());
                    }
                    if (StrUtil.isNotBlank(outpMr.getDiagnosisCodeMz2())){
                        DictDiseaseIcd10 dictDiseaseIcd102 = dictDiseaseIcd10Mapper.selectByEmrCode(outpMr.getDiagnosisCodeMz2());
                        if(dictDiseaseIcd10 == null){
                            emrAdmissionInfo.setWmInitalDiagnosisCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                            emrAdmissionInfo.setWmInitalDiagnosisName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                        }else {
                            diseases.add(dictDiseaseIcd102.getHubCode());
                            emrAdmissionInfo.setWmInitalDiagnosisCode(emrAdmissionInfo.getWmInitalDiagnosisCode() + "||" + dictDiseaseIcd102.getHubCode());
                            emrAdmissionInfo.setWmInitalDiagnosisName(emrAdmissionInfo.getWmInitalDiagnosisName() + "||" + dictDiseaseIcd102.getHubName());
                        }
                    }
                }
                emrAdmissionInfo.setTreatment(outpMr.getAdvice());
                emrAdmissionInfo.setInitalDiagnosisDate(outpMr.getVisitDate());
                if (StrUtil.isNotBlank(outpMr.getDoctor())){
                    R<Users> user = commFeignClient.getUserByName(patsInHospital.getDoctorInCharge());
                    if (R.SUCCESS == user.getCode() && user.getData() != null){
                        emrAdmissionInfo.setVisitingPhysicianId(user.getData().getUserId());
                    }
                }
                // 查询是否传染病
                if (!diseases.isEmpty()){
                    List<DatasetDiseaseData> datasetDiseaseDatas = datasetDiseaseDataMapper.selectByCodes(diseases.toArray(new String[diseases.size()]));
                    if (!datasetDiseaseDatas.isEmpty()){
                        emrAdmissionInfo.setInfectionCode("1");
                    }
                }
            }

            emrAdmissionInfo.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
            emrAdmissionInfo.setOrgName(HubCodeEnum.ORG_CODE.getName());

            synchroEmrMonitorService.syncEmrAdmissionInfo(emrAdmissionInfo, httpMethod);
        }else {
            logger.error("对应PatMasterIndex信息为空，无法同步");
        }
    }
}
