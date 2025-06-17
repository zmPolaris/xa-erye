package cn.xa.eyre.service;

import cn.hutool.core.bean.BeanUtil;
import cn.xa.eyre.comm.domain.DeptDict;
import cn.xa.eyre.comm.domain.StaffDict;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.hub.domain.base.BaseDept;
import cn.xa.eyre.hub.domain.base.BaseUser;
import cn.xa.eyre.hub.service.SynchroBaseService;
import cn.xa.eyre.system.dict.domain.DictDisDept;
import cn.xa.eyre.system.dict.mapper.DictDisDeptMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CommConvertService {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DictDisDeptMapper dictDisDeptMapper;// 转码表
    @Autowired
    private SynchroBaseService synchroBaseService;

    public void deptDict(DBMessage dbMessage) {
        logger.debug("科室信息表DEPT_DICT变更接口");
        logger.debug("DEPT_DICT表变更需要调用baseDept同步接口");
        BaseDept baseDept = new BaseDept();
        String httpMethod = null;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")){
            httpMethod = Constants.HTTP_METHOD_DELETE;
            DeptDict deptDict = BeanUtil.toBean(dbMessage.getBeforeData(), DeptDict.class);
            DictDisDept deptParam = new DictDisDept();
            deptParam.setStatus(Constants.STATUS_NORMAL);
            deptParam.setEmrCode(deptDict.getDeptCode());
            // 根据HIS编码查询转码表，存在使用转码表数据，不存在则获取默认值
            DictDisDept dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
            if(dictDisDept == null){
                deptParam.setEmrName(null);
                deptParam.setIsDefault(Constants.IS_DEFAULT);
                dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
                dictDisDept.setEmrCode(deptDict.getDeptCode());
                dictDisDept.setEmrName(deptDict.getDeptName());
            }else {
                // 更新转码表状态
                dictDisDept.setStatus(Constants.STATUS_STOP);
                dictDisDept.setUpdateTime(DateUtils.getNowDate());
                dictDisDeptMapper.updateByPrimaryKey(dictDisDept);
            }
            baseDept.setDeptCode(dictDisDept.getEmrCode());
            baseDept.setDeptName(dictDisDept.getEmrName());
            baseDept.setTargetDeptCode(dictDisDept.getHubCode());
            baseDept.setTargetDeptName(dictDisDept.getHubName());
            baseDept.setCreateTime(DateUtils.getNowDate());
        }else {
            // 插入 修改
            httpMethod = Constants.HTTP_METHOD_POST;
            DeptDict deptDict = BeanUtil.toBean(dbMessage.getAfterData(), DeptDict.class);
            DictDisDept deptParam = new DictDisDept();
            deptParam.setStatus(Constants.STATUS_NORMAL);
            deptParam.setEmrCode(deptDict.getDeptCode());
            // 根据HIS编码查询转码表，存在使用转码表数据
            DictDisDept dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
            if(dictDisDept == null){
                List<DictDisDept> list = dictDisDeptMapper.selectAll();
                boolean isExist = false;
                for (DictDisDept dict : list) {
                    // 获取转码表中不存在的院内编码并包含科室名称的
                   if(dict.getHubName().contains(deptDict.getDeptName()) && !dict.getEmrCode().equals(deptDict.getDeptCode())) {
                       dictDisDept.setHubCode(dict.getHubCode());
                       dictDisDept.setHubName(dict.getHubName());
                       dictDisDept.setUpdateTime(DateUtils.getNowDate());
                       dictDisDeptMapper.updateByPrimaryKey(dictDisDept);
                       isExist = true;
                   }
                }
                // 不存在则获取默认前置软件代码
                if (!isExist){
                    deptParam.setEmrName(null);
                    deptParam.setIsDefault(Constants.IS_DEFAULT);
                    dictDisDept = dictDisDeptMapper.selectByCondition(deptParam);
                    dictDisDept.setEmrCode(deptDict.getDeptCode());
                    dictDisDept.setEmrName(deptDict.getDeptName());
                    dictDisDept.setCreateTime(DateUtils.getNowDate());
                    dictDisDept.setId(null);
                    dictDisDeptMapper.insertSelective(dictDisDept);
                }
            }else {
                // 更新转码表
                dictDisDept.setEmrCode(deptDict.getDeptCode());
                dictDisDept.setEmrName(deptDict.getDeptName());
                dictDisDept.setUpdateTime(DateUtils.getNowDate());
                dictDisDeptMapper.updateByPrimaryKey(dictDisDept);
            }
            baseDept.setDeptCode(dictDisDept.getEmrCode());
            baseDept.setDeptName(dictDisDept.getEmrName());
            baseDept.setTargetDeptCode(dictDisDept.getHubCode());
            baseDept.setTargetDeptName(dictDisDept.getHubName());
            baseDept.setCreateTime(DateUtils.getNowDate());
        }
        // 调用前置软件接口
        synchroBaseService.syncBaseDept(baseDept, httpMethod);
    }

    public void baseUser(DBMessage dbMessage) {
        logger.debug("系统用户信息base_user变更接口");
        logger.debug("STAFF_DICT表变更需要调用baseUser同步接口");
        BaseUser baseUser = new BaseUser();
        String httpMethod = null;
        StaffDict staffDict = null;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")) {
            // 删除
            httpMethod = Constants.HTTP_METHOD_DELETE;
            staffDict = BeanUtil.toBean(dbMessage.getBeforeData(), StaffDict.class);
        }else {
            // 插入 修改
            httpMethod = Constants.HTTP_METHOD_POST;
            staffDict = BeanUtil.toBean(dbMessage.getAfterData(), StaffDict.class);
        }
        // 院内用户ID
        baseUser.setId(staffDict.getEmpNo());
        baseUser.setOrgCode(staffDict.getDeptCode());
        baseUser.setDeptCode(staffDict.getDeptCode());
        baseUser.setUserName(staffDict.getName());
        baseUser.setIdCardTypeCode("01");
        baseUser.setLoginName(staffDict.getUserName());
        baseUser.setUserTypeCode("2");
        baseUser.setCreateTime(new Date());
        synchroBaseService.syncBaseUser(baseUser, httpMethod);
    }
}
