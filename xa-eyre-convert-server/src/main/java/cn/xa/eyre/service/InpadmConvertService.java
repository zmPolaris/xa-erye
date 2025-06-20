package cn.xa.eyre.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.domain.R;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.hisapi.MedrecFeignClient;
import cn.xa.eyre.hub.domain.emrmonitor.EmrAdmissionInfo;
import cn.xa.eyre.hub.service.SynchroEmrMonitorService;
import cn.xa.eyre.hub.staticvalue.HubCodeEnum;
import cn.xa.eyre.inpadm.domain.PatsInHospital;
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
public class InpadmConvertService {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private MedrecFeignClient medrecFeignClient;
    @Autowired
    private SynchroEmrMonitorService synchroEmrMonitorService;
    @Autowired
    private DictDisDeptMapper dictDisDeptMapper;// 转码表

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

            PatMasterIndex patMasterIndex = medrecResult.getData();
            emrAdmissionInfo.setPatientName(patMasterIndex.getName());
            emrAdmissionInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
            emrAdmissionInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
            emrAdmissionInfo.setIdCard(patMasterIndex.getIdNo());

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

            emrAdmissionInfo.setOrgCode(HubCodeEnum.ORG_CODE.getCode());
            emrAdmissionInfo.setOrgName(HubCodeEnum.ORG_CODE.getName());

            synchroEmrMonitorService.syncEmrAdmissionInfo(emrAdmissionInfo, httpMethod);
        }else {
            logger.error("对应PatMasterIndex信息为空，无法同步");
        }
    }
}
