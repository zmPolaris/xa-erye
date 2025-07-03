package cn.xa.eyre.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.xa.eyre.comm.domain.Users;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.domain.R;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.common.utils.StringUtils;
import cn.xa.eyre.exam.domain.ExamReport;
import cn.xa.eyre.hisapi.CommFeignClient;
import cn.xa.eyre.hisapi.InpadmFeignClient;
import cn.xa.eyre.hisapi.LabFeignClient;
import cn.xa.eyre.hisapi.MedrecFeignClient;
import cn.xa.eyre.hub.domain.emrmonitor.EmrExLab;
import cn.xa.eyre.hub.domain.emrmonitor.EmrExLabItem;
import cn.xa.eyre.hub.service.SynchroEmrMonitorService;
import cn.xa.eyre.hub.staticvalue.HubCodeEnum;
import cn.xa.eyre.inpadm.domain.PatsInHospital;
import cn.xa.eyre.lab.domain.LabResult;
import cn.xa.eyre.lab.domain.LabTestMaster;
import cn.xa.eyre.medrec.domain.PatMasterIndex;
import cn.xa.eyre.system.dict.domain.DictDisDept;
import cn.xa.eyre.system.dict.domain.DictExamType;
import cn.xa.eyre.system.dict.domain.DictSpecimenCategory;
import cn.xa.eyre.system.dict.mapper.DictDisDeptMapper;
import cn.xa.eyre.system.dict.mapper.DictSpecimenCategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LabConvertService {
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
    private InpadmFeignClient inpadmFeignClient;
    @Autowired
    private LabFeignClient labFeignClient;
    @Autowired
    private HubToolService hubToolService;
    @Autowired
    private DictSpecimenCategoryMapper dictSpecimenCategoryMapper;// 标本转码表

    public void labResult(DBMessage dbMessage) {
        logger.debug("检验结果表LAB_RESULT变更接口");
        logger.debug("LAB_RESULT变更需调用emrExLab、emrExLabItem同步接口");
        String httpMethod = null;
        LabResult labResult;
        Map<String, String> data;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")){
            httpMethod = Constants.HTTP_METHOD_DELETE;
            data = dbMessage.getBeforeData();
        }else {
            httpMethod = Constants.HTTP_METHOD_POST;
            data = dbMessage.getAfterData();
        }
        labResult = BeanUtil.toBeanIgnoreError(data, LabResult.class);

        R<LabTestMaster> labTestMasterResult = labFeignClient.getLabTestMaster(labResult.getTestNo());
        if (labTestMasterResult.getCode() == R.SUCCESS && labTestMasterResult.getData() != null){
            LabTestMaster labTestMaster = labTestMasterResult.getData();
            R<PatMasterIndex> medrecResult = medrecFeignClient.getPatMasterIndex(labTestMaster.getPatientId());
            if (R.SUCCESS == medrecResult.getCode() && medrecResult.getData() != null) {
                // 更新推送患者信息
                hubToolService.syncPatInfo(medrecResult.getData());
                DictDisDept dept = new DictDisDept();
                dept.setStatus(Constants.STATUS_NORMAL);
                dept.setIsDefault(Constants.IS_DEFAULT);
                DictDisDept dictDisDeptDefault = dictDisDeptMapper.selectByCondition(dept);

                logger.debug("构造emrExLab接口数据...");
                EmrExLab emrExLab = new EmrExLab();
                // ID使用LAB_RESULT表联合主键拼接计算MD5
                String id = DigestUtil.md5Hex(labResult.getTestNo() + labResult.getItemNo() + labResult.getPrintOrder());
                emrExLab.setId(id);
                emrExLab.setApplicationFormNo(String.valueOf(labResult.getItemNo()));

                emrExLab.setPatientId(labTestMaster.getPatientId());
                if("1".equals(labTestMaster.getPatientSource())){
                    emrExLab.setActivityTypeCode(HubCodeEnum.DIAGNOSIS_ACTIVITIES_OUTPATIENT.getCode());
                    emrExLab.setActivityTypeName(HubCodeEnum.DIAGNOSIS_ACTIVITIES_OUTPATIENT.getName());
                    emrExLab.setSerialNumber(DigestUtil.md5Hex(labTestMaster.getPatientId() + labTestMaster.getVisitNo()));
                }else if("2".equals(labTestMaster.getPatientSource())){
                    emrExLab.setActivityTypeCode(HubCodeEnum.DIAGNOSIS_ACTIVITIES_HOSPITALIZATION.getCode());
                    emrExLab.setActivityTypeName(HubCodeEnum.DIAGNOSIS_ACTIVITIES_HOSPITALIZATION.getName());
                    emrExLab.setSerialNumber(DigestUtil.md5Hex(labTestMaster.getPatientId() + labTestMaster.getVisitId()));
                    R<PatsInHospital> hospitalResult = inpadmFeignClient.getPatsInHospital(labTestMaster.getPatientId(), labTestMaster.getVisitId());
                    emrExLab.setWardNo(hospitalResult.getData().getWardCode());
                    emrExLab.setBedNo(String.valueOf(hospitalResult.getData().getBedNo()));
                }
                emrExLab.setApplyOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrExLab.setApplyOrgName(HubCodeEnum.ORG_CODE.getName());
                emrExLab.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrExLab.setOrgName(HubCodeEnum.ORG_CODE.getName());
                if(StringUtils.isNotBlank(labTestMaster.getOrderingDept())){
                    DictDisDept dictDisDept = hubToolService.getDept(labTestMaster.getOrderingDept());
                    emrExLab.setApplyDeptCode(dictDisDept.getHubCode());
                    emrExLab.setApplyDeptName(dictDisDept.getHubName());
                }
                R<Users> user = commFeignClient.getUserByName(labTestMaster.getOrderingProvider());
                if (R.SUCCESS == user.getCode() && user.getData() != null){
                    emrExLab.setApplyPhysicianId(user.getData().getUserId());
                }
                emrExLab.setExaminationReportDate(DateUtils.dateTime(labResult.getResultDateTime()));
                DictSpecimenCategory dictSpecimenCategory = dictSpecimenCategoryMapper.selectByEmrName(labTestMaster.getSpecimen());
                if(dictSpecimenCategory == null){
                    emrExLab.setSpecimenCategoryCode(HubCodeEnum.PAY_TYPE_OTHER.getCode());
                    emrExLab.setSpecimenCategoryName(labTestMaster.getSpecimen());
                }else {
                    emrExLab.setSpecimenCategoryCode(dictSpecimenCategory.getHubCode());
                    emrExLab.setSpecimenCategoryName(dictSpecimenCategory.getHubName());
                }

                emrExLab.setPatientName(medrecResult.getData().getName());
                if (StringUtils.isBlank(medrecResult.getData().getIdNo())){
                    emrExLab.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE_OTHER.getCode());
                    emrExLab.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE_OTHER.getName());
                    emrExLab.setIdCard("-");
                }else {
                    emrExLab.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                    emrExLab.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                    emrExLab.setIdCard(medrecResult.getData().getIdNo());
                }

                logger.debug("构造emrExLabItem接口数据...");
                EmrExLabItem emrExLabItem = new EmrExLabItem();
            }else {
                logger.error("{}对应PatMasterIndex信息为空，无法同步", labTestMaster.getPatientId());
            }
        }else {
            logger.error("{}LabTestMaster，无法同步", labResult.getTestNo());
        }
    }
}
