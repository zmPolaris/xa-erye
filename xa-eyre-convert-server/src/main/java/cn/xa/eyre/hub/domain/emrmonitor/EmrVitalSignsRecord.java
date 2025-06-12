package cn.xa.eyre.hub.domain.emrmonitor;

import java.util.Date;

import cn.xa.eyre.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 生命体征记录对象 emr_vital_signs_record
 *
 * @author ruoyi
 * @date 2025-06-11
 */
public class EmrVitalSignsRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 死亡信息在院内的唯一识别标识 */
    private String id;

    /** 机构内患者基本信息唯一标识 */
    private String patientId;

    /** 关联的就诊活动类别代码 */
    private String activityTypeCode;

    /** 关联的就诊活动类别名称 */
    private String activityTypeName;

    /** 诊疗活动发生在门（急）诊期间时，就诊流水号为门（急）诊号；诊疗活动为住院期间时，就诊流水号为住院号 */
    private String serialNumber;

    /** 患者本人在公安户籍管理部门正式登记注册的姓氏和名称 */
    private String patientName;

    /** 患者身份证件所属类别代码 */
    private String idCardTypeCode;

    /** 患者身份证件所属类别名称 */
    private String idCardTypeName;

    /** 身份证件号码 */
    private String idCard;

    /** 记录创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createDate;

    /** 患者是否同意使用呼吸机辅助通气代码 */
    private String ventilatorusedCode;

    /** 患者是否同意使用呼吸机辅助通气名称 */
    private String ventilatorusedName;

    /** 标识患者当前是否属于重症监护中代码 */
    private String criticalCareCode;

    /** 标识患者当前是否属于重症监护中名称 */
    private String criticalCareName;

    /** 医疗机构代码 */
    private String orgCode;

    /** 医疗机构名称 */
    private String orgName;

    /** 科室代码 */
    private String deptCode;

    /** 科室名称 */
    private String deptName;

    /** 操作人ID */
    private String operatorId;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date operationTime;

    /** 作废标志 */
    private String invalidFlag;

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }
    public void setPatientId(String patientId)
    {
        this.patientId = patientId;
    }

    public String getPatientId()
    {
        return patientId;
    }
    public void setActivityTypeCode(String activityTypeCode)
    {
        this.activityTypeCode = activityTypeCode;
    }

    public String getActivityTypeCode()
    {
        return activityTypeCode;
    }
    public void setActivityTypeName(String activityTypeName)
    {
        this.activityTypeName = activityTypeName;
    }

    public String getActivityTypeName()
    {
        return activityTypeName;
    }
    public void setSerialNumber(String serialNumber)
    {
        this.serialNumber = serialNumber;
    }

    public String getSerialNumber()
    {
        return serialNumber;
    }
    public void setPatientName(String patientName)
    {
        this.patientName = patientName;
    }

    public String getPatientName()
    {
        return patientName;
    }
    public void setIdCardTypeCode(String idCardTypeCode)
    {
        this.idCardTypeCode = idCardTypeCode;
    }

    public String getIdCardTypeCode()
    {
        return idCardTypeCode;
    }
    public void setIdCardTypeName(String idCardTypeName)
    {
        this.idCardTypeName = idCardTypeName;
    }

    public String getIdCardTypeName()
    {
        return idCardTypeName;
    }
    public void setIdCard(String idCard)
    {
        this.idCard = idCard;
    }

    public String getIdCard()
    {
        return idCard;
    }
    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    public Date getCreateDate()
    {
        return createDate;
    }
    public void setVentilatorusedCode(String ventilatorusedCode)
    {
        this.ventilatorusedCode = ventilatorusedCode;
    }

    public String getVentilatorusedCode()
    {
        return ventilatorusedCode;
    }
    public void setVentilatorusedName(String ventilatorusedName)
    {
        this.ventilatorusedName = ventilatorusedName;
    }

    public String getVentilatorusedName()
    {
        return ventilatorusedName;
    }
    public void setCriticalCareCode(String criticalCareCode)
    {
        this.criticalCareCode = criticalCareCode;
    }

    public String getCriticalCareCode()
    {
        return criticalCareCode;
    }
    public void setCriticalCareName(String criticalCareName)
    {
        this.criticalCareName = criticalCareName;
    }

    public String getCriticalCareName()
    {
        return criticalCareName;
    }
    public void setOrgCode(String orgCode)
    {
        this.orgCode = orgCode;
    }

    public String getOrgCode()
    {
        return orgCode;
    }
    public void setOrgName(String orgName)
    {
        this.orgName = orgName;
    }

    public String getOrgName()
    {
        return orgName;
    }
    public void setDeptCode(String deptCode)
    {
        this.deptCode = deptCode;
    }

    public String getDeptCode()
    {
        return deptCode;
    }
    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getDeptName()
    {
        return deptName;
    }
    public void setOperatorId(String operatorId)
    {
        this.operatorId = operatorId;
    }

    public String getOperatorId()
    {
        return operatorId;
    }
    public void setOperationTime(Date operationTime)
    {
        this.operationTime = operationTime;
    }

    public Date getOperationTime()
    {
        return operationTime;
    }
    public void setInvalidFlag(String invalidFlag)
    {
        this.invalidFlag = invalidFlag;
    }

    public String getInvalidFlag()
    {
        return invalidFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("patientId", getPatientId())
                .append("activityTypeCode", getActivityTypeCode())
                .append("activityTypeName", getActivityTypeName())
                .append("serialNumber", getSerialNumber())
                .append("patientName", getPatientName())
                .append("idCardTypeCode", getIdCardTypeCode())
                .append("idCardTypeName", getIdCardTypeName())
                .append("idCard", getIdCard())
                .append("createDate", getCreateDate())
                .append("ventilatorusedCode", getVentilatorusedCode())
                .append("ventilatorusedName", getVentilatorusedName())
                .append("criticalCareCode", getCriticalCareCode())
                .append("criticalCareName", getCriticalCareName())
                .append("orgCode", getOrgCode())
                .append("orgName", getOrgName())
                .append("deptCode", getDeptCode())
                .append("deptName", getDeptName())
                .append("operatorId", getOperatorId())
                .append("operationTime", getOperationTime())
                .append("invalidFlag", getInvalidFlag())
                .toString();
    }
}
