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
import cn.xa.eyre.hub.service.SynchroEmrMonitorService;
import cn.xa.eyre.hub.staticvalue.HubCodeEnum;
import cn.xa.eyre.medrec.domain.PatMasterIndex;
import cn.xa.eyre.outpadm.domain.ClinicMaster;
import cn.xa.eyre.outpdoct.domain.OutpMr;
import cn.xa.eyre.system.dict.domain.DictDisDept;
import cn.xa.eyre.system.dict.mapper.DictDisDeptMapper;
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
    private DictDisDeptMapper dictDisDeptMapper;// 转码表
    @Autowired
    private SynchroEmrMonitorService synchroEmrMonitorService;
    @Autowired
    private CommFeignClient commFeignClient;

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

        logger.debug("构造emrOutpatientRecord接口数据...");
        if (StrUtil.isNotBlank(outpMr.getPatientId())){
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
                // 需转码
//                emrOutpatientRecord.setWmDiagnosisCode("");
                emrOutpatientRecord.setWmDiagnosisName(outpMr.getDiagDesc());
                emrOutpatientRecord.setTreatment(outpMr.getAdvice());

                PatMasterIndex patMasterIndex = medrecResult.getData();
                emrOutpatientRecord.setPatientName(patMasterIndex.getName());
                emrOutpatientRecord.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
                emrOutpatientRecord.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
                emrOutpatientRecord.setIdCard(patMasterIndex.getIdNo());

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
                R<Users> user = commFeignClient.getUserByName(patMasterIndex.getOperator());
                if (R.SUCCESS == user.getCode() && user.getData() != null){
                    emrOutpatientRecord.setOperatorId(user.getData().getUserId());
                }

                emrOutpatientRecord.setDeptCode(dictDisDept.getHubCode());
                emrOutpatientRecord.setDeptName(dictDisDept.getHubName());

                emrOutpatientRecord.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
                emrOutpatientRecord.setOrgName(HubCodeEnum.ORG_CODE.getName());

                synchroEmrMonitorService.syncEmrOutpatientRecord(emrOutpatientRecord, httpMethod);
            }else {
                logger.error("对应PatMasterIndex信息或ClinicMaster信息为空，无法同步");
            }
        }else {
            logger.error("patientId为空，无法同步");
        }

    }
}
