package cn.xa.eyre.hisapi;


import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(name = "medrecService", url = Constants.HIS_URL)
public interface MedrecFeignClient {

    @GetMapping("/medrec/getPatMasterIndex/{patientId}")
    public AjaxResult getMedrec(@PathVariable("patientId") String patientId);
}
