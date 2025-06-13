package cn.xa.eyre.medrec.controller;

import cn.xa.eyre.common.core.domain.AjaxResult;
import cn.xa.eyre.medrec.service.MedrecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/medrec")
public class MedrecController {

    @Autowired
    private MedrecService medrecService;

    @GetMapping("/getPatMasterIndex/{patientId}")
    public AjaxResult getPatMasterIndex(@PathVariable("patientId") String patientId){
        // 0104180009
        return AjaxResult.success("接口调用成功", medrecService.selectPatMasterIndex(patientId));
    }
}
