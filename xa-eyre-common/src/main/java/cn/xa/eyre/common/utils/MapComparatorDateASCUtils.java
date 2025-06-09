package cn.xa.eyre.common.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.xa.eyre.common.utils.vo.CertificateInfoVo;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * @author qiaoyu
 * @date 2023-09-12 11:17
 * @Description {list集合排序方法（根据日期）}
 */
@Component
public class MapComparatorDateASCUtils implements Comparator<CertificateInfoVo> {

    /**
     * 根据日期正序排列
     */
    @Override
    public int compare(CertificateInfoVo m1, CertificateInfoVo m2) {
//        SimpleDateFormat formate = new SimpleDateFormat("MM-dd");
//        Date date1 = null;
//        Date date2 = null;
//        String name1 = String.valueOf(m1.getEndTime());
//        String name2 = String.valueOf(m2.getEndTime());
//        try {
//            date1 = formate.parse(name1);
//            date2 = formate.parse(name2);
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
        DateTime date1 = DateUtil.parse(m1.getEndTime());
        DateTime date2 = DateUtil.parse(m2.getEndTime());
        return date1.compareTo(date2);
    }

}
