package cn.xa.eyre.hub.staticvalue;

/**
 * 前置软件接口中，在HIS找不到对应码获无需转码的，固定值
 */
public enum HubCodeEnum {
    ID_CARD_TYPE("01", "身份证号"),
    ORG_CODE("520111021", "中国人民解放军联勤保障部队第九二五医院"),
    NATIONALITY_CODE("156", "中国"),
    NATION_CODE("97", "其他")
    ;

    private final String code;

    private final String name;

    HubCodeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
