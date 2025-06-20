package cn.xa.eyre.hisapi;


import cn.xa.eyre.comm.domain.Users;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.domain.R;
import cn.xa.eyre.outpadm.domain.ClinicMaster;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(name = "commService", url = Constants.HIS_URL)
public interface CommFeignClient {

    @GetMapping("/comm/getUserByName")
    public R<Users> getUserByName(@RequestParam("userName") String userName);
}
