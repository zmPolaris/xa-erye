package cn.xa.eyre.service;

import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.constant.UserConstants;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.core.kafka.TableCreateTimeResolver;
import cn.xa.eyre.common.exception.ServiceException;
import cn.xa.eyre.system.system.domain.SysConfig;
import cn.xa.eyre.system.system.service.ISysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Kafka数据推送时效配置服务（convert-server，权威存储侧）
 * <p>
 * 阈值持久化于sys_config表（数据库），经ISysConfigService自动同步Redis缓存：
 * 读取时缓存优先、未命中回源数据库并回写；修改时数据库与缓存双写。
 * </p>
 *
 * @author zm
 */
@Service
public class KafkaPushConfigService
{
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /** 时效天数合法范围上限（约10年），防止误配置极端值 */
    private static final int MAX_EXPIRE_DAYS = 3650;

    @Autowired
    private ISysConfigService sysConfigService;

    /**
     * 查询当前推送时效天数。
     * 配置缺失或非法时返回默认值{@link Constants#KAFKA_PUSH_EXPIRE_DAYS_DEFAULT}（15天）。
     *
     * @return 时效天数（1-3650）
     */
    public int getExpireDays()
    {
        String value = sysConfigService.selectConfigByKey(Constants.CONFIG_XA_ERYE_KAFKA_PUSH_EXPIRE_DAYS);
        int days = -1;
        try
        {
            if (value != null && !value.trim().isEmpty())
            {
                days = Integer.parseInt(value.trim());
            }
        }
        catch (NumberFormatException e)
        {
            days = -1;
        }
        if (days < 1 || days > MAX_EXPIRE_DAYS)
        {
            logger.warn("推送时效天数配置[{}]缺失或非法，使用默认值{}天", value, Constants.KAFKA_PUSH_EXPIRE_DAYS_DEFAULT);
            return Constants.KAFKA_PUSH_EXPIRE_DAYS_DEFAULT;
        }
        return days;
    }

    /**
     * 修改推送时效天数（数据库+缓存双写）。
     * 配置行已存在则按configId更新，不存在则新增。
     *
     * @param days 时效天数，必须在1-3650之间
     * @throws ServiceException 参数非法时抛出
     */
    public void updateExpireDays(int days)
    {
        if (days < 1 || days > MAX_EXPIRE_DAYS)
        {
            throw new ServiceException("推送时效天数必须为1-" + MAX_EXPIRE_DAYS + "之间的整数");
        }
        SysConfig config = new SysConfig();
        config.setConfigKey(Constants.CONFIG_XA_ERYE_KAFKA_PUSH_EXPIRE_DAYS);
        config.setConfigValue(String.valueOf(days));
        SysConfig exist = sysConfigService.selectSysConfigByKey(Constants.CONFIG_XA_ERYE_KAFKA_PUSH_EXPIRE_DAYS);
        if (exist != null)
        {
            // 已存在：按主键更新，保留原名称与类型
            config.setConfigId(exist.getConfigId());
            config.setConfigName(exist.getConfigName());
            config.setConfigType(exist.getConfigType());
            sysConfigService.updateConfig(config);
        }
        else
        {
            // 不存在：新增（与sql种子数据保持一致）
            config.setConfigName("Kafka数据推送时效天数");
            config.setConfigType(UserConstants.YES);
            config.setCreateBy("admin");
            sysConfigService.insertConfig(config);
        }
        logger.info("推送时效天数已修改为{}天", days);
    }

    /**
     * 判断Kafka消息是否已超过推送时效（convert-server入口兜底校验用）。
     * 创建时间无法解析时放行（返回false），并记warn日志。
     *
     * @param dbMessage Kafka消息
     * @return true=已超期，调用方应跳过不再推送
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
            logger.warn("数据超过推送时效已跳过：{}.{}，创建时间：{}，时效阈值：{}天",
                    dbMessage.getSchema(), dbMessage.getTable(),
                    TableCreateTimeResolver.formatDate(TableCreateTimeResolver.resolveCreateTime(
                            dbMessage.getSchema(), dbMessage.getTable(), dbMessage.getOperation(),
                            dbMessage.getBeforeData(), dbMessage.getAfterData())), expireDays);
        }
        return expired;
    }
}
