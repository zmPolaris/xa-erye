package cn.xa.eyre.convertapi;


import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(name = "convertService", url = Constants.CONVERT_URL)
public interface ConvertFeignClient {

    @PostMapping("/test/getCofig")
    public AjaxResult getCofig();

    /**
     * 查询Kafka数据推送时效天数（权威存储在convert-server的sys_config+Redis）
     */
    @GetMapping("/convert/config/pushExpireDays")
    public AjaxResult getPushExpireDays();

    /**
     * 修改Kafka数据推送时效天数（convert-server负责写sys_config数据库与Redis缓存）
     */
    @PostMapping("/convert/config/pushExpireDays")
    public AjaxResult updatePushExpireDays(@RequestParam("days") int days);
}
