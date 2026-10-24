-- ===============================================================
-- Kafka数据推送时效天数配置（种子数据）
-- 作用数据库：convert_db（MySQL，sys_config所在库）
-- 说明：数据创建时间距当前时间超过该天数则不再推送（消费端预过滤 + convert-server入口兜底）。
--       配置同时持久化于本表与Redis缓存（key: sys_config:xa.erye.kafka.push.expire.days），
--       由SysConfigServiceImpl维护双写；代码侧对配置缺失有默认15天兜底，本脚本仅作显式持久化。
-- 执行一次即可；重复执行请先删除已存在的行（见文末注释）。
-- ===============================================================

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, remark, create_time)
VALUES ('Kafka数据推送时效天数', 'xa.erye.kafka.push.expire.days', '15', 'Y', 'admin',
        '数据创建时间距当前时间超过该天数则不再推送，默认15天', sysdate());

-- 如重复执行报键名重复，可先删除旧行再插入：
-- DELETE FROM sys_config WHERE config_key = 'xa.erye.kafka.push.expire.days';
