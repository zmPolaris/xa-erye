package cn.xa.eyre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 启动程序
 * 
 * @author zm
 */
@EnableTransactionManagement
@EnableFeignClients(basePackages = "cn.xa.eyre")
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableScheduling
public class EyreConvertServerApplication
{
    private static final Logger logger = LoggerFactory.getLogger(EyreConvertServerApplication.class);

    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(EyreConvertServerApplication.class, args);
        logger.info("前置软件数据转换服务启动成功");
    }
}
