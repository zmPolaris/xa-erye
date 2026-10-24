package cn.xa.eyre.hub.service;

import cn.hutool.json.JSONUtil;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.common.utils.http.HttpClientUtils;
import cn.xa.eyre.hub.domain.SynchroResponseVo;
import cn.xa.eyre.hub.domain.emrreal.EmrActivityInfo;
import cn.xa.eyre.hub.domain.emrreal.EmrInfReport;
import cn.xa.eyre.hub.domain.emrreal.EmrPatientInfo;
import cn.xa.eyre.hub.staticvalue.SyncApiStaticValue;
import cn.xa.eyre.system.log.domain.LogSynchroApiWithBLOBs;
import cn.xa.eyre.system.log.mapper.LogSynchroApiMapper;
import cn.xa.eyre.system.system.service.ISysConfigService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 实时采集数据同步
 */
@Service
public class SynchroEmrRealService {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ISysConfigService sysConfigService;
    @Autowired
    private LogSynchroApiMapper logSynchroApiMapper;
    /** 前置软件服务地址：启动时加载一次，避免每次同步都查配置库（原实现存在多线程字段竞争） */
    private volatile String serviceUrl;
    /** 是否保存接口日志：启动时加载一次，原实现只置true从不复位，一旦开启即永久生效 */
    private volatile boolean isSaveLog = false;
    private volatile boolean configLoaded = false;

    /**
     * 患者基本信息表数据操作 API 接口
     * @param emrPatientInfo
     * @param method
     * @return
     */
    public void syncEmrPatientInfo(EmrPatientInfo emrPatientInfo, String method){
        init();
        String apiUrl = serviceUrl + SyncApiStaticValue.API_EMR_PATIENT_INFO;
        LogSynchroApiWithBLOBs log = new LogSynchroApiWithBLOBs();
        log.setUrl(apiUrl);
        log.setMethodName(method);
        String requestJson = JSONUtil.toJsonStr(emrPatientInfo);
        log.setInputParams(requestJson);
        logger.debug("->{},url:{},request:{},method:{}", "患者基本信息表数据操作 API 接口", apiUrl, requestJson, method);
        try {
            String result = HttpClientUtils.sendHttpRequest(apiUrl, requestJson, method);
            logger.debug("->{},response:{}", "患者基本信息表数据操作 API 接口", result);
            SynchroResponseVo synchroResponseVo = JSONObject.parseObject(result, new TypeReference<SynchroResponseVo>(){});
            log.setOutputParams(JSONUtil.toJsonStr(synchroResponseVo));
            if (synchroResponseVo.isResult()){
                log.setResult(Constants.API_STATUS_SUCCESS);
            }else {
                log.setResult(Constants.API_STATUS_FAIL);
            }
        } catch (Exception e) {
            logger.error("同步接口调用失败，url:{}", apiUrl, e);
            log.setResult(Constants.API_STATUS_FAIL);
        }
        if(isSaveLog){
            log.setAddTime(DateUtils.getNowDate());
            logSynchroApiMapper.insertSelective(log);
        }
    }

    /**
     * 诊疗活动信息表数据操作 API 接口
     * @param emrActivityInfo
     * @param method
     * @return
     */
    public void syncEmrActivityInfo(EmrActivityInfo emrActivityInfo, String method){
        init();
        String apiUrl = serviceUrl + SyncApiStaticValue.API_EMR_ACTIVITY_INFO;
        LogSynchroApiWithBLOBs log = new LogSynchroApiWithBLOBs();
        log.setUrl(apiUrl);
        log.setMethodName(method);
        String requestJson = JSONUtil.toJsonStr(emrActivityInfo);
        log.setInputParams(requestJson);
        logger.debug("->{},url:{},request:{},method:{}", "诊疗活动信息表数据操作 API 接口", apiUrl, requestJson, method);
        try {
            String result = HttpClientUtils.sendHttpRequest(apiUrl, requestJson, method);
            logger.debug("->{},response:{}", "诊疗活动信息表数据操作 API 接口", result);
            SynchroResponseVo synchroResponseVo =  JSONObject.parseObject(result, new TypeReference<SynchroResponseVo>(){});
            log.setOutputParams(JSONUtil.toJsonStr(synchroResponseVo));
            if (synchroResponseVo.isResult()){
                log.setResult(Constants.API_STATUS_SUCCESS);
            }else {
                log.setResult(Constants.API_STATUS_FAIL);
            }
        } catch (Exception e) {
            logger.error("同步接口调用失败，url:{}", apiUrl, e);
            log.setResult(Constants.API_STATUS_FAIL);
        }
        if(isSaveLog){
            log.setAddTime(DateUtils.getNowDate());
            logSynchroApiMapper.insertSelective(log);
        }
    }

    /**
     * 传染病报告卡数据操作 API 接口
     * @param emrInfReport
     * @param method
     * @return
     */
    public void syncEmrInfReport(EmrInfReport emrInfReport, String method){
        init();
        String apiUrl = serviceUrl + SyncApiStaticValue.API_EMR_INF_REPORT;
        LogSynchroApiWithBLOBs log = new LogSynchroApiWithBLOBs();
        log.setUrl(apiUrl);
        log.setMethodName(method);
        String requestJson = JSONUtil.toJsonStr(emrInfReport);
        log.setInputParams(requestJson);
        logger.debug("->{},url:{},request:{},method:{}", "传染病报告卡数据操作 API 接口", apiUrl, requestJson, method);
        try {
            String result = HttpClientUtils.sendHttpRequest(apiUrl, requestJson, method);
            logger.debug("->{},response:{}", "传染病报告卡数据操作 API 接口", result);
            SynchroResponseVo synchroResponseVo =  JSONObject.parseObject(result, new TypeReference<SynchroResponseVo>(){});
            log.setOutputParams(JSONUtil.toJsonStr(synchroResponseVo));
            if (synchroResponseVo.isResult()){
                log.setResult(Constants.API_STATUS_SUCCESS);
            }else {
                log.setResult(Constants.API_STATUS_FAIL);
            }
        } catch (Exception e) {
            logger.error("同步接口调用失败，url:{}", apiUrl, e);
            log.setResult(Constants.API_STATUS_FAIL);
        }
        if(isSaveLog){
            log.setAddTime(DateUtils.getNowDate());
            logSynchroApiMapper.insertSelective(log);
        }
    }


    private void init(){
        if (configLoaded) {
            return;
        }
        synchronized (this) {
            if (configLoaded) {
                return;
            }
            serviceUrl = sysConfigService.selectConfigByKey(Constants.CONFIG_XA_ERYE_HUB_SERVICE_URL);
            int saveLog = Integer.valueOf(sysConfigService.selectConfigByKey(Constants.CONFIG_XA_ERYE_SAVE_API_LOG));
            // 直接赋值：原实现只在等于YES时置true，关闭后无法复位
            isSaveLog = saveLog == Constants.SAVE_API_LOG_YES;
            configLoaded = true;
        }
    }
}
