package cn.xa.eyre.config.service;

import cn.xa.eyre.common.constant.CacheConstants;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.constant.HttpStatus;
import cn.xa.eyre.common.core.domain.AjaxResult;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.core.kafka.TableCreateTimeResolver;
import cn.xa.eyre.common.core.redis.RedisCache;
import cn.xa.eyre.common.exception.ServiceException;
import cn.xa.eyre.convertapi.ConvertFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Kafka数据推送时效配置服务（his-service，消费端预过滤侧）
 * <p>
 * his-service的数据源只有Oracle HIS，无法直接访问convert-server的sys_config表，
 * 因此阈值获取采用两级策略：
 * 1. 读共享Redis（与convert-server同一实例、同一key，由SysConfigServiceImpl双写维护）；
 * 2. 未命中时经ConvertFeignClient调convert-server接口获取（权威源：sys_config数据库+Redis），并回写共享缓存；
 * 3. 远程也不可用时兜底默认15天，保证Kafka消费链路不中断。
 * </p>
 *
 * @author zm
 */
@Service
public class KafkaPushConfigService
{
    private static final Logger logger = LoggerFactory.getLogger(KafkaPushConfigService.class);

    /** 时效天数合法范围上限，与convert-server保持一致 */
    private static final int MAX_EXPIRE_DAYS = 3650;

    /**
     * 共享缓存key：与convert-server的sys_config Redis key保持一致（SYS_CONFIG_KEY + configKey），
     * 这样convert-server修改配置后本端能立即读到新值。
     */
    private static final String CACHE_KEY = CacheConstants.SYS_CONFIG_KEY + Constants.CONFIG_XA_ERYE_KAFKA_PUSH_EXPIRE_DAYS;

    @Autowired
    private RedisCache redisCache;
    @Autowired
    private ConvertFeignClient convertFeignClient;

    /**
     * 查询当前推送时效天数（共享Redis -> Feign远程 -> 默认值）
     *
     * @return 时效天数（1-3650）
     */
    public int getExpireDays()
    {
        // 1. 共享Redis缓存（convert-server写入的值为字符串，兼容Integer类型做防御）
        try
        {
            Object cached = redisCache.getCacheObject(CACHE_KEY);
            if (cached != null)
            {
                int days = Integer.parseInt(cached.toString().trim());
                if (days >= 1 && days <= MAX_EXPIRE_DAYS)
                {
                    return days;
                }
                logger.warn("共享缓存中的推送时效天数[{}]非法，转远程获取", cached);
            }
        }
        catch (Exception e)
        {
            // 热路径（每条Kafka消息都会走）：只打一行摘要，避免连接异常时全量堆栈刷爆日志
            logger.warn("读取推送时效共享缓存失败，转远程获取: {}", e.toString());
        }

        // 2. Feign远程获取（convert-server权威存储）
        try
        {
            AjaxResult result = convertFeignClient.getPushExpireDays();
            if (result != null && Objects.equals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG))
                    && result.get(AjaxResult.DATA_TAG) != null)
            {
                int days = Integer.parseInt(result.get(AjaxResult.DATA_TAG).toString().trim());
                if (days >= 1 && days <= MAX_EXPIRE_DAYS)
                {
                    // 回写共享缓存失败不影响本次取值（Redis故障时不应退化成默认值）
                    try
                    {
                        redisCache.setCacheObject(CACHE_KEY, String.valueOf(days));
                    }
                    catch (Exception e)
                    {
                        logger.warn("回写推送时效共享缓存失败（不影响本次取值）: {}", e.toString());
                    }
                    return days;
                }
            }
            logger.warn("远程获取推送时效天数失败或返回值非法: {}", result);
        }
        catch (Exception e)
        {
            logger.warn("远程获取推送时效天数异常，使用默认值: {}", e.toString());
        }

        // 3. 兜底默认值，保证消费不中断
        return Constants.KAFKA_PUSH_EXPIRE_DAYS_DEFAULT;
    }

    /**
     * 修改推送时效天数：经Feign代理到convert-server（由其写sys_config数据库+Redis缓存），
     * 成功后删除共享缓存，下次读取按最新值回源。
     *
     * @param days 时效天数，必须为1-3650之间的整数
     * @throws ServiceException 参数非法或远端修改失败时抛出
     */
    public void updateExpireDays(int days)
    {
        if (days < 1 || days > MAX_EXPIRE_DAYS)
        {
            throw new ServiceException("推送时效天数必须为1-" + MAX_EXPIRE_DAYS + "之间的整数");
        }
        AjaxResult result = convertFeignClient.updatePushExpireDays(days);
        if (result == null || !Objects.equals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG)))
        {
            String msg = result == null ? "远端无响应" : String.valueOf(result.get(AjaxResult.MSG_TAG));
            throw new ServiceException("修改推送时效天数失败：" + msg);
        }
        // 删除共享缓存，强制下次读取回源到最新值（convert-server侧已双写，此处为双保险）；
        // 删除失败不影响修改结果（convert-server已双写新值），仅记录日志
        try
        {
            redisCache.deleteObject(CACHE_KEY);
        }
        catch (Exception e)
        {
            logger.warn("删除推送时效共享缓存失败（不影响修改结果）: {}", e.toString());
        }
        logger.info("推送时效天数已通过convert-server修改为{}天", days);
    }

    /**
     * 判断Kafka消息是否已超过推送时效（消费端预过滤用）。
     * 创建时间无法解析时放行（返回false）。
     *
     * @param dbMessage Kafka消息
     * @return true=已超期，调用方应跳过不再转发
     */
    public boolean isExpired(DBMessage dbMessage)
    {
        if (dbMessage == null)
        {
            return false;
        }
        int expireDays = getExpireDays();
        boolean expired = TableCreateTimeResolver.isExpired(dbMessage.getSchema(), dbMessage.getTable(),
                dbMessage.getOperation(), dbMessage.getBeforeData(), dbMessage.getAfterData(), expireDays);
        if (expired)
        {
            logger.warn("数据超过推送时效不转发：{}.{}，创建时间：{}，时效阈值：{}天",
                    dbMessage.getSchema(), dbMessage.getTable(),
                    TableCreateTimeResolver.formatDate(TableCreateTimeResolver.resolveCreateTime(
                            dbMessage.getSchema(), dbMessage.getTable(), dbMessage.getOperation(),
                            dbMessage.getBeforeData(), dbMessage.getAfterData())), expireDays);
        }
        return expired;
    }
}
