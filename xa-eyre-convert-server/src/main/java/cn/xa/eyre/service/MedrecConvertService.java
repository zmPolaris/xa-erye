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
import cn.xa.eyre.hisapi.InpadmFeignClient;
import cn.xa.eyre.hisapi.MedrecFeignClient;
import cn.xa.eyre.hub.domain.emrmonitor.EmrDailyCourse;
import cn.xa.eyre.hub.domain.emrmonitor.EmrDischargeInfo;
import cn.xa.eyre.hub.domain.emrmonitor.EmrFirstCourse;
import cn.xa.eyre.hub.domain.emrreal.EmrPatientInfo;
import cn.xa.eyre.hub.service.SynchroEmrMonitorService;
import cn.xa.eyre.hub.service.SynchroEmrRealService;
import cn.xa.eyre.hub.staticvalue.HubCodeEnum;
import cn.xa.eyre.inpadm.domain.PatsInHospital;
import cn.xa.eyre.medrec.domain.Diagnosis;
import cn.xa.eyre.medrec.domain.PatMasterIndex;
import cn.xa.eyre.system.dict.domain.DdNation;
import cn.xa.eyre.system.dict.domain.DictDisDept;
import cn.xa.eyre.system.dict.mapper.DdNationMapper;
import cn.xa.eyre.system.dict.mapper.DictDisDeptMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private DictDisDeptMapper dictDisDeptMapper;// 转码表

    public void patMasterIndex(DBMessage dbMessage) {
        logger.debug("病人主索引表PAT_MASTER_INDEX变更接口");
        logger.debug("PAT_MASTER_INDEX变更需调用emrPatientInfo同步接口");
        EmrPatientInfo emrPatientInfo = new EmrPatientInfo();
        String httpMethod = null;
        PatMasterIndex patMasterIndex;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")){
            httpMethod = Constants.HTTP_METHOD_DELETE;
            patMasterIndex = BeanUtil.toBean(dbMessage.getBeforeData(), PatMasterIndex.class);
        }else {
            httpMethod = Constants.HTTP_METHOD_POST;
            patMasterIndex = BeanUtil.toBean(dbMessage.getAfterData(), PatMasterIndex.class);
        }

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

    public void Diagnosis(DBMessage dbMessage) {
        logger.debug("诊断表DIAGNOSIS变更接口");
        logger.debug("DIAGNOSIS变更需调用emrFirstCourse、emrDailyCourse同步接口");
        String httpMethod = null;
        Diagnosis diagnosis;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")){
            httpMethod = Constants.HTTP_METHOD_DELETE;
            diagnosis = BeanUtil.toBean(dbMessage.getBeforeData(), Diagnosis.class);
        }else {
            httpMethod = Constants.HTTP_METHOD_POST;
            diagnosis = BeanUtil.toBean(dbMessage.getAfterData(), Diagnosis.class);
        }

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
            }

        }else {
            logger.error("{}对应PatMasterIndex或PatsInHospital信息为空，无法同步", diagnosis.getPatientId());
        }
    }
}
