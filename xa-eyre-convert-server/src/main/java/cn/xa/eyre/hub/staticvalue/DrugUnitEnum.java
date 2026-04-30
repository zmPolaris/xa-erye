package cn.xa.eyre.hub.staticvalue;

import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * @ClassName DrugUnitEnum
 * @Description
 * @Author Administrator
 * @Date 2026/4/29 20:48
 * @Version 1.0.0
 */
public enum DrugUnitEnum {
    DRUG_UNIT_G("01","克（g）","g"),
    DRUG_UNIT_MG("02","毫克（mg）","mg"),
    DRUG_UNIT_UG("03","微克 (μg）","μg"),
    DRUG_UNIT_NG("04","纳克（ng）","ng"),
    DRUG_UNIT_L("05","升（L）","L"),
    DRUG_UNIT_ML("06","毫升（ml）","ml"),
    DRUG_UNIT_IU("07","国际单位（IU）","iu"),
    DRUG_UNIT_U("08","单位（U）","u"),
    DRUG_UNIT_OTH("99","其他",""),
    ;

    /* 值 */
    private final String code;

    /* 对比值 */
    private final String name;

    /* 值含义 */
    private final String value;

    DrugUnitEnum(String code, String value, String name) {
        this.code = code;
        this.name = name;
        this.value = value;
    }

    private static final Map<String, DrugUnitEnum> NAME_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(DrugUnitEnum::getName, drug -> drug));

    /**
     * 通过 name 属性获取枚举对象（精确匹配）
     */
    public static DrugUnitEnum fromName(String name) {
        return NAME_MAP.get(name);
    }

    /**
     * 通过 name 属性获取枚举对象（忽略大小写）
     */
    public static DrugUnitEnum fromNameIgnoreCase(String name) {
        if (name == null) return null;
        return NAME_MAP.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(DRUG_UNIT_OTH);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DrugUnitEnum.class.getSimpleName() + "[", "]")
                .add("code='" + code + "'")
                .add("name='" + name + "'")
                .add("value='" + value + "'")
                .toString();
    }
}
