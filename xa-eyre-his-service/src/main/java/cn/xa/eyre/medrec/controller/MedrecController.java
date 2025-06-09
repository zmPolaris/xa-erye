package cn.xa.eyre.medrec.controller;

import cn.xa.eyre.common.core.domain.AjaxResult;
import cn.xa.eyre.medrec.service.MedrecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/medrec")
public class MedrecController {

    @Autowired
    private MedrecService medrecService;

    @GetMapping("/getPatMaster")
    public AjaxResult getMedrec(HttpServletRequest request){
        return AjaxResult.success("接口调用成功", medrecService.selectPatMasterIndex("0104180009"));
    }
}
