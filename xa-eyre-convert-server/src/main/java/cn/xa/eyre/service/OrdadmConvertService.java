package cn.xa.eyre.service;

import cn.hutool.core.bean.BeanUtil;
import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.kafka.DBMessage;
import cn.xa.eyre.common.utils.DateUtils;
import cn.xa.eyre.hub.domain.emrmonitor.EmrOrder;
import cn.xa.eyre.ordadm.domain.Orders;
import cn.xa.eyre.outpdoct.domain.OutpMr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrdadmConvertService {
    Logger logger = LoggerFactory.getLogger(OrdadmConvertService.class);


    public void ordersInfo(DBMessage dbMessage) {
        logger.debug("医嘱表ORDERS变更接口");
        logger.debug("ORDERS变更需调用emrOrder,同步接口");

        String httpMethod = null;
        Orders orders;
        Map<String, String> data;
        if(dbMessage.getOperation().equalsIgnoreCase("DELETE")){
            httpMethod = Constants.HTTP_METHOD_DELETE;
            data = dbMessage.getBeforeData();
        }else {
            httpMethod = Constants.HTTP_METHOD_POST;
            data = dbMessage.getAfterData();
        }
        orders = BeanUtil.toBeanIgnoreError(data, Orders.class);
//        orders.setVisitDate(DateUtils.getLongDate(dbMessage.getAfterData().get("visitDate")));


    }
}
