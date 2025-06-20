package cn.xa.eyre.service;

import cn.xa.eyre.comm.domain.Users;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.domain.R;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.hisapi.CommFeignClient;
import cn.xa.eyre.hisapi.MedrecFeignClient;
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

import java.util.List;

@Service
public class HubToolService {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private SynchroEmrRealService synchroEmrRealService;
    @Autowired
    private DdNationMapper ddNationMapper;
    @Autowired
    private CommFeignClient commFeignClient;
    @Autowired
    private MedrecFeignClient medrecFeignClient;
    public boolean synchroPatient(Integer num) {
        R<List<PatMasterIndex>> patsResult = medrecFeignClient.getPatMasterIndexList(num);
        if (R.SUCCESS == patsResult.getCode() && !patsResult.getData().isEmpty()){
            for (PatMasterIndex patMasterIndex : patsResult.getData()) {
                EmrPatientInfo emrPatientInfo = new EmrPatientInfo();
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
                // 查询操作员ID
                R<Users> user = commFeignClient.getUserByName(patMasterIndex.getOperator());
                if (R.SUCCESS == user.getCode() && user.getData() != null){
                    emrPatientInfo.setOperatorId(user.getData().getUserId());
                }
                emrPatientInfo.setOperationTime(DateUtils.getTime());
                synchroEmrRealService.syncEmrPatientInfo(emrPatientInfo, Constants.HTTP_METHOD_POST);
            }
            return true;
        }
        return false;
    }
}
