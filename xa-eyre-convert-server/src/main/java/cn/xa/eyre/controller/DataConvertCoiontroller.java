package cn.xa.eyre.controller;

import cn.xa.eyre.common.core.domain.AjaxResult;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.service.DataConvertService;
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

    /**
     * 部门信息转码
     * @param request
     * @return
     */
    @PostMapping("/convertDept")
    public AjaxResult convertDept(HttpServletRequest request){
        return AjaxResult.success("转码成功", dataConvertService.convertDept());
    }

    @PostMapping("/receiveKafkaData")
    public AjaxResult receiveKafkaData(@RequestBody DBMessage dbMessage, HttpServletRequest request, HttpServletResponse response){
        logger.debug("receiveKafkaData:{}", dbMessage);
        String schema = dbMessage.getSchema();
        String table = dbMessage.getTable();
        String dbName = (schema + table).toLowerCase();
        switch (dbName) {
            case "comm.staff_dict":
                dataConvertService.baseUser(dbMessage);
                break;
            case "comm.dept_dict":
                dataConvertService.baseDept(dbMessage);
                break;
            case "eyre_user_role":

                break;
            case "eyre_role":

                break;
            default:

        }
        return AjaxResult.success("转码成功");
    }
}
