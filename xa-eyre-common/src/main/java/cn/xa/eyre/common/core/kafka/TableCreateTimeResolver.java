package cn.xa.eyre.common.core.kafka;

import cn.xa.eyre.common.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka消息业务表创建时间解析器（推送时效控制的单一事实源）
 * <p>
 * 按表从Kafka消息数据中解析业务创建时间，供消费端（his-service）与转换入口
 * （convert-server）共用，避免两端各维护一份表字段映射导致判断不一致。
 * </p>
 * 使用方式：
 * <pre>
 *     Date createTime = TableCreateTimeResolver.resolve(schema, table, data);
 * </pre>
 *
 * @author zm
 */
public class TableCreateTimeResolver
{
    /**
     * 表名（小写schema.table） -> 有序候选创建时间字段列表（驼峰key，与DBMessage中afterData/beforeData的key一致）。
     * 按优先级依次尝试，取第一个可解析的值。
     * 空列表表示该表没有创建时间字段（如COMM.DEPT_DICT），无法判断时效，调用方应放行。
     */
    private static final Map<String, List<String>> TABLE_CREATE_TIME_FIELDS;

    static
    {
        Map<String, List<String>> map = new HashMap<>();
        map.put("comm.users", Arrays.asList("createDate"));
        // 科室字典表无任何时间字段，无法判断时效
        map.put("comm.dept_dict", Collections.emptyList());
        map.put("medrec.pat_master_index", Arrays.asList("createDate"));
        // 门诊病历无独立创建时间列，以就诊日期作为业务时间
        map.put("outpdoct.outp_mr", Arrays.asList("visitDate"));
        map.put("inpadm.pats_in_hospital", Arrays.asList("admissionDateTime", "startDateTime"));
        map.put("medrec.diagnosis", Arrays.asList("diagnosisDate"));
        map.put("medrec.pat_visit", Arrays.asList("admissionDateTime", "catalogDate"));
        map.put("exam.exam_master", Arrays.asList("reqDateTime", "visitDate"));
        map.put("lab.lab_test_master", Arrays.asList("requestedDateTime", "executeDate"));
        map.put("pharmacy.drug_presc_master", Arrays.asList("prescDate", "enteredDatetime"));
        map.put("lqt2024.lqt_crb_zd", Arrays.asList("createTime", "activityTime"));
        TABLE_CREATE_TIME_FIELDS = Collections.unmodifiableMap(map);
    }

    private TableCreateTimeResolver()
    {
    }

    /**
     * 解析指定表的业务创建时间
     *
     * @param schema 库名（如COMM）
     * @param table  表名（如USERS）
     * @param data   Kafka消息数据（INSERT/UPDATE取afterData，DELETE取beforeData），key为驼峰字段名
     * @return 创建时间；无映射表、无时间字段、字段缺失或不可解析时返回null（调用方按放行处理）
     */
    public static Date resolve(String schema, String table, Map<String, String> data)
    {
        if (data == null || data.isEmpty() || StringUtils.isBlank(schema) || StringUtils.isBlank(table))
        {
            return null;
        }
        List<String> fields = TABLE_CREATE_TIME_FIELDS.get((schema + "." + table).toLowerCase());
        if (fields == null || fields.isEmpty())
        {
            return null;
        }
        for (String field : fields)
        {
            Date date = parseDateValue(data.get(field));
            if (date != null)
            {
                return date;
            }
        }
        return null;
    }

    /**
     * 按操作类型解析创建时间：DELETE用beforeData（删除后afterData为空），INSERT/UPDATE用afterData
     *
     * @return 创建时间；无法解析时返回null
     */
    public static Date resolveCreateTime(String schema, String table, String operation,
                                         Map<String, String> beforeData, Map<String, String> afterData)
    {
        Map<String, String> data = "DELETE".equalsIgnoreCase(operation) ? beforeData : afterData;
        return resolve(schema, table, data);
    }

    /**
     * 判断Kafka消息是否已超过推送时效
     * <p>
     * 规则：创建时间距当前时间超过 expireDays 天则判定为超期（不再推送）。
     * 创建时间无法解析时返回false（放行）——宁可推送不漏数据。
     * </p>
     *
     * @param expireDays 时效阈值（天），调用方负责从配置获取并保证为正数
     * @return true=已超期应跳过；false=未超期或无法判断
     */
    public static boolean isExpired(String schema, String table, String operation,
                                    Map<String, String> beforeData, Map<String, String> afterData, int expireDays)
    {
        Date createTime = resolveCreateTime(schema, table, operation, beforeData, afterData);
        if (createTime == null)
        {
            return false;
        }
        long expireMillis = expireDays * 24L * 3600L * 1000L;
        return System.currentTimeMillis() - createTime.getTime() > expireMillis;
    }

    /**
     * 解析单个字段值：兼容Debezium两种常见输出
     * 1) epoch毫秒字符串（纯数字且长度>=12，如"1730000000000"）
     * 2) 日期字符串（yyyy-MM-dd HH:mm:ss / yyyy-MM-dd / yyyy/MM/dd 等，由DateUtils.parsePatterns覆盖）
     * <p>
     * 注意：毫秒直接按epoch构造Date，不做时区平移——时效判断是"距当前时间的 elapsed 比较"，
     * 与DateUtils.getLongDate（面向展示的东八区平移）语义不同，平移反而会引入8小时误差。
     * </p>
     */
    private static Date parseDateValue(String value)
    {
        if (StringUtils.isBlank(value) || "null".equalsIgnoreCase(value.trim()))
        {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 12 && isAllDigits(trimmed))
        {
            try
            {
                return new Date(Long.parseLong(trimmed));
            }
            catch (NumberFormatException e)
            {
                // 数字越界时落入下方字符串解析
            }
        }
        return DateUtils.parseDate(trimmed);
    }

    private static boolean isAllDigits(String s)
    {
        for (int i = 0; i < s.length(); i++)
        {
            if (!Character.isDigit(s.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 格式化创建时间用于日志输出（两端共用，避免各写一份格式）
     *
     * @param date 创建时间，null时返回"未知"
     * @return yyyy-MM-dd HH:mm:ss 格式字符串
     */
    public static String formatDate(Date date)
    {
        if (date == null)
        {
            return "未知";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
