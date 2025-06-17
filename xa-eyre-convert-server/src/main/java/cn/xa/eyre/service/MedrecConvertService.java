package cn.xa.eyre.service;

import cn.hutool.core.bean.BeanUtil;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.hub.domain.emrreal.EmrPatientInfo;
import cn.xa.eyre.hub.service.SynchroEmrRealService;
import cn.xa.eyre.hub.staticvalue.HubCodeEnum;
import cn.xa.eyre.medrec.domain.PatMasterIndex;
import cn.xa.eyre.system.dict.domain.DdNation;
import cn.xa.eyre.system.dict.mapper.DdNationMapper;
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
        // 构造请求参数
        emrPatientInfo.setId(patMasterIndex.getPatientId());
        emrPatientInfo.setPatientName(patMasterIndex.getName());
        emrPatientInfo.setIdCardTypeCode(HubCodeEnum.ID_CARD_TYPE.getCode());
        emrPatientInfo.setIdCardTypeName(HubCodeEnum.ID_CARD_TYPE.getName());
        emrPatientInfo.setIdCard(patMasterIndex.getIdNo());
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
        // 查询操作ID
        emrPatientInfo.setOperationTime(DateUtils.getTime());
        synchroEmrRealService.syncEmrPatientInfo(emrPatientInfo, httpMethod);
    }
}
