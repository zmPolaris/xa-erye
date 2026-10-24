package cn.xa.eyre.common.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * XSS攻击过滤请求包装器
 * <p>
 * 对请求参数、参数数组、参数Map及请求头中的危险内容进行清洗，
 * 包括script/iframe等标签、onXXX事件属性、javascript:等脚本协议。
 *
 * @author zm
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /** 危险内容匹配规则（静态编译一次，避免重复编译开销） */
    private static final Pattern[] DANGEROUS_PATTERNS = new Pattern[]{
            // <script>...</script> 整块
            Pattern.compile("(?i)<\\s*script.*?(/>|>.*?<\\s*/\\s*script\\s*>)"),
            // 危险标签本身
            Pattern.compile("(?i)</?\\s*(script|iframe|object|embed|link|meta|svg|form|base)\\b[^>]*>"),
            // onXXX= 事件属性
            Pattern.compile("(?i)\\son\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)"),
            // 脚本协议
            Pattern.compile("(?i)\\s*(javascript|vbscript|livescript|mocha)\\s*:"),
            // expression()/eval() 动态执行
            Pattern.compile("(?i)\\s*(expression|eval)\\s*\\(")
    };

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        return cleanXss(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null || values.length == 0) {
            return values;
        }
        String[] cleaned = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleaned[i] = cleanXss(values[i]);
        }
        return cleaned;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> rawMap = super.getParameterMap();
        Map<String, String[]> cleanedMap = new HashMap<>(rawMap.size());
        for (Map.Entry<String, String[]> entry : rawMap.entrySet()) {
            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                cleanedMap.put(entry.getKey(), values);
                continue;
            }
            String[] cleaned = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleaned[i] = cleanXss(values[i]);
            }
            cleanedMap.put(entry.getKey(), cleaned);
        }
        return cleanedMap;
    }

    @Override
    public String getHeader(String name) {
        return cleanXss(super.getHeader(name));
    }

    /**
     * 清洗单个字符串中的危险内容
     */
    private String cleanXss(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String cleaned = value;
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            cleaned = pattern.matcher(cleaned).replaceAll("");
        }
        return cleaned;
    }
}
