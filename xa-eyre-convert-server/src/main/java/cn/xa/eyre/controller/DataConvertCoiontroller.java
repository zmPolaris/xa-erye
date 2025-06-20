package cn.xa.eyre.controller;

import cn.xa.eyre.common.core.domain.AjaxResult;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.service.*;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/convert")
public class DataConvertCoiontroller {

    Logger logger = org.slf4j.LoggerFactory.getLogger(DataConvertCoiontroller.class);

    @Autowired
    private DataConvertService dataConvertService;
    @Autowired
    private CommConvertService commConvertService;
    @Autowired
    private MedrecConvertService medrecConvertService;
    @Autowired
    private OutpdoctConvertService outpdoctConvertService;
    @Autowired
    private InpadmConvertService inpadmConvertService;

    /**
     * 部门信息转码
     * @param request
     * @return
     */
    @PostMapping("/convertDept")
    public AjaxResult convertDept(HttpServletRequest request){
        return AjaxResult.success("转码成功", dataConvertService.convertDept());
    }

    /**
     * 部门信息转码
     * @param request
     * @return
     */
    @PostMapping("/convertDiseaseIcd")
    public AjaxResult convertDiseaseIcd(HttpServletRequest request){
        return AjaxResult.success("转码成功", dataConvertService.convertDiseaseIcd());
    }

    @PostMapping("/receiveKafkaData")
    public AjaxResult receiveKafkaData(@RequestBody DBMessage dbMessage, HttpServletRequest request, HttpServletResponse response){
        logger.debug("receiveKafkaData:{}", dbMessage);
        String schema = dbMessage.getSchema();
        String table = dbMessage.getTable();
        String dbName = (schema + table).toLowerCase();
        switch (dbName) {
            case "comm.users":
                commConvertService.baseUser(dbMessage);
                break;
            case "comm.dept_dict":
                commConvertService.deptDict(dbMessage);
                break;
            case "medrec.pat_master_index":
                medrecConvertService.patMasterIndex(dbMessage);
                break;
            case "outpdoct.outp_mr":
                outpdoctConvertService.outpMr(dbMessage);
                break;
            case "inpadm.pats_in_hospital":
                inpadmConvertService.patsInHospital(dbMessage);
                break;
            case "medrec.diagnosis":
                medrecConvertService.Diagnosis(dbMessage);
                break;
            default:

        }
        return AjaxResult.success("转码成功");
    }
}
