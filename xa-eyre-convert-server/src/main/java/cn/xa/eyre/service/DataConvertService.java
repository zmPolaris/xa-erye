package cn.xa.eyre.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.xa.eyre.comm.domain.Users;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.domain.R;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.common.utils.FuzzyMatcher;
import cn.xa.eyre.common.utils.StringUtils;
import cn.xa.eyre.hisapi.*;
import cn.xa.eyre.hub.domain.emrmonitor.EmrDailyCourse;
import cn.xa.eyre.hub.domain.emrmonitor.EmrFirstCourse;
import cn.xa.eyre.hub.domain.emrmonitor.EmrOutpatientRecord;
import cn.xa.eyre.hub.domain.emrreal.EmrActivityInfo;
import cn.xa.eyre.hub.domain.emrreal.EmrPatientInfo;
import cn.xa.eyre.hub.service.SynchroBaseService;
import cn.xa.eyre.hub.service.SynchroEmrMonitorService;
import cn.xa.eyre.hub.service.SynchroEmrRealService;
import cn.xa.eyre.hub.staticvalue.HubCodeEnum;
import cn.xa.eyre.inpadm.domain.PatsInHospital;
import cn.xa.eyre.insurance.domain.GysybIcd10;
import cn.xa.eyre.medrec.domain.*;
import cn.xa.eyre.outpadm.domain.ClinicMaster;
import cn.xa.eyre.outpdoct.domain.OutpMr;
import cn.xa.eyre.outpdoct.domain.OutpMrKey;
import cn.xa.eyre.outpdoct.domain.OutpWaitQueue;
import cn.xa.eyre.system.dict.domain.*;
import cn.xa.eyre.system.dict.mapper.*;
import cn.xa.eyre.system.temp.domain.DictTemp;
import cn.xa.eyre.system.temp.domain.HisDeptDict;
import cn.xa.eyre.system.temp.mapper.DictTempMapper;
import cn.xa.eyre.system.temp.mapper.HisDeptDictMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.Period;
import java.util.Date;
import java.util.List;

@Service
public class DataConvertService {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private HisDeptDictMapper hisDeptDictMapper;// oracle数据
    @Autowired
    private DictTempMapper dictTempMapper;// 前置软件数据
    @Autowired
    private DictDisDeptMapper dictDisDeptMapper;// 转码表
    @Autowired
    private DictDiseaseIcd10Mapper dictDiseaseIcd10Mapper;// 转码表
    @Autowired
    private DdDiseaseIcdMapper ddDiseaseIcdMapper;// 前置软件诊断代码表
    @Autowired
    private InsuranceFeignClient insuranceFeignClient;// HIS
    @Autowired
    private DictSpecimenCategoryMapper dictSpecimenCategoryMapper;
    @Autowired
    private OutpdoctFeignClient outpdoctFeignClient;
    @Autowired
    private MedrecFeignClient medrecFeignClient;
    @Autowired
    private OutpadmFeignClient outpadmFeignClient;
    @Autowired
    private DdNationMapper ddNationMapper;
    @Autowired
    SynchroEmrRealService synchroEmrRealService;
    @Autowired
    private CommFeignClient commFeignClient;
    @Autowired
    private HubToolService hubToolService;
    @Autowired
    private InpadmFeignClient inpadmFeignClient;
    @Autowired
    private DictTreatResultMapper dictTreatResultMapper;

    @Resource
    SynchroBaseService synchroBaseService;

    public boolean convertDept() {
        List<HisDeptDict> merList = hisDeptDictMapper.selectAll();

        List<DictTemp> hubList = dictTempMapper.selectAll();

        String hisName = null;
        DictDisDept dictDisDept = new DictDisDept();
        dictDisDept.setStatus(0);
        dictDisDept.setIsDefault(1);
        dictDisDept.setCreateTime(DateUtils.getNowDate());
//        for (HisDeptDict his:merList) {
//            boolean exist = false;
//            hisName = his.getDeptName();
//            if (hisName.length() > 3){
//                hisName = hisName.substring(0, hisName.length() - 3);//截取掉最后两个字
//            }
//            for (DictTemp temp: hubList) {
//                if(temp.getName().contains(hisName)){
//                    exist = true;
//                    dictDisDept.setRemark("精准匹配");
//                    dictDisDept.setEmrCode(his.getDeptCode());
//                    dictDisDept.setEmrName(his.getDeptName());
//                    dictDisDept.setHubCode(temp.getCode());
//                    dictDisDept.setHubName(temp.getName());
//                    break;// 优先匹配第一个
//                }
//            }
//            // 没找到
//            if(!exist){
//                dictDisDept.setRemark("在前置软件中没有同名的");
//                dictDisDept.setEmrCode(his.getDeptCode());
//                dictDisDept.setEmrName(his.getDeptName());
//                dictDisDept.setHubCode("D99");
//                dictDisDept.setHubName("其他科室");
//            }
//            dictDisDeptMapper.insertSelective(dictDisDept);
//        }

        // 把HIS中没有的写入
        List<DictDisDept> list = dictDisDeptMapper.selectAll();
        for (DictTemp temp: hubList) {
            boolean exist = false;
            for (DictDisDept dept:list) {
                if(dept.getHubCode().equals(temp.getCode())){
                    exist = true;
                    break;// 优先匹配第一个
                }
            }
            if (!exist){
                dictDisDept.setRemark("在HIS中没有");
                dictDisDept.setHubCode(temp.getCode());
                dictDisDept.setHubName(temp.getName());
                dictDisDeptMapper.insertSelective(dictDisDept);
            }
        }

        return true;
    }

    public void baseDept(DBMessage dbMessage) {
        logger.debug("医院信息系统科室信息接口");
      //  synchroBaseService.syncBaseDept(dbMessage.getAfterData(), "add");
    }

    public void baseUser(DBMessage dbMessage) {
        logger.debug("医院信息系统用户信息接口");

    }

    public boolean convertDiseaseIcd() {
        List<DdDiseaseIcd> hubIcds = ddDiseaseIcdMapper.selectAll();
        R<List<GysybIcd10>> icd10ListResult = insuranceFeignClient.getGysybIcd10List();
        if (R.SUCCESS == icd10ListResult.getCode() && !icd10ListResult.getData().isEmpty()){
            for (GysybIcd10 emricd : icd10ListResult.getData()) {
                DictDiseaseIcd10 dictDiseaseIcd10 = new DictDiseaseIcd10();
                dictDiseaseIcd10.setEmrCode(emricd.getIcdCode());
                dictDiseaseIcd10.setEmrName(emricd.getIcdName());
                boolean match = false;
                // 精准怕匹配
                for (DdDiseaseIcd hubicd : hubIcds) {
                    if (hubicd.getName().equals(emricd.getIcdName())){
                        dictDiseaseIcd10.setRemark("精准匹配");
                        dictDiseaseIcd10.setHubCode(hubicd.getCode());
                        dictDiseaseIcd10.setHubName(hubicd.getName());
                        match = true;
                        break;
                    }
                }
                 if (!match){
                     // 模糊匹配
                     for (DdDiseaseIcd hubicd : hubIcds) {
                         if (FuzzyMatcher.fuzzyMatch(hubicd.getName(), emricd.getIcdName())){
                             dictDiseaseIcd10.setRemark("模糊匹配");
                             dictDiseaseIcd10.setHubCode(hubicd.getCode());
                             dictDiseaseIcd10.setHubName(hubicd.getName());
                             match = true;
                             break;
                         }
                     }
                 }

                if (!match){
                    // 查询默认
                    dictDiseaseIcd10.setRemark("未匹配到，其他类");
                    dictDiseaseIcd10.setHubCode("143");
                    dictDiseaseIcd10.setHubName("其他");
                }
                dictDiseaseIcd10Mapper.insertSelective(dictDiseaseIcd10);
            }
        }
        return true;
    }

    public boolean convertBb() {
        List<DictSpecimenCategory> dictSpecimenCategories = dictSpecimenCategoryMapper.selectAll();
        List<DictTemp> hubList = dictTempMapper.selectAll();
        for (DictSpecimenCategory sp : dictSpecimenCategories) {
            boolean match = false;
            for (DictTemp temp: hubList) {
                if (temp.getName().equals(sp.getEmrName())){
                    sp.setRemark("精准匹配");
                    sp.setHubCode(temp.getCode());
                    sp.setHubName(temp.getName());
                    match = true;
                    break;
                }
            }
            if (!match){
                // 模糊匹配
                for (DictTemp temp2: hubList) {
                    if (FuzzyMatcher.fuzzyMatch(temp2.getName(), sp.getEmrName())){
                        sp.setRemark("模糊匹配");
                        sp.setHubCode(temp2.getCode());
                        sp.setHubName(temp2.getName());
                        match = true;
                        break;
                    }
                }
            }
            if (!match){
                // 查询默认
                sp.setRemark("未匹配到，其他类");
                sp.setHubCode("99");
                sp.setHubName("其他");
            }
            dictSpecimenCategoryMapper.updateByPrimaryKey(sp);

        }
        return true;
    }

    public EmrActivityInfo pushOutpMr(OutpMr outpMrParam) {
        OutpMr outpMr = outpdoctFeignClient.getOutpMrByCondition(outpMrParam).getData().get(0);

        if (StringUtils.isNotBlank(outpMr.getPatientId())){
            logger.debug("构造emrOutpatientRecord接口数据...");
            R<PatMasterIndex> medrecResult = medrecFeignClient.getPatMasterIndex(outpMr.getPatientId());
            R<ClinicMaster> outpadmResult = outpadmFeignClient.getClinicMaster(outpMr.getPatientId(), outpMr.getVisitNo(), DateUtils.dateTime(outpMr.getVisitDate()));
            if (R.SUCCESS == medrecResult.getCode() && medrecResult.getData() != null
                    && R.SUCCESS == outpadmResult.getCode() && outpadmResult.getData() != null){
                // 军人+文职不推送
                if (StringUtils.isNotBlank(outpadmResult.getData().getIdentity()) && ArrayUtil.contains(Constants.IDENTIFY_LIST, outpadmResult.getData().getIdentity())){
                    logger.error("身份为军人，不推送数据");
                    return null;
                }
                if (StringUtils.isNotBlank(outpadmResult.getData().getSecurityTypeCode()) && ArrayUtil.contains(Constants.SECURITY_TYPE_CODE_LIST, outpadmResult.getData().getSecurityTypeCode())){
                    logger.error("身份为文职，不推送数据");
                    return null;
                }

                // 更新推送患者信息
                hubToolService.syncPatInfo(medrecResult.getData());
                EmrOutpatientRecord emrOutpatientRecord = new EmrOutpatientRecord();
                // ID使用OUTP_MR表联合主键拼接计算MD5
                String id = DigestUtil.md5Hex(DateUtils.dateTime(outpMr.getVisitDate()) + outpMr.getVisitNo() + outpMr.getOrdinal());
                emrOutpatientRecord.setId(id);
                emrOutpatientRecord.setPatientId(outpMr.getPatientId());
                emrOutpatientRecord.setSerialNumber(DigestUtil.md5Hex(outpMr.getPatientId() + outpMr.getVisitNo()));
                emrOutpatientRecord.setOutpatientDate(outpMr.getVisitDate());
                emrOutpatientRecord.setInitalDiagnosisCode(String.valueOf(1)); // 初诊标识，表中没有这个字段
                emrOutpatientRecord.setChiefComplaint(outpMr.getIllnessDesc());
                emrOutpatientRecord.setPresentIllnessHis(outpMr.getMedHistory());
                emrOutpatientRecord.setPastIllnessHis(outpMr.getAnamnesis());
                emrOutpatientRecord.setOperationHis(outpMr.getMedicalRecord());
                emrOutpatientRecord.setMaritalHis(outpMr.getMarrital());
                if(StringUtils.isNotBlank(outpMr.getIndividual())){
                    emrOutpatientRecord.setAllergyHisFlag("1");
                    emrOutpatientRecord.setAllergyHis(outpMr.getIndividual());
                }
                emrOutpatientRecord.setMenstrualHis(outpMr.getMenses());
                emrOutpatientRecord.setFamilyHis(outpMr.getFamilyIll());
                emrOutpatientRecord.setPhysicalExamination(outpMr.getBodyExam());
                emrOutpatientRecord.setStudiesSummaryResult(outpMr.getAssistExam());

                // 诊断代码
                emrOutpatientRecord.setWmDiagnosisCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                emrOutpatientRecord.setWmDiagnosisName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                OutpMrYb outpMrYbKey = new OutpMrYb();
                BeanUtil.copyProperties(outpMr, outpMrYbKey);
                outpMrYbKey.setVisitDateStr(DateUtils.dateTime(outpMr.getVisitDate()));
                R<OutpMrYb> outpMrYbResult = medrecFeignClient.getOutpMrYb(outpMrYbKey);
                Users doctor = null;
                if(outpMrYbResult.getCode() == R.SUCCESS && outpMrYbResult.getData() != null){
                    if (StringUtils.isNotBlank(outpMrYbResult.getData().getIcdCode01())){
                        DictDiseaseIcd10 dictDiseaseIcd10 = dictDiseaseIcd10Mapper.selectByEmrCode(outpMrYbResult.getData().getIcdCode01());
                        if(dictDiseaseIcd10 == null || dictDiseaseIcd10.getHubCode().equals(HubCodeEnum.DISEASE_ICD10_CODE.getCode())){
                            emrOutpatientRecord.setWmDiagnosisCode(outpMrYbResult.getData().getIcdCode01());
                            emrOutpatientRecord.setWmDiagnosisName(outpMrYbResult.getData().getIcdName01());
                        }else {
                            emrOutpatientRecord.setWmDiagnosisCode(dictDiseaseIcd10.getHubCode());
                            emrOutpatientRecord.setWmDiagnosisName(dictDiseaseIcd10.getHubName());
                        }
                        if (StringUtils.isNotBlank(outpMrYbResult.getData().getIcdCode02())){
                            DictDiseaseIcd10 dictDiseaseIcd102 = dictDiseaseIcd10Mapper.selectByEmrCode(outpMrYbResult.getData().getIcdCode02());
                            if(dictDiseaseIcd102 == null || dictDiseaseIcd102.getHubCode().equals(HubCodeEnum.DISEASE_ICD10_CODE.getCode())){
                                emrOutpatientRecord.setWmDiagnosisCode(emrOutpatientRecord.getWmDiagnosisCode() + "||" + outpMrYbResult.getData().getIcdCode02());
                                emrOutpatientRecord.setWmDiagnosisName(emrOutpatientRecord.getWmDiagnosisName() + "||" + outpMrYbResult.getData().getIcdName02());
                            }else {
                                emrOutpatientRecord.setWmDiagnosisCode(emrOutpatientRecord.getWmDiagnosisCode() + "||" + dictDiseaseIcd102.getHubCode());
                                emrOutpatientRecord.setWmDiagnosisName(emrOutpatientRecord.getWmDiagnosisName() + "||" + dictDiseaseIcd102.getHubName());
                            }
                        }
                    }
                    if (StringUtils.isNotBlank(outpMrYbResult.getData().getDoctor())){
                        R<Users> user = commFeignClient.getUserByName(outpMrYbResult.getData().getDoctor());
                        if (R.SUCCESS == user.getCode() && user.getData() != null){
                            doctor = user.getData();
                            emrOutpatientRecord.setOperatorId(doctor.getUserId());
                        }
                    }
                }else {
                    if (StringUtils.isNotBlank(outpMr.getDiagnosisCodeMz1())){
                        DictDiseaseIcd10 dictDiseaseIcd10 = dictDiseaseIcd10Mapper.selectByEmrCode(outpMr.getDiagnosisCodeMz1());
                        if(dictDiseaseIcd10 == null || dictDiseaseIcd10.getHubCode().equals(HubCodeEnum.DISEASE_ICD10_CODE.getCode())){
                            emrOutpatientRecord.setWmDiagnosisCode(outpMr.getDiagnosisCodeMz1());
                            emrOutpatientRecord.setWmDiagnosisName(outpMr.getDiagnosisMz1());
                        }else {
                            emrOutpatientRecord.setWmDiagnosisCode(dictDiseaseIcd10.getHubCode());
                            emrOutpatientRecord.setWmDiagnosisName(dictDiseaseIcd10.getHubName());
                        }
                        if (StringUtils.isNotBlank(outpMr.getDiagnosisCodeMz2())){
                            DictDiseaseIcd10 dictDiseaseIcd102 = dictDiseaseIcd10Mapper.selectByEmrCode(outpMr.getDiagnosisCodeMz2());
                            if(dictDiseaseIcd102 == null || dictDiseaseIcd102.getHubCode().equals(HubCodeEnum.DISEASE_ICD10_CODE.getCode())){
                                emrOutpatientRecord.setWmDiagnosisCode(emrOutpatientRecord.getWmDiagnosisCode() + "||" + outpMr.getDiagnosisCodeMz2());
                                emrOutpatientRecord.setWmDiagnosisName(emrOutpatientRecord.getWmDiagnosisName() + "||" + outpMr.getDiagnosisMz2());
                            }else {
                                emrOutpatientRecord.setWmDiagnosisCode(emrOutpatientRecord.getWmDiagnosisCode() + "||" + dictDiseaseIcd102.getHubCode());
                                emrOutpatientRecord.setWmDiagnosisName(emrOutpatientRecord.getWmDiagnosisName() + "||" + dictDiseaseIcd102.getHubName());
                            }
                        }
                    }
                }
//                emrOutpatientRecord.setTreatment(outpMr.getAdvice());

                PatMasterIndex patMasterIndex = medrecResult.getData();
                emrOutpatientRecord.setPatientName(patMasterIndex.getName());
                if (StringUtils.isBlank(patMasterIndex.getIdNo())){
                    emrOutpatientRecord.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                    emrOutpatientRecord.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                    emrOutpatientRecord.setIdCard("-");
                }else {
                    emrOutpatientRecord.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                    emrOutpatientRecord.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                    emrOutpatientRecord.setIdCard(patMasterIndex.getIdNo());
                }

                ClinicMaster clinicMaster = outpadmResult.getData();
                DictDisDept dictDisDept = hubToolService.getDept(clinicMaster.getVisitDept());

                // 查询操作员ID
                if (StringUtils.isBlank(emrOutpatientRecord.getOperatorId())){
                    R<Users> user = commFeignClient.getUserByName(outpMr.getDoctor());
                    if (R.SUCCESS == user.getCode() && user.getData() != null){
                        doctor = user.getData();
                        emrOutpatientRecord.setOperatorId(doctor.getUserId());
                    }
                }
                if(StringUtils.isBlank(emrOutpatientRecord.getOperatorId())){
                    OutpWaitQueue queue = new OutpWaitQueue();
                    queue.setVisitDateStr(DateUtils.dateTime(outpMr.getVisitDate()));
                    queue.setVisitNo(Integer.valueOf(outpMr.getVisitNo()));
                    R<OutpWaitQueue> outpWaitQueueResult = outpdoctFeignClient.getOutpWaitQueueByCondition(queue);
                    if (outpWaitQueueResult.getCode() == R.SUCCESS && outpWaitQueueResult.getData() != null){
                        R<Users> user = commFeignClient.getUserByName(outpWaitQueueResult.getData().getDoctor());
                        if (R.SUCCESS == user.getCode() && user.getData() != null){
                            doctor = user.getData();
                            emrOutpatientRecord.setOperatorId(doctor.getUserId());
                        }
                    }
                }

                emrOutpatientRecord.setDeptCode(dictDisDept.getHubCode());
                emrOutpatientRecord.setDeptName(dictDisDept.getHubName());

                emrOutpatientRecord.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrOutpatientRecord.setOrgName(HubCodeEnum.ORG_CODE.getName());
                emrOutpatientRecord.setOperationTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, DateUtils.getNowDate()));
//                synchroEmrMonitorService.syncEmrOutpatientRecord(emrOutpatientRecord, httpMethod);

                logger.debug("构造emrActivityInfo(门诊/急诊)接口数据...");
                EmrActivityInfo emrActivityInfo = new EmrActivityInfo();
                emrActivityInfo.setId(id);
                emrActivityInfo.setPatientId(outpMr.getPatientId());
                String clinicType = clinicMaster.getClinicType();
                if (StringUtils.isNotBlank(clinicType)){
                    if (clinicType.contains("急诊号")){
                        emrActivityInfo.setActivityTypeCode(HubCodeEnum.DIAGNOSIS_ACTIVITIES_EMERGENCY.getCode());
                        emrActivityInfo.setActivityTypeName(HubCodeEnum.DIAGNOSIS_ACTIVITIES_EMERGENCY.getName());
                    } else {
                        emrActivityInfo.setActivityTypeCode(HubCodeEnum.DIAGNOSIS_ACTIVITIES_OUTPATIENT.getCode());
                        emrActivityInfo.setActivityTypeName(HubCodeEnum.DIAGNOSIS_ACTIVITIES_OUTPATIENT.getName());

                    }
                }
                emrActivityInfo.setSerialNumber(emrOutpatientRecord.getSerialNumber());
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
                if (StringUtils.isNotBlank(emrOutpatientRecord.getWmDiagnosisCode())){
                    emrActivityInfo.setWmDiseaseCode(emrOutpatientRecord.getWmDiagnosisCode());
                    emrActivityInfo.setWmDiseaseName(emrOutpatientRecord.getWmDiagnosisName());
                }else {
                    emrActivityInfo.setWmDiseaseCode(HubCodeEnum.DISEASE_ICD10_CODE.getCode());
                    emrActivityInfo.setWmDiseaseName(HubCodeEnum.DISEASE_ICD10_CODE.getName());
                }

                emrActivityInfo.setFillDoctor(doctor.getUserName());

                emrActivityInfo.setDeptCode(dictDisDept.getHubCode());
                emrActivityInfo.setDeptName(dictDisDept.getHubName());

                emrActivityInfo.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrActivityInfo.setOrgName(HubCodeEnum.ORG_CODE.getName());
                emrActivityInfo.setOperationTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, DateUtils.getNowDate()));
                emrActivityInfo.setOperatorId(emrOutpatientRecord.getOperatorId());
                if (StringUtils.isBlank(emrOutpatientRecord.getOperatorId())){
                    emrActivityInfo.setOperatorId("-");
                }
                return emrActivityInfo;
//                synchroEmrRealService.syncEmrActivityInfo(emrActivityInfo, httpMethod);
            }else {
                logger.error("{}对应PatMasterIndex信息或ClinicMaster信息为空，无法同步", outpMr.getPatientId());
            }
        }else {
            logger.error("patientId为空，无法同步");
        }
        return null;
    }

    public EmrPatientInfo pushPatMasterIndex(PatMasterIndex patMasterIndexParam) {
        PatMasterIndex patMasterIndex = medrecFeignClient.getPatMasterIndex(patMasterIndexParam.getPatientId()).getData();
        logger.debug("构造emrPatientInfo接口数据...");
        // 构造请求参数
        EmrPatientInfo emrPatientInfo = new EmrPatientInfo();
        emrPatientInfo.setId(patMasterIndex.getPatientId());
        emrPatientInfo.setPatientName(patMasterIndex.getName());
        if (StringUtils.isBlank(patMasterIndex.getIdNo())){
            emrPatientInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
            emrPatientInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
            emrPatientInfo.setIdCard("-");
        }else {
            if (IdcardUtil.isValidCard(patMasterIndex.getIdNo())){
                emrPatientInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                emrPatientInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                emrPatientInfo.setIdCard(patMasterIndex.getIdNo());
            }else {
                emrPatientInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                emrPatientInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                emrPatientInfo.setIdCard(patMasterIndex.getIdNo());
            }
        }
        if (StringUtils.isBlank(patMasterIndex.getSexCode())){
            emrPatientInfo.setGenderCode(HubCodeEnum.SEX_OTHER.getCode());
            if(StringUtils.isNotBlank(patMasterIndex.getSex())){
                if (patMasterIndex.getSex().equals("男")){
                    emrPatientInfo.setGenderCode("1");
                } else if (patMasterIndex.getSex().equals("女")) {
                    emrPatientInfo.setGenderCode("2");
                } else {
                    emrPatientInfo.setGenderCode(HubCodeEnum.SEX_OTHER.getCode());
                }
            }
        }else {
            emrPatientInfo.setGenderCode(patMasterIndex.getSexCode());
        }
        emrPatientInfo.setGenderName(patMasterIndex.getSex());
        emrPatientInfo.setBirthDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, patMasterIndex.getDateOfBirth()));
        if("CN".equals(patMasterIndex.getCitizenship())){
            emrPatientInfo.setNationalityCode(HubCodeEnum.NATIONALITY_CODE.getCode());
            emrPatientInfo.setNationalityName(HubCodeEnum.NATIONALITY_CODE.getName());
        }
        DdNation ddNation = ddNationMapper.selectByName(patMasterIndex.getNation());
        if (ddNation != null){
            emrPatientInfo.setNationCode(ddNation.getCode());
            emrPatientInfo.setNationName(ddNation.getName());
        }
//        else {
//            emrPatientInfo.setNationCode(HubCodeEnum.NATION_CODE.getCode());
//            emrPatientInfo.setNationName(HubCodeEnum.NATION_CODE.getName());
//        }
        emrPatientInfo.setCurrentAddrCode(patMasterIndex.getMailingAreaCode4());
        emrPatientInfo.setCurrentAddrName(patMasterIndex.getMailingAddress());
        emrPatientInfo.setCurrentAddrDetail(patMasterIndex.getNextOfKinAddr());
        emrPatientInfo.setWorkunit(patMasterIndex.getWorkunit());
        Date birthDate = patMasterIndex.getDateOfBirth();
        if (null != birthDate) {
            LocalDate localDate = DateUtils.convertDateToLocalDate(birthDate);
            Period period = Period.between(localDate, LocalDate.now());
            if (period.getYears() <= 14) {
                if(patMasterIndex.getNextOfKin() != null){
                    emrPatientInfo.setContacts(patMasterIndex.getNextOfKin());
                    emrPatientInfo.setContactsTel(patMasterIndex.getNextOfKinPhone());
                }else {
                    emrPatientInfo.setContacts(patMasterIndex.getGuardianName());
                    emrPatientInfo.setContactsTel(patMasterIndex.getGuardianPhone());
                }
            }
        }
        emrPatientInfo.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
        emrPatientInfo.setOrgName(HubCodeEnum.ORG_CODE.getName());
        // 查询操作员ID
        if (StringUtils.isNotBlank(patMasterIndex.getOperator())){
            R<Users> user = commFeignClient.getUserByName(patMasterIndex.getOperator());
            if (R.SUCCESS == user.getCode() && user.getData() != null){
                emrPatientInfo.setOperatorId(user.getData().getUserId());
            }
        }

        emrPatientInfo.setOperationTime(DateUtils.getTime());
//        synchroEmrRealService.syncEmrPatientInfo(emrPatientInfo, httpMethod);
        return emrPatientInfo;
    }

    public EmrActivityInfo pushDiagnosis(DiagnosisKey diagnosisKey) {
        Diagnosis diagnosis = medrecFeignClient.getDiagnosis(diagnosisKey).getData();
        R<PatMasterIndex> medrecResult = medrecFeignClient.getPatMasterIndex(diagnosis.getPatientId());
        DiagnosticCategoryKey diagnosticCategoryKey = new DiagnosticCategoryKey();
        BeanUtil.copyProperties(diagnosis, diagnosticCategoryKey);
        // 诊断编码，先取diagnosis表，没有则取DiagnosticCategory表，还是没有就默认为支气管炎
        DictDiseaseIcd10 dictDiseaseIcd10 = null;
        if (StringUtils.isNotBlank(diagnosis.getRydjZd()) && StringUtils.isNotBlank(diagnosis.getRydjZdbm())){
            dictDiseaseIcd10 = hubToolService.getDiseaseIcd10(diagnosis.getRydjZdbm(), diagnosis.getRydjZd());
        }else {
            R<DiagnosticCategory> diagnosticCatResult = medrecFeignClient.getDiagnosticCategory(diagnosticCategoryKey);
            if (diagnosticCatResult.getCode() == R.SUCCESS && diagnosticCatResult.getData() != null){
                dictDiseaseIcd10 = hubToolService.getDiseaseIcd10(diagnosticCatResult.getData().getDiagnosisCode(), diagnosis.getDiagnosisDesc());
            }else {
                logger.info("{}诊断编码为空，默认诊断为支气管炎", medrecResult.getData().getPatientId());
                dictDiseaseIcd10 = new DictDiseaseIcd10(HubCodeEnum.DISEASE_ICD10_CODE_DEFAULT.getCode(), HubCodeEnum.DISEASE_ICD10_CODE_DEFAULT.getName());
            }
        }

        PatVisitKey patVisitKey = new PatVisitKey();
        BeanUtil.copyProperties(diagnosis, patVisitKey);
        R<PatVisit> patVisitResult = medrecFeignClient.getPatVisit(patVisitKey);
//        R<PatsInHospital> hospitalResult = inpadmFeignClient.getPatsInHospital(diagnosis.getPatientId(), diagnosis.getVisitId());
        if (R.SUCCESS == medrecResult.getCode() && medrecResult.getData() != null
                && R.SUCCESS == patVisitResult.getCode() && patVisitResult.getData() != null){
            // 更新推送患者信息
            hubToolService.syncPatInfo(medrecResult.getData());
            // 军人+文职不推送
            if (StringUtils.isNotBlank(patVisitResult.getData().getIdentity()) && ArrayUtil.contains(Constants.IDENTIFY_LIST, patVisitResult.getData().getIdentity())){
                logger.error("身份为军人，不推送数据");
                return null;
            }
            if (StringUtils.isNotBlank(patVisitResult.getData().getSecurityTypeCode()) && ArrayUtil.contains(Constants.SECURITY_TYPE_CODE_LIST, patVisitResult.getData().getSecurityTypeCode())){
                logger.error("身份为文职，不推送数据");
                return null;
            }

            EmrFirstCourse emrFirstCourse = new EmrFirstCourse();
            EmrDailyCourse emrDailyCourse = new EmrDailyCourse();
            // ID使用DIAGNOSIS表patientId、visitId、diagnosisDate拼接计算MD5
            String id = DigestUtil.md5Hex(diagnosis.getPatientId() + diagnosis.getVisitId() + diagnosis.getDiagnosisType() + diagnosis.getDiagnosisNo());

            if (diagnosis.getDiagnosisType().equals(Constants.DIAGNOSIS_TYPE_CODE_RYCZ) || diagnosis.getDiagnosisType().equals(Constants.DIAGNOSIS_TYPE_CODE_MZZD)){
                logger.debug("构造emrFirstCourse接口数据...");
                emrFirstCourse.setId(id);
                emrFirstCourse.setPatientId(diagnosis.getPatientId());
                emrFirstCourse.setSerialNumber(DigestUtil.md5Hex(diagnosis.getPatientId() + diagnosis.getVisitId()));
                emrFirstCourse.setCreateDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, diagnosis.getDiagnosisDate()));
                emrFirstCourse.setPresentIllnessHis(diagnosis.getDiagnosisDesc());

                emrFirstCourse.setPatientName(medrecResult.getData().getName());
                if (StringUtils.isBlank(medrecResult.getData().getIdNo())){
                    emrFirstCourse.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                    emrFirstCourse.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                    emrFirstCourse.setIdCard("-");
                }else {
                    emrFirstCourse.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                    emrFirstCourse.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                    emrFirstCourse.setIdCard(medrecResult.getData().getIdNo());
                }

                R<PatsInHospital> hospitalResult = inpadmFeignClient.getPatsInHospital(diagnosis.getPatientId(), diagnosis.getVisitId());
                if (hospitalResult.getCode() == R.SUCCESS && hospitalResult.getData() != null){
                    emrFirstCourse.setWardNo(hospitalResult.getData().getWardCode());
                    emrFirstCourse.setBedNo(String.valueOf(hospitalResult.getData().getBedNo()));
                }

                // 治疗医生
                if (StringUtils.isNotBlank(patVisitResult.getData().getDoctorInCharge())){
                    R<Users> user = commFeignClient.getUserByName(patVisitResult.getData().getDoctorInCharge());
                    if (R.SUCCESS == user.getCode() && user.getData() != null){
                        emrFirstCourse.setResidentPhysicianId(user.getData().getUserId());
                        emrFirstCourse.setOperatorId(user.getData().getUserId());
                    }
                }else {
                    R<Users> user = commFeignClient.getUserByName(hospitalResult.getData().getDoctorInCharge());
                    if (R.SUCCESS == user.getCode() && user.getData() != null){
                        emrFirstCourse.setResidentPhysicianId(user.getData().getUserId());
                        emrFirstCourse.setOperatorId(user.getData().getUserId());
                    }
                }
                emrFirstCourse.setWmInitalDiagnosisCode(dictDiseaseIcd10.getHubCode());
                emrFirstCourse.setWmInitalDiagnosisName(dictDiseaseIcd10.getHubName());

                if(StringUtils.isNotBlank(diagnosis.getTreatResult())){
                    DictTreatResult dictTreatResult = dictTreatResultMapper.selectByEmrName(diagnosis.getTreatResult());
                    if(dictTreatResult == null || dictTreatResult.getHubCode().equals(HubCodeEnum.TREAT_RESULT_OTHER.getCode())){
                        emrFirstCourse.setDiseaseProgressionCode(HubCodeEnum.TREAT_RESULT_OTHER.getCode());
                        emrFirstCourse.setDiseaseProgressionName(diagnosis.getTreatResult());
                    }else {
                        emrFirstCourse.setDiseaseProgressionCode(dictTreatResult.getHubCode());
                        emrFirstCourse.setDiseaseProgressionName(dictTreatResult.getHubName());
                    }
                }

                DictDisDept dictDisDept = hubToolService.getDept(patVisitResult.getData().getDeptAdmissionTo());

                emrFirstCourse.setDeptCode(dictDisDept.getHubCode());
                emrFirstCourse.setDeptName(dictDisDept.getHubName());

                emrFirstCourse.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrFirstCourse.setOrgName(HubCodeEnum.ORG_CODE.getName());
                emrFirstCourse.setOperationTime(DateUtils.getTime());

//                synchroEmrMonitorService.syncEmrFirstCourse(emrFirstCourse, httpMethod);

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
                emrActivityInfo.setFillDoctor(patVisitResult.getData().getDoctorInCharge());
                emrActivityInfo.setOperatorId(emrFirstCourse.getOperatorId());
                if (StringUtils.isBlank(emrActivityInfo.getFillDoctor()))
                    emrActivityInfo.setFillDoctor("-");
                if (StringUtils.isBlank(emrActivityInfo.getOperatorId()))
                    emrActivityInfo.setOperatorId("-");
                emrActivityInfo.setDeptCode(emrFirstCourse.getDeptCode());
                emrActivityInfo.setDeptName(emrFirstCourse.getDeptName());
                emrActivityInfo.setOrgCode(emrFirstCourse.getOrgCode());
                emrActivityInfo.setOrgName(emrFirstCourse.getOrgName());
                emrActivityInfo.setOperationTime(DateUtils.getTime());
//                synchroEmrRealService.syncEmrActivityInfo(emrActivityInfo, httpMethod);
                return emrActivityInfo;

            }else
//                if (diagnosis.getDiagnosisType().equals(Constants.DIAGNOSIS_TYPE_CODE_QT) && diagnosis.getVisitId() != null)
            {
                logger.debug("构造emrDailyCourse接口数据...");
                emrDailyCourse.setId(id);
                emrDailyCourse.setPatientId(diagnosis.getPatientId());
                emrDailyCourse.setSerialNumber(DigestUtil.md5Hex(diagnosis.getPatientId() + diagnosis.getVisitId()));
                emrDailyCourse.setCreateDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, diagnosis.getDiagnosisDate()));
                emrDailyCourse.setCourse(diagnosis.getDiagnosisDesc());

                emrDailyCourse.setPatientName(medrecResult.getData().getName());
                if (StringUtils.isBlank(medrecResult.getData().getIdNo())){
                    emrDailyCourse.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                    emrDailyCourse.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                    emrDailyCourse.setIdCard("-");
                }else {
                    emrDailyCourse.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                    emrDailyCourse.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                    emrDailyCourse.setIdCard(medrecResult.getData().getIdNo());
                }
                R<Users> user = commFeignClient.getUserByName(patVisitResult.getData().getDoctorInCharge());
                if (R.SUCCESS == user.getCode() && user.getData() != null){
                    emrDailyCourse.setOperatorId(user.getData().getUserId());
                }

                DictDisDept dictDisDept = hubToolService.getDept(patVisitResult.getData().getDeptAdmissionTo());

                emrDailyCourse.setDeptCode(dictDisDept.getHubCode());
                emrDailyCourse.setDeptName(dictDisDept.getHubName());

                emrDailyCourse.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrDailyCourse.setOrgName(HubCodeEnum.ORG_CODE.getName());
                emrDailyCourse.setOperationTime(DateUtils.getTime());

                if(StringUtils.isNotBlank(diagnosis.getTreatResult())){
                    DictTreatResult dictTreatResult = dictTreatResultMapper.selectByEmrName(diagnosis.getTreatResult());
                    if(dictTreatResult == null || dictTreatResult.getHubCode().equals(HubCodeEnum.TREAT_RESULT_OTHER.getCode())){
                        emrDailyCourse.setDiseaseProgressionCode(HubCodeEnum.TREAT_RESULT_OTHER.getCode());
                        emrDailyCourse.setDiseaseProgressionName(diagnosis.getTreatResult());
                    }else {
                        emrDailyCourse.setDiseaseProgressionCode(dictTreatResult.getHubCode());
                        emrDailyCourse.setDiseaseProgressionName(dictTreatResult.getHubName());
                    }
                }

//                synchroEmrMonitorService.syncEmrDailyCourse(emrDailyCourse, httpMethod);

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
                emrActivityInfo.setWmDiseaseCode(dictDiseaseIcd10.getHubCode());
                emrActivityInfo.setWmDiseaseName(dictDiseaseIcd10.getHubName());
                emrActivityInfo.setFillDoctor(patVisitResult.getData().getDoctorInCharge());
                emrActivityInfo.setOperatorId(emrDailyCourse.getOperatorId());
                if (StringUtils.isBlank(emrActivityInfo.getFillDoctor()))
                    emrActivityInfo.setFillDoctor("-");
                if (StringUtils.isBlank(emrActivityInfo.getOperatorId()))
                    emrActivityInfo.setOperatorId("-");
                emrActivityInfo.setDeptCode(emrDailyCourse.getDeptCode());
                emrActivityInfo.setDeptName(emrDailyCourse.getDeptName());
                emrActivityInfo.setOrgCode(emrDailyCourse.getOrgCode());
                emrActivityInfo.setOrgName(emrDailyCourse.getOrgName());
                emrActivityInfo.setOperationTime(DateUtils.getTime());
//                synchroEmrRealService.syncEmrActivityInfo(emrActivityInfo, httpMethod);
                return emrActivityInfo;
            }

        }else {
            logger.error("{}对应PatMasterIndex或PatVisit信息为空，无法同步", diagnosis.getPatientId());
        }
        return null;
    }
}
