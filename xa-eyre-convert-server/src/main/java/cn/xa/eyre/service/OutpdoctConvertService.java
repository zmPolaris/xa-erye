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
import cn.xa.eyre.hisapi.OutpadmFeignClient;
import cn.xa.eyre.hub.domain.emrmonitor.EmrOutpatientRecord;
import cn.xa.eyre.hub.domain.emrreal.EmrActivityInfo;
import cn.xa.eyre.hub.service.SynchroEmrMonitorService;
import cn.xa.eyre.hub.service.SynchroEmrRealService;
import cn.xa.eyre.hub.staticvalue.HubCodeEnum;
import cn.xa.eyre.medrec.domain.PatMasterIndex;
import cn.xa.eyre.outpadm.domain.ClinicMaster;
import cn.xa.eyre.outpdoct.domain.OutpMr;
import cn.xa.eyre.system.dict.domain.DictDisDept;
import cn.xa.eyre.system.dict.domain.DictDiseaseIcd10;
import cn.xa.eyre.system.dict.mapper.DictDisDeptMapper;
import cn.xa.eyre.system.dict.mapper.DictDiseaseIcd10Mapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OutpdoctConvertService {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private MedrecFeignClient medrecFeignClient;
    @Autowired
    private OutpadmFeignClient outpadmFeignClient;
    @Autowired
    private DictDisDeptMapper dictDisDeptMapper;// 科室代码转码表
    @Autowired
    private SynchroEmrMonitorService synchroEmrMonitorService;
    @Autowired
    SynchroEmrRealService synchroEmrRealService;
    @Autowired
    private CommFeignClient commFeignClient;
    @Autowired
    private DictDiseaseIcd10Mapper dictDiseaseIcd10Mapper;// ICD10转码表

    public void outpMr(DBMessage dbMessage) {
        logger.debug("OUTP_MR表变更接口");
        logger.debug("OUTP_MR表变更需调用emrActivityInfo、emrOutpatientRecord同步接口");

        String httpMethod = null;
        OutpMr outpMr;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")){
            httpMethod = Constants.HTTP_METHOD_DELETE;
            outpMr = BeanUtil.toBean(dbMessage.getBeforeData(), OutpMr.class);
        }else {
            httpMethod = Constants.HTTP_METHOD_POST;
            outpMr = BeanUtil.toBean(dbMessage.getAfterData(), OutpMr.class);
        }

        if (StrUtil.isNotBlank(outpMr.getPatientId())){
            logger.debug("构造emrOutpatientRecord接口数据...");
            R<PatMasterIndex> medrecResult = medrecFeignClient.getMedrec(outpMr.getPatientId());
            R<ClinicMaster> outpadmResult = outpadmFeignClient.getClinicMaster(outpMr.getPatientId(), outpMr.getVisitNo(), DateUtils.dateTime(outpMr.getVisitDate()));
            if (R.SUCCESS == medrecResult.getCode() && R.SUCCESS == outpadmResult.getCode()
            && medrecResult.getData() != null && outpadmResult.getData() != null){
                EmrOutpatientRecord emrOutpatientRecord = new EmrOutpatientRecord();
                // ID使用OUTP_MR表联合主键拼接计算MD5
                String id = DigestUtil.md5Hex(DateUtils.dateTime(outpMr.getVisitDate()) + outpMr.getVisitNo() + outpMr.getOrdinal());
                emrOutpatientRecord.setId(id);
                emrOutpatientRecord.setPatientId(outpMr.getPatientId());
                emrOutpatientRecord.setSerialNumber(String.valueOf(outpMr.getVisitNo()));
                emrOutpatientRecord.setOutpatientDate(outpMr.getVisitDate());
                emrOutpatientRecord.setInitalDiagnosisCode(String.valueOf(1)); // 初诊标识，表中没有这个字段
                emrOutpatientRecord.setChiefComplaint(outpMr.getIllnessDesc());
                emrOutpatientRecord.setPresentIllnessHis(outpMr.getMedHistory());
                emrOutpatientRecord.setPastIllnessHis(outpMr.getAnamnesis());
                emrOutpatientRecord.setOperationHis(outpMr.getMedicalRecord());
                emrOutpatientRecord.setMaritalHis(outpMr.getMarrital());
                if(StrUtil.isNotBlank(outpMr.getIndividual())){
                    emrOutpatientRecord.setAllergyHisFlag("1");
                    emrOutpatientRecord.setAllergyHis(outpMr.getIndividual());
                }
                emrOutpatientRecord.setMenstrualHis(outpMr.getMenses());
                emrOutpatientRecord.setFamilyHis(outpMr.getFamilyIll());
                emrOutpatientRecord.setPhysicalExamination(outpMr.getBodyExam());
                emrOutpatientRecord.setStudiesSummaryResult(outpMr.getAssistExam());
                // 诊断代码
                if (StrUtil.isNotBlank(outpMr.getDiagnosisCodeMz1())){
                    DictDiseaseIcd10 dictDiseaseIcd10 = dictDiseaseIcd10Mapper.selectByEmrCode(outpMr.getDiagnosisCodeMz1());
                    if(dictDiseaseIcd10 == null){
                        emrOutpatientRecord.setWmDiagnosisCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                        emrOutpatientRecord.setWmDiagnosisName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                    }else {
                        emrOutpatientRecord.setWmDiagnosisCode(dictDiseaseIcd10.getHubCode());
                        emrOutpatientRecord.setWmDiagnosisName(dictDiseaseIcd10.getHubName());
                    }
                    if (StrUtil.isNotBlank(outpMr.getDiagnosisCodeMz2())){
                        DictDiseaseIcd10 dictDiseaseIcd102 = dictDiseaseIcd10Mapper.selectByEmrCode(outpMr.getDiagnosisCodeMz2());
                        if(dictDiseaseIcd10 == null){
                            emrOutpatientRecord.setWmDiagnosisCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                            emrOutpatientRecord.setWmDiagnosisName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                        }else {
                            emrOutpatientRecord.setWmDiagnosisCode(emrOutpatientRecord.getWmDiagnosisCode() + "||" + dictDiseaseIcd102.getHubCode());
                            emrOutpatientRecord.setWmDiagnosisName(emrOutpatientRecord.getWmDiagnosisName() + "||" + dictDiseaseIcd102.getHubName());
                        }
                    }
                }else {
                    emrOutpatientRecord.setWmDiagnosisCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                    emrOutpatientRecord.setWmDiagnosisName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                }
                emrOutpatientRecord.setTreatment(outpMr.getAdvice());

                PatMasterIndex patMasterIndex = medrecResult.getData();
                emrOutpatientRecord.setPatientName(patMasterIndex.getName());
                if (StrUtil.isBlank(patMasterIndex.getIdNo())){
                    emrOutpatientRecord.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                    emrOutpatientRecord.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                    emrOutpatientRecord.setIdCard("-");
                }else {
                    emrOutpatientRecord.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                    emrOutpatientRecord.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                    emrOutpatientRecord.setIdCard(patMasterIndex.getIdNo());
                }

                ClinicMaster clinicMaster = outpadmResult.getData();
                DictDisDept deptParam = new DictDisDept();
                deptParam.setStatus(Constants.STATUS_NORMAL);
                deptParam.setEmrCode(clinicMaster.getVisitDept());
                DictDisDept dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
                if (dictDisDept == null){
                    deptParam.setEmrName(null);
                    deptParam.setIsDefault(Constants.IS_DEFAULT);
                    dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
                }

                // 查询操作员ID
                if (StrUtil.isNotBlank(clinicMaster.getOperator())){
                    R<Users> user = commFeignClient.getUserByName(patMasterIndex.getOperator());
                    if (R.SUCCESS == user.getCode() && user.getData() != null){
                        emrOutpatientRecord.setOperatorId(user.getData().getUserId());
                    }
                }

                emrOutpatientRecord.setDeptCode(dictDisDept.getHubCode());
                emrOutpatientRecord.setDeptName(dictDisDept.getHubName());

                emrOutpatientRecord.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrOutpatientRecord.setOrgName(HubCodeEnum.ORG_CODE.getName());
                emrOutpatientRecord.setOperationTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, DateUtils.getNowDate()));
                synchroEmrMonitorService.syncEmrOutpatientRecord(emrOutpatientRecord, httpMethod);

                logger.debug("构造emrActivityInfo接口数据...");
                EmrActivityInfo emrActivityInfo = new EmrActivityInfo();
                emrActivityInfo.setId(id);
                emrActivityInfo.setPatientId(outpMr.getPatientId());
                String clinicType = clinicMaster.getClinicType();
                if (StrUtil.isNotBlank(clinicType)){
                    if (clinicType.contains("急诊号")){
                        emrActivityInfo.setActivityTypeCode(HubCodeEnum.DIAGNOSIS_ACTIVITIES_EMERGENCY.getCode());
                        emrActivityInfo.setActivityTypeName(HubCodeEnum.DIAGNOSIS_ACTIVITIES_EMERGENCY.getName());
                    } else {
                        emrActivityInfo.setActivityTypeCode(HubCodeEnum.DIAGNOSIS_ACTIVITIES_OUTPATIENT.getCode());
                        emrActivityInfo.setActivityTypeName(HubCodeEnum.DIAGNOSIS_ACTIVITIES_OUTPATIENT.getName());

                    }
                }
                emrActivityInfo.setSerialNumber(String.valueOf(outpMr.getVisitNo()));
                emrActivityInfo.setActivityTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, outpMr.getVisitDate()));
                String idNo = patMasterIndex.getIdNo();
                if (StringUtils.isNotBlank(idNo)) {
                    emrActivityInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                    emrActivityInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                    emrActivityInfo.setIdCard(idNo);
                } else {
                    emrActivityInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                    emrActivityInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                    emrActivityInfo.setIdCard("-");
                }
                emrActivityInfo.setPatientName(patMasterIndex.getName());

                emrActivityInfo.setChiefComplaint(outpMr.getIllnessDesc());
                emrActivityInfo.setPresentIllnessHis(outpMr.getMedHistory());
                emrActivityInfo.setPhysicalExamination(outpMr.getBodyExam());
                emrActivityInfo.setStudiesSummaryResult(outpMr.getAssistExam());
                emrActivityInfo.setDiagnoseTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, outpMr.getVisitDate()));

                // 诊断代码
                if (StrUtil.isNotBlank(outpMr.getDiagnosisCodeMz1())){
                    DictDiseaseIcd10 dictDiseaseIcd10 = dictDiseaseIcd10Mapper.selectByEmrCode(outpMr.getDiagnosisCodeMz1());
                    if(dictDiseaseIcd10 == null){
                        emrActivityInfo.setWmDiseaseCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                        emrActivityInfo.setWmDiseaseName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                    }else {
                        emrActivityInfo.setWmDiseaseCode(dictDiseaseIcd10.getHubCode());
                        emrActivityInfo.setWmDiseaseName(dictDiseaseIcd10.getHubName());
                    }
                    if (StrUtil.isNotBlank(outpMr.getDiagnosisCodeMz2())){
                        DictDiseaseIcd10 dictDiseaseIcd102 = dictDiseaseIcd10Mapper.selectByEmrCode(outpMr.getDiagnosisCodeMz2());
                        if(dictDiseaseIcd10 == null){
                            emrActivityInfo.setWmDiseaseCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                            emrActivityInfo.setWmDiseaseName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                        }else {
                            emrActivityInfo.setWmDiseaseCode(emrActivityInfo.getWmDiseaseCode() + "||" + dictDiseaseIcd102.getHubCode());
                            emrActivityInfo.setWmDiseaseName(emrActivityInfo.getWmDiseaseName() + "||" + dictDiseaseIcd102.getHubName());
                        }
                    }
                }else {
                    emrActivityInfo.setWmDiseaseCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                    emrActivityInfo.setWmDiseaseName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                }

                emrActivityInfo.setFillDoctor(patMasterIndex.getOperator());

                // 查询操作员ID
                if (StrUtil.isNotBlank(clinicMaster.getOperator())){
                    R<Users> user = commFeignClient.getUserByName(patMasterIndex.getOperator());
                    if (R.SUCCESS == user.getCode() && user.getData() != null){
                        emrActivityInfo.setOperatorId(user.getData().getUserId());
                    }
                }

                emrActivityInfo.setDeptCode(dictDisDept.getHubCode());
                emrActivityInfo.setDeptName(dictDisDept.getHubName());

                emrActivityInfo.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrActivityInfo.setOrgName(HubCodeEnum.ORG_CODE.getName());
                emrActivityInfo.setOperationTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, DateUtils.getNowDate()));
                synchroEmrRealService.syncEmrActivityInfo(emrActivityInfo, httpMethod);
            }else {
                logger.error("{}对应PatMasterIndex信息或ClinicMaster信息为空，无法同步", outpMr.getPatientId());
            }
        }else {
            logger.error("patientId为空，无法同步");
        }
    }
}
