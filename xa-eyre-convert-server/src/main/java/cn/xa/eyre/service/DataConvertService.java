package cn.xa.eyre.service;

import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.hub.service.SynchroBaseService;
import cn.xa.eyre.system.dict.domain.DictDisDept;
import cn.xa.eyre.system.dict.mapper.DictDisDeptMapper;
import cn.xa.eyre.system.temp.domain.DictTemp;
import cn.xa.eyre.system.temp.domain.HisDeptDict;
import cn.xa.eyre.system.temp.mapper.DictTempMapper;
import cn.xa.eyre.system.temp.mapper.HisDeptDictMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
}
