# Kafka 数据推送时效控制 功能说明

> 版本：2025-10-24 新增功能
> 目标：数据创建时间距当前时间超过阈值（默认 15 天）的 Kafka 变更数据不再推送，避免历史数据回灌。

## 1. 功能链路

```
Oracle CDC(Debezium) → Kafka
  → [his-service 消费端预过滤] 超期 → ack 后丢弃（不转发）
  → 未超期 → HTTP POST /convert/receiveKafkaData
  → [convert-server 入口兜底校验] 超期 → 直接返回，不路由不推送
  → 未超期 → 按表路由到转换服务 → 推送前置软件
```

- **双端拦截**：消费端减少无效转发；convert-server 入口兜底，防止绕过消费端的直推（如 `/convert/pushOutpMr` 之外的补推口以外的路径）。
- **同一判断标准**：两端共用 `TableCreateTimeResolver`（字段映射 + 比较逻辑单一事实源），不会出现一端放行一端拦截。

## 2. 数据创建时间取值（按业务表适配）

`DBMessage.afterData`（DELETE 用 `beforeData`）中按表取候选字段，首个可解析者生效：

| 表 | 候选字段（按优先级） |
|---|---|
| comm.users | createDate |
| comm.dept_dict | 无时间字段 → 放行 |
| medrec.pat_master_index | createDate |
| outpdoct.outp_mr | visitDate |
| inpadm.pats_in_hospital | admissionDateTime → startDateTime |
| medrec.diagnosis | diagnosisDate |
| medrec.pat_visit | admissionDateTime → catalogDate |
| exam.exam_master | reqDateTime → visitDate |
| lab.lab_test_master | requestedDateTime → executeDate |
| pharmacy.drug_presc_master | prescDate → enteredDatetime |
| lqt2024.lqt_crb_zd | createTime → activityTime |

解析兼容两种格式：epoch 毫秒字符串（≥12 位纯数字）与日期字符串（`yyyy-MM-dd HH:mm:ss` / `yyyy-MM-dd` / `yyyy/MM/dd` 等）。
**无法解析时放行**（fail-open，宁可推送不漏数据），并记 warn 日志。

## 3. 阈值存储（数据库 + 缓存）

- **数据库（权威源）**：`sys_config` 表，`config_key = xa.erye.kafka.push.expire.days`，默认值 `15`，种子脚本 `sql/kafka_push_expire_config.sql`（作用于 convert_db）。
- **缓存**：Redis key `sys_config:xa.erye.kafka.push.expire.days`，由 `SysConfigServiceImpl` 维护（读：缓存优先→回源 DB→回写；写：DB+Redis 双写）。
- **代码兜底**：配置缺失/非法/Redis 不可用/convert-server 不可达时，统一回退默认 15 天，保证 Kafka 消费链路不中断。

## 4. 管理接口

| 服务 | 方法 | 路径 | 说明 |
|---|---|---|---|
| convert-server（权威） | GET | `/convert/config/pushExpireDays` | 查询当前天数 |
| convert-server（权威） | POST | `/convert/config/pushExpireDays?days=15` | 修改（写 sys_config + Redis 双写） |
| his-service（代理） | GET | `/config/pushExpireDays` | 查询（共享 Redis → Feign → 默认值） |
| his-service（代理） | POST | `/config/pushExpireDays?days=15` | 修改（Feign 代理到 convert-server，成功后删共享缓存） |

参数约束：整数 1–3650，非法返回错误提示。修改后**立即生效**，无需重启。

## 5. 运维注意事项

1. **Redis 为共享依赖**：两端读同一 key；convert-server 停机时 his-service 走默认 15 天并告警日志，不会阻断消费。
2. **新增表接入**：在 `TableCreateTimeResolver` 映射表加一行（表名小写 + 候选字段），两端自动生效。
3. **调整默认值**：改 `Constants.KAFKA_PUSH_EXPIRE_DAYS_DEFAULT`（仅影响配置缺失时的兜底）。
4. **关闭时效控制**：将阈值调到极大值（如 3650）等效于基本不过滤；代码不提供"关闭开关"，避免配置缺失时语义不清。
5. **日志定位**：超期跳过时两端均打 warn（含 schema.table、创建时间、阈值），可用 `数据超过推送时效` 检索。

## 6. 本次变更文件清单

| 文件 | 类型 |
|---|---|
| `xa-eyre-common/.../core/kafka/TableCreateTimeResolver.java` | 新增（共享解析器） |
| `xa-eyre-common/.../constant/Constants.java` | 修改（配置键 + 默认值常量） |
| `xa-eyre-convert-server/.../service/KafkaPushConfigService.java` | 新增（权威存储侧） |
| `xa-eyre-convert-server/.../controller/KafkaConfigController.java` | 新增（/convert/config 接口） |
| `xa-eyre-convert-server/.../controller/DataConvertCoiontroller.java` | 修改（入口兜底校验） |
| `xa-eyre-his-service/.../config/service/KafkaPushConfigService.java` | 新增（Redis+Feign 三级获取） |
| `xa-eyre-his-service/.../config/controller/KafkaConfigController.java` | 新增（/config 代理接口） |
| `xa-eyre-his-service/.../convertapi/ConvertFeignClient.java` | 修改（新增 2 个 Feign 方法） |
| `xa-eyre-his-service/.../medrec/service/KafkaConsumerService.java` | 修改（消费端预过滤） |
| `sql/kafka_push_expire_config.sql` | 新增（种子数据） |
