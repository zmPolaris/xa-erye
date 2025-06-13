package cn.xa.eyre.controller;

import cn.xa.eyre.common.core.domain.AjaxResult;
import cn.xa.eyre.hisapi.MedrecFeignClient;
import cn.xa.eyre.system.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ISysConfigService sysConfigService;
    @Autowired
    private MedrecFeignClient medrecFeignClient;

    @PostMapping("/getCofig")
    public AjaxResult addOperator(HttpServletRequest request){
        return AjaxResult.success("测试成功", sysConfigService.selectConfigByKey("snca.ycpc.ca.message.templateId"));
    }

    @GetMapping("/getPatMasterIndex/{patientId}")
    public AjaxResult getPatMasterIndex(@PathVariable("patientId") String patientId){
        return medrecFeignClient.getMedrec(patientId);
    }
}
