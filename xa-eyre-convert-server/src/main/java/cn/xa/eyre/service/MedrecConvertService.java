package cn.xa.eyre.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.xa.eyre.comm.domain.Users;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.domain.R;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.hisapi.CommFeignClient;
import cn.xa.eyre.hisapi.InpadmFeignClient;
import cn.xa.eyre.hisapi.MedrecFeignClient;
import cn.xa.eyre.hub.domain.emrmonitor.EmrAdmissionRecord;
import cn.xa.eyre.hub.domain.emrmonitor.EmrDailyCourse;
import cn.xa.eyre.hub.domain.emrmonitor.EmrDischargeInfo;
import cn.xa.eyre.hub.domain.emrmonitor.EmrFirstCourse;
import cn.xa.eyre.hub.domain.emrreal.EmrActivityInfo;
import cn.xa.eyre.hub.domain.emrreal.EmrPatientInfo;
import cn.xa.eyre.hub.service.SynchroEmrMonitorService;
import cn.xa.eyre.hub.service.SynchroEmrRealService;
import cn.xa.eyre.hub.staticvalue.HubCodeEnum;
import cn.xa.eyre.inpadm.domain.PatsInHospital;
import cn.xa.eyre.medrec.domain.Diagnosis;
import cn.xa.eyre.medrec.domain.PatMasterIndex;
import cn.xa.eyre.medrec.domain.PatVisit;
import cn.xa.eyre.system.dict.domain.DdNation;
import cn.xa.eyre.system.dict.domain.DictChargeType;
import cn.xa.eyre.system.dict.domain.DictDisDept;
import cn.xa.eyre.system.dict.mapper.DdNationMapper;
import cn.xa.eyre.system.dict.mapper.DictChargeTypeMapper;
import cn.xa.eyre.system.dict.mapper.DictDisDeptMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MedrecConvertService {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private SynchroEmrRealService synchroEmrRealService;
    @Autowired
    private DdNationMapper ddNationMapper;
    @Autowired
    private CommFeignClient commFeignClient;
    @Autowired
    private MedrecFeignClient medrecFeignClient;
    @Autowired
    private InpadmFeignClient inpadmFeignClient;
    @Autowired
    private SynchroEmrMonitorService synchroEmrMonitorService;
    @Autowired
    private DictDisDeptMapper dictDisDeptMapper;// 科室转码表
    @Autowired
    private DictChargeTypeMapper dictChargeTypeMapper;// 付费方式转码

    public void patMasterIndex(DBMessage dbMessage) {
        logger.debug("病人主索引表PAT_MASTER_INDEX变更接口");
        logger.debug("PAT_MASTER_INDEX变更需调用emrPatientInfo同步接口");
        EmrPatientInfo emrPatientInfo = new EmrPatientInfo();
        String httpMethod = null;
        PatMasterIndex patMasterIndex;
        Map<String, String> data;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")){
            httpMethod = Constants.HTTP_METHOD_DELETE;
            data = dbMessage.getBeforeData();
        }else {
            httpMethod = Constants.HTTP_METHOD_POST;
            data = dbMessage.getAfterData();
        }
        patMasterIndex = BeanUtil.toBeanIgnoreError(data, PatMasterIndex.class);
        patMasterIndex.setDateOfBirth(DateUtils.getLongDate(dbMessage.getAfterData().get("dateOfBirth")));
        patMasterIndex.setLastVisitDate(DateUtils.getLongDate(dbMessage.getAfterData().get("lastVisitDate")));
        patMasterIndex.setCreateDate(DateUtils.getLongDate(dbMessage.getAfterData().get("createDate")));
        patMasterIndex.setModifyTime(DateUtils.getLongDate(dbMessage.getAfterData().get("modifyTime")));
        patMasterIndex.setIdentityExpireDate(DateUtils.getLongDate(dbMessage.getAfterData().get("identityExpireDate")));

        logger.debug("构造emrPatientInfo接口数据...");
        // 构造请求参数
        emrPatientInfo.setId(patMasterIndex.getPatientId());
        emrPatientInfo.setPatientName(patMasterIndex.getName());
        if (StrUtil.isBlank(patMasterIndex.getIdNo())){
            emrPatientInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
            emrPatientInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
            emrPatientInfo.setIdCard("-");
        }else {
            emrPatientInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
            emrPatientInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
            emrPatientInfo.setIdCard(patMasterIndex.getIdNo());
        }
        emrPatientInfo.setGenderCode(patMasterIndex.getSexCode());
        emrPatientInfo.setGenderName(patMasterIndex.getSex());
        emrPatientInfo.setBirthDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, patMasterIndex.getDateOfBirth()));
        if(patMasterIndex.getCitizenship().equals("CN")){
            emrPatientInfo.setNationalityCode(HubCodeEnum.NATIONALITY_CODE.getCode());
            emrPatientInfo.setNationalityName(HubCodeEnum.NATIONALITY_CODE.getName());
        }
        DdNation ddNation = ddNationMapper.selectByName(patMasterIndex.getNation());
        if (ddNation != null){
            emrPatientInfo.setNationCode(ddNation.getCode());
            emrPatientInfo.setNationName(ddNation.getName());
        }else {
            emrPatientInfo.setNationCode(HubCodeEnum.NATION_CODE.getCode());
            emrPatientInfo.setNationName(HubCodeEnum.NATION_CODE.getName());
        }
        emrPatientInfo.setCurrentAddrCode(patMasterIndex.getMailingAreaCode4());
        emrPatientInfo.setCurrentAddrName(patMasterIndex.getMailingAddress());
        emrPatientInfo.setCurrentAddrDetail(patMasterIndex.getNextOfKinAddr());
        emrPatientInfo.setWorkunit(patMasterIndex.getWorkunit());
        if(patMasterIndex.getNextOfKin() != null){
            emrPatientInfo.setContacts(patMasterIndex.getNextOfKin());
            emrPatientInfo.setContactsTel(patMasterIndex.getNextOfKinPhone());
        }else {
            emrPatientInfo.setContacts(patMasterIndex.getGuardianName());
            emrPatientInfo.setContactsTel(patMasterIndex.getGuardianPhone());
        }
        emrPatientInfo.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
        emrPatientInfo.setOrgName(HubCodeEnum.ORG_CODE.getName());
        // 查询操作员ID
        if (StrUtil.isNotBlank(patMasterIndex.getOperator())){
            R<Users> user = commFeignClient.getUserByName(patMasterIndex.getOperator());
            if (R.SUCCESS == user.getCode() && user.getData() != null){
                emrPatientInfo.setOperatorId(user.getData().getUserId());
            }
        }

        emrPatientInfo.setOperationTime(DateUtils.getTime());
        synchroEmrRealService.syncEmrPatientInfo(emrPatientInfo, httpMethod);
    }

    public void diagnosis(DBMessage dbMessage) {
        logger.debug("诊断表DIAGNOSIS变更接口");
        logger.debug("DIAGNOSIS变更需调用emrFirstCourse、emrDailyCourse同步接口");
        String httpMethod = null;
        Diagnosis diagnosis;
        Map<String, String> data;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")){
            httpMethod = Constants.HTTP_METHOD_DELETE;
            data = dbMessage.getBeforeData();
        }else {
            httpMethod = Constants.HTTP_METHOD_POST;
            data = dbMessage.getAfterData();
        }
        diagnosis = BeanUtil.toBeanIgnoreError(data, Diagnosis.class);
        diagnosis.setDiagnosisDate(DateUtils.getLongDate(dbMessage.getAfterData().get("diagnosisDate")));

        R<PatMasterIndex> medrecResult = medrecFeignClient.getMedrec(diagnosis.getPatientId());
        R<PatsInHospital> hospitalResult = inpadmFeignClient.getPatsInHospital(diagnosis.getPatientId(), diagnosis.getVisitId());
        if (R.SUCCESS == hospitalResult.getCode() && R.SUCCESS == medrecResult.getCode()){
            EmrFirstCourse emrFirstCourse = new EmrFirstCourse();
            EmrDailyCourse emrDailyCourse = new EmrDailyCourse();
            // ID使用DIAGNOSIS表patientId、visitId、diagnosisDate拼接计算MD5
            String id = DigestUtil.md5Hex(diagnosis.getPatientId() + diagnosis.getVisitId() + DateUtils.dateTime(diagnosis.getDiagnosisDate()));

            if (diagnosis.getDiagnosisCode().equals(Constants.DIAGNOSIS_TYPE_CODE_RYCZ)){
                logger.debug("构造emrFirstCourse接口数据...");
                emrFirstCourse.setId(id);
                emrFirstCourse.setPatientId(diagnosis.getPatientId());
                emrFirstCourse.setSerialNumber(String.valueOf(diagnosis.getVisitId()));
                emrFirstCourse.setCreateDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, diagnosis.getDiagnosisDate()));
                emrFirstCourse.setPresentIllnessHis(diagnosis.getDiagnosisDesc());

                emrFirstCourse.setPatientName(medrecResult.getData().getName());
                if (StrUtil.isBlank(medrecResult.getData().getIdNo())){
                    emrFirstCourse.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                    emrFirstCourse.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                    emrFirstCourse.setIdCard("-");
                }else {
                    emrFirstCourse.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                    emrFirstCourse.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                    emrFirstCourse.setIdCard(medrecResult.getData().getIdNo());
                }

                emrFirstCourse.setWardNo(hospitalResult.getData().getWardCode());
                emrFirstCourse.setBedNo(String.valueOf(hospitalResult.getData().getBedNo()));
                // 治疗医生
                if (StrUtil.isNotBlank(hospitalResult.getData().getDoctorInCharge())){
                    R<Users> user = commFeignClient.getUserByName(hospitalResult.getData().getDoctorInCharge());
                    if (R.SUCCESS == user.getCode() && user.getData() != null){
                        emrFirstCourse.setResidentPhysicianId(user.getData().getUserId());
                    }
                }
                DictDisDept deptParam = new DictDisDept();
                deptParam.setStatus(Constants.STATUS_NORMAL);
                deptParam.setEmrCode(hospitalResult.getData().getDeptCode());
                DictDisDept dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
                if (dictDisDept == null){
                    deptParam.setEmrName(null);
                    deptParam.setIsDefault(Constants.IS_DEFAULT);
                    dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
                }

                emrFirstCourse.setDeptCode(dictDisDept.getHubCode());
                emrFirstCourse.setDeptName(dictDisDept.getHubName());

                emrFirstCourse.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrFirstCourse.setOrgName(HubCodeEnum.ORG_CODE.getName());
                emrFirstCourse.setOperationTime(DateUtils.getTime());

                synchroEmrMonitorService.syncEmrFirstCourse(emrFirstCourse, httpMethod);

                logger.debug("构造emrActivityInfo(首次病程)接口数据...");
                EmrActivityInfo emrActivityInfo = new EmrActivityInfo();
                emrActivityInfo.setId(id);
                emrActivityInfo.setPatientId(emrFirstCourse.getPatientId());
                emrActivityInfo.setActivityTypeCode(HubCodeEnum.DIAGNOSIS_ACTIVITIES_FIRST_COURSE.getCode());
                emrActivityInfo.setActivityTypeName(HubCodeEnum.DIAGNOSIS_ACTIVITIES_FIRST_COURSE.getName());
                emrActivityInfo.setSerialNumber(emrFirstCourse.getSerialNumber());
                emrActivityInfo.setActivityTime(emrFirstCourse.getCreateDate());
                emrActivityInfo.setIdCardTypeCode(emrFirstCourse.getIdCardTypeCode());
                emrActivityInfo.setIdCardTypeName(emrFirstCourse.getIdCardTypeName());
                emrActivityInfo.setIdCard(emrFirstCourse.getIdCard());
                emrActivityInfo.setPatientName(emrFirstCourse.getPatientName());
                emrActivityInfo.setChiefComplaint(emrFirstCourse.getChiefComplaint());
                emrActivityInfo.setPresentIllnessHis(emrFirstCourse.getPresentIllnessHis());
                emrActivityInfo.setDiagnoseTime(emrFirstCourse.getCreateDate());
                emrActivityInfo.setWmDiseaseCode(emrFirstCourse.getWmInitalDiagnosisCode());
                emrActivityInfo.setWmDiseaseName(emrFirstCourse.getWmInitalDiagnosisName());
                emrActivityInfo.setFillDoctor(hospitalResult.getData().getDoctorInCharge());
                emrActivityInfo.setOperatorId(emrFirstCourse.getOperatorId());
                emrActivityInfo.setDeptCode(emrFirstCourse.getDeptCode());
                emrActivityInfo.setDeptName(emrFirstCourse.getDeptName());
                emrActivityInfo.setOrgCode(emrFirstCourse.getOrgCode());
                emrActivityInfo.setOrgName(emrFirstCourse.getOrgName());
                emrActivityInfo.setOperationTime(emrFirstCourse.getOperationTime());
                synchroEmrRealService.syncEmrActivityInfo(emrActivityInfo, httpMethod);


            }else if (diagnosis.getDiagnosisCode().equals(Constants.DIAGNOSIS_TYPE_CODE_ZYZD)){
                logger.debug("构造emrDailyCourse接口数据...");
                emrDailyCourse.setId(id);
                emrDailyCourse.setPatientId(diagnosis.getPatientId());
                emrDailyCourse.setSerialNumber(String.valueOf(diagnosis.getVisitId()));
                emrDailyCourse.setCreateDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, diagnosis.getDiagnosisDate()));
                emrDailyCourse.setCourse(diagnosis.getDiagnosisDesc());

                emrDailyCourse.setPatientName(medrecResult.getData().getName());
                if (StrUtil.isBlank(medrecResult.getData().getIdNo())){
                    emrDailyCourse.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                    emrDailyCourse.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                    emrDailyCourse.setIdCard("-");
                }else {
                    emrDailyCourse.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                    emrDailyCourse.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                    emrDailyCourse.setIdCard(medrecResult.getData().getIdNo());
                }

                emrDailyCourse.setWardNo(hospitalResult.getData().getWardCode());
                emrDailyCourse.setBedNo(String.valueOf(hospitalResult.getData().getBedNo()));
                DictDisDept deptParam = new DictDisDept();
                deptParam.setStatus(Constants.STATUS_NORMAL);
                deptParam.setEmrCode(hospitalResult.getData().getDeptCode());
                DictDisDept dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
                if (dictDisDept == null){
                    deptParam.setEmrName(null);
                    deptParam.setIsDefault(Constants.IS_DEFAULT);
                    dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
                }

                emrDailyCourse.setDeptCode(dictDisDept.getHubCode());
                emrDailyCourse.setDeptName(dictDisDept.getHubName());

                emrDailyCourse.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrDailyCourse.setOrgName(HubCodeEnum.ORG_CODE.getName());
                emrDailyCourse.setOperationTime(DateUtils.getTime());

                synchroEmrMonitorService.syncEmrDailyCourse(emrDailyCourse, httpMethod);

                logger.debug("构造emrActivityInfo(日常病程)接口数据...");
                EmrActivityInfo emrActivityInfo = new EmrActivityInfo();
                emrActivityInfo.setId(id);
                emrActivityInfo.setPatientId(emrDailyCourse.getPatientId());
                emrActivityInfo.setActivityTypeCode(HubCodeEnum.DIAGNOSIS_ACTIVITIES_DAILY_COURSE.getCode());
                emrActivityInfo.setActivityTypeName(HubCodeEnum.DIAGNOSIS_ACTIVITIES_DAILY_COURSE.getName());
                emrActivityInfo.setSerialNumber(emrDailyCourse.getSerialNumber());
                emrActivityInfo.setActivityTime(emrDailyCourse.getCreateDate());
                emrActivityInfo.setIdCardTypeCode(emrDailyCourse.getIdCardTypeCode());
                emrActivityInfo.setIdCardTypeName(emrDailyCourse.getIdCardTypeName());
                emrActivityInfo.setIdCard(emrDailyCourse.getIdCard());
                emrActivityInfo.setPatientName(emrDailyCourse.getPatientName());
                emrActivityInfo.setDiagnoseTime(emrDailyCourse.getCreateDate());
                emrActivityInfo.setWmDiseaseCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                emrActivityInfo.setWmDiseaseName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                emrActivityInfo.setFillDoctor(hospitalResult.getData().getDoctorInCharge());
                emrActivityInfo.setOperatorId(emrDailyCourse.getOperatorId());
                emrActivityInfo.setDeptCode(emrDailyCourse.getDeptCode());
                emrActivityInfo.setDeptName(emrDailyCourse.getDeptName());
                emrActivityInfo.setOrgCode(emrDailyCourse.getOrgCode());
                emrActivityInfo.setOrgName(emrDailyCourse.getOrgName());
                emrActivityInfo.setOperationTime(emrDailyCourse.getOperationTime());
                synchroEmrRealService.syncEmrActivityInfo(emrActivityInfo, httpMethod);
            }

        }else {
            logger.error("{}对应PatMasterIndex或PatsInHospital信息为空，无法同步", diagnosis.getPatientId());
        }
    }

    public void patVisit(DBMessage dbMessage) {
        logger.debug("住院表PAT_VISIT变更接口");
        logger.debug("PAT_VISIT变更需调用emrAdmissionRecord、emrDischargeInfo同步接口");
        String httpMethod = null;
        PatVisit patVisit;
        Map<String, String> data;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")){
            httpMethod = Constants.HTTP_METHOD_DELETE;
            data = dbMessage.getBeforeData();
        }else {
            httpMethod = Constants.HTTP_METHOD_POST;
            data = dbMessage.getAfterData();
        }
        patVisit = BeanUtil.toBeanIgnoreError(data, PatVisit.class);
        patVisit.setAdmissionDateTime(DateUtils.getLongDate(dbMessage.getAfterData().get("admissionDateTime")));
        patVisit.setDischargeDateTime(DateUtils.getLongDate(dbMessage.getAfterData().get("dischargeDateTime")));
        patVisit.setConsultingDate(DateUtils.getLongDate(dbMessage.getAfterData().get("consultingDate")));

        R<PatMasterIndex> medrecResult = medrecFeignClient.getMedrec(patVisit.getPatientId());
        R<PatsInHospital> hospitalResult = inpadmFeignClient.getPatsInHospital(patVisit.getPatientId(), patVisit.getVisitId());
        if (R.SUCCESS == hospitalResult.getCode() && R.SUCCESS == medrecResult.getCode()){
            EmrAdmissionRecord emrAdmissionRecord = new EmrAdmissionRecord();
            EmrDischargeInfo emrDischargeInfo = new EmrDischargeInfo();
            // ID使用PAT_VISIT表patientId、visitId拼接计算MD5
            String id = DigestUtil.md5Hex(patVisit.getPatientId() + patVisit.getVisitId());
            logger.debug("构造emrAdmissionRecord接口数据...");
            emrAdmissionRecord.setId(id);
            emrAdmissionRecord.setPatientId(patVisit.getPatientId());
            emrAdmissionRecord.setSerialNumber(String.valueOf(patVisit.getVisitId()));
            if (StrUtil.isNotBlank(patVisit.getChargeType())){
                DictChargeType dictChargeType = dictChargeTypeMapper.selectByEmrCode(patVisit.getChargeType());
                if (dictChargeType == null){
                    emrAdmissionRecord.setPayMethodCode(HubCodeEnum.PAY_TYPE_OTHER.getCode());
                    emrAdmissionRecord.setPayMethodName(HubCodeEnum.PAY_TYPE_OTHER.getName());
                }else {
                    emrAdmissionRecord.setPayMethodCode(dictChargeType.getHubCode());
                    emrAdmissionRecord.setPayMethodName(dictChargeType.getHubName());
                }
            }
            emrAdmissionRecord.setAdmissionNum(String.valueOf(patVisit.getVisitId()));
            emrAdmissionRecord.setRegNo(patVisit.getPatientId());
            emrAdmissionRecord.setAdmissionDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, patVisit.getAdmissionDateTime()));
            // 住院医师
            if (StrUtil.isNotBlank(patVisit.getDoctorInCharge())){
                R<Users> user = commFeignClient.getUserByName(patVisit.getDoctorInCharge());
                if (R.SUCCESS == user.getCode() && user.getData() != null){
                    emrAdmissionRecord.setResidentPhysicianId(user.getData().getUserId());
                }
            }
            // 主治医师
            if (StrUtil.isNotBlank(patVisit.getAttendingDoctor())){
                R<Users> user = commFeignClient.getUserByName(patVisit.getAttendingDoctor());
                if (R.SUCCESS == user.getCode() && user.getData() != null){
                    emrAdmissionRecord.setChiefPhysicianId(user.getData().getUserId());
                }
            }
            DictDisDept deptParam = new DictDisDept();
            deptParam.setStatus(Constants.STATUS_NORMAL);
            deptParam.setEmrCode(patVisit.getDeptAdmissionTo());
            DictDisDept dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
            if (dictDisDept == null){
                deptParam.setEmrName(null);
                deptParam.setIsDefault(Constants.IS_DEFAULT);
                dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
            }
            emrAdmissionRecord.setDeptCode(dictDisDept.getHubCode());
            emrAdmissionRecord.setDeptName(dictDisDept.getHubName());
            emrAdmissionRecord.setAdmissionDeptCode(dictDisDept.getHubCode());
            emrAdmissionRecord.setAdmissionDeptName(dictDisDept.getHubName());
            if (StrUtil.isNotBlank(patVisit.getAlergyDrugs())){
                emrAdmissionRecord.setAllergyCode("1");
                emrAdmissionRecord.setAllergyDrug(patVisit.getAlergyDrugs());
            }else {
                emrAdmissionRecord.setAllergyCode("0");
            }
            if (patVisit.getAutopsyIndicator() != null){
                emrAdmissionRecord.setAutopsyCode(String.valueOf(patVisit.getAutopsyIndicator()));
            }

            emrAdmissionRecord.setPatientName(medrecResult.getData().getName());
            if (StrUtil.isBlank(medrecResult.getData().getIdNo())){
                emrAdmissionRecord.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                emrAdmissionRecord.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                emrAdmissionRecord.setIdCard("-");
            }else {
                emrAdmissionRecord.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                emrAdmissionRecord.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                emrAdmissionRecord.setIdCard(medrecResult.getData().getIdNo());
            }
            emrAdmissionRecord.setWardNo(hospitalResult.getData().getWardCode());

            if (patVisit.getDischargeDateTime() != null){
                emrAdmissionRecord.setDischargeDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, patVisit.getDischargeDateTime()));
                // 计算住院天数
                long betweenDay = DateUtil.between(patVisit.getAdmissionDateTime(), patVisit.getDischargeDateTime(), DateUnit.DAY);
                emrAdmissionRecord.setAdmissionDays(String.valueOf(betweenDay));
                emrAdmissionRecord.setDischargeWard(hospitalResult.getData().getWardCode());
                deptParam.setEmrCode(patVisit.getDeptDischargeFrom());
                dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
                if (dictDisDept == null){
                    deptParam.setEmrName(null);
                    deptParam.setIsDefault(Constants.IS_DEFAULT);
                    dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
                }
                emrAdmissionRecord.setDischargeDeptCode(dictDisDept.getHubCode());
                emrAdmissionRecord.setDischargeDeptName(dictDisDept.getHubName());
            }

            emrAdmissionRecord.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
            emrAdmissionRecord.setOrgName(HubCodeEnum.ORG_CODE.getName());
            emrAdmissionRecord.setOperationTime(DateUtils.getTime());
            synchroEmrMonitorService.syncEmrAdmissionRecord(emrAdmissionRecord, httpMethod);

            if (patVisit.getDischargeDateTime() != null){
                // 出院时间不为空，同步出院记录
                logger.debug("构造emrDischargeInfo接口数据...");
                emrDischargeInfo.setId(id);
                emrDischargeInfo.setPatientId(patVisit.getPatientId());
                emrDischargeInfo.setSerialNumber(String.valueOf(patVisit.getVisitId()));
                emrDischargeInfo.setDischargeDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, patVisit.getDischargeDateTime()));
                emrDischargeInfo.setAdmissionDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, patVisit.getAdmissionDateTime()));
                // 住院医师
                if (StrUtil.isNotBlank(patVisit.getDoctorInCharge())){
                    R<Users> user = commFeignClient.getUserByName(patVisit.getDoctorInCharge());
                    if (R.SUCCESS == user.getCode() && user.getData() != null){
                        emrDischargeInfo.setResidentPhysicianId(user.getData().getUserId());
                    }
                }
                // 主治医师
                if (StrUtil.isNotBlank(patVisit.getAttendingDoctor())){
                    R<Users> user = commFeignClient.getUserByName(patVisit.getAttendingDoctor());
                    if (R.SUCCESS == user.getCode() && user.getData() != null){
                        emrDischargeInfo.setChiefPhysicianId(user.getData().getUserId());
                    }
                }

                emrDischargeInfo.setPatientName(medrecResult.getData().getName());
                if (StrUtil.isBlank(medrecResult.getData().getIdNo())){
                    emrDischargeInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                    emrDischargeInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                    emrDischargeInfo.setIdCard("-");
                }else {
                    emrDischargeInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                    emrDischargeInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                    emrDischargeInfo.setIdCard(medrecResult.getData().getIdNo());
                }

                emrDischargeInfo.setWardNo(hospitalResult.getData().getWardCode());
                emrDischargeInfo.setBedNo(String.valueOf(hospitalResult.getData().getBedNo()));
                emrDischargeInfo.setAdmissionDesc(hospitalResult.getData().getDiagnosis());

                emrDischargeInfo.setDeptCode(dictDisDept.getHubCode());
                emrDischargeInfo.setDeptName(dictDisDept.getHubName());

                emrDischargeInfo.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrDischargeInfo.setOrgName(HubCodeEnum.ORG_CODE.getName());
                emrDischargeInfo.setOperationTime(DateUtils.getTime());
                synchroEmrMonitorService.syncEmrDischargeInfo(emrDischargeInfo, httpMethod);

                logger.debug("构造emrActivityInfo(出院)接口数据...");
                EmrActivityInfo emrActivityInfo = new EmrActivityInfo();
                emrActivityInfo.setId(id);
                emrActivityInfo.setPatientId(emrDischargeInfo.getPatientId());
                emrActivityInfo.setActivityTypeCode(HubCodeEnum.DIAGNOSIS_ACTIVITIES_HOSPITALIZATION.getCode());
                emrActivityInfo.setActivityTypeName(HubCodeEnum.DIAGNOSIS_ACTIVITIES_HOSPITALIZATION.getName());
                emrActivityInfo.setSerialNumber(emrDischargeInfo.getSerialNumber());
                emrActivityInfo.setActivityTime(emrDischargeInfo.getDischargeDate());
                emrActivityInfo.setIdCardTypeCode(emrDischargeInfo.getIdCardTypeCode());
                emrActivityInfo.setIdCardTypeName(emrDischargeInfo.getIdCardTypeName());
                emrActivityInfo.setIdCard(emrDischargeInfo.getIdCard());
                emrActivityInfo.setPatientName(emrDischargeInfo.getPatientName());
                emrActivityInfo.setPresentIllnessHis(emrDischargeInfo.getDischargeSymptomsSigns());
                emrActivityInfo.setStudiesSummaryResult(emrDischargeInfo.getStudiesSummaryResult());
                emrActivityInfo.setDiagnoseTime(emrDischargeInfo.getAdmissionDate());
                emrActivityInfo.setWmDiseaseCode(emrDischargeInfo.getDischargeDiagnosisCode());
                emrActivityInfo.setWmDiseaseName(emrDischargeInfo.getDischargeDiagnosisName());
                emrActivityInfo.setFillDoctor(patVisit.getAttendingDoctor());
                emrActivityInfo.setOperatorId(emrDischargeInfo.getOperatorId());
                emrActivityInfo.setDeptCode(emrDischargeInfo.getDeptCode());
                emrActivityInfo.setDeptName(emrDischargeInfo.getDeptName());
                emrActivityInfo.setOrgCode(emrDischargeInfo.getOrgCode());
                emrActivityInfo.setOrgName(emrDischargeInfo.getOrgName());
                emrActivityInfo.setOperationTime(emrDischargeInfo.getOperationTime());
                synchroEmrRealService.syncEmrActivityInfo(emrActivityInfo, httpMethod);
            }

        }else {
            logger.error("{}对应PatMasterIndex或PatsInHospital信息为空，无法同步", patVisit.getPatientId());
        }
    }
}
