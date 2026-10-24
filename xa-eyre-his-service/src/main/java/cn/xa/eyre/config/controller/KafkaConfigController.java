package cn.xa.eyre.config.controller;

import cn.xa.eyre.common.core.domain.AjaxResult;
import cn.xa.eyre.config.service.KafkaPushConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kafka数据推送时效配置接口（his-service，代理侧）
 * <p>
 * 权威存储在convert-server（sys_config数据库+Redis缓存），本服务经Feign代理查询与修改，
 * 使消费端运维人员无需跨服务即可维护该配置。
 * </p>
 *
 * @author zm
 */
@RestController
@RequestMapping("/config")
public class KafkaConfigController
{
    @Autowired
    private KafkaPushConfigService kafkaPushConfigService;

    /**
     * 查询当前推送时效天数
     */
    @GetMapping("/pushExpireDays")
    public AjaxResult getPushExpireDays()
    {
        return AjaxResult.success("查询成功", kafkaPushConfigService.getExpireDays());
    }

    /**
     * 修改推送时效天数（经Feign写convert-server的sys_config+Redis）
     *
     * @param days 时效天数，必须为1-3650之间的整数
     */
    @PostMapping("/pushExpireDays")
    public AjaxResult updatePushExpireDays(@RequestParam("days") Integer days)
    {
        if (days == null)
        {
            return AjaxResult.error("推送时效天数不能为空");
        }
        try
        {
            kafkaPushConfigService.updateExpireDays(days);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success("修改成功", kafkaPushConfigService.getExpireDays());
    }
}
