package cn.xa.eyre.controller;

import cn.xa.eyre.common.core.domain.AjaxResult;
import cn.xa.eyre.service.DataConvertService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/convert")
public class DataConvertCoiontroller {

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
}
