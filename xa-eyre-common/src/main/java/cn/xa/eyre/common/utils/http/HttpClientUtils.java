package cn.xa.eyre.common.utils.http;

import cn.xa.eyre.common.constant.Constants;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * 通用http发送方法
 *
 * @author ruoyi
 */
public class HttpClientUtils
{
    /** 连接超时（毫秒） */
    private static final int CONNECT_TIMEOUT = 5000;
    /** 读取超时（毫秒） */
    private static final int SOCKET_TIMEOUT = 30000;
    /** 连接池最大连接数 */
    private static final int MAX_CONN_TOTAL = 100;
    /** 单路由最大连接数 */
    private static final int MAX_CONN_PER_ROUTE = 20;

    /** 全类共享的连接池与客户端（原每次请求createDefault会重复创建连接池且无超时，对端挂起将无限阻塞） */
    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();
    private static final CloseableHttpClient HTTP_CLIENT;

    static {
        CONNECTION_MANAGER.setMaxTotal(MAX_CONN_TOTAL);
        CONNECTION_MANAGER.setDefaultMaxPerRoute(MAX_CONN_PER_ROUTE);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .build();
        HTTP_CLIENT = HttpClients.custom()
                .setConnectionManager(CONNECTION_MANAGER)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(60, TimeUnit.SECONDS)
                .build();
    }

    public static String commDataRequest(String url,String data)throws ClientProtocolException,ParseException,IOException{
        HttpPost httpPost = new HttpPost(url);
        HttpEntity entity = new StringEntity(data,Consts.UTF_8);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(entity);
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity(),Charset.forName("UTF-8"));
        }
    }

    /**
     * 向指定地址发送post请求
     * @param url http请求地址
     * @param param 请求参数
     * @return
     * @throws Exception
     */
    public static String sendHttpClientPost(String url, String param)throws ClientProtocolException, ParseException,IOException
    {
        HttpPost httpPost = new HttpPost(url);
        HttpEntity entity = new StringEntity(param, Consts.UTF_8);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(entity);
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
        }
    }

    /**
     * 向指定地址发送delete请求
     * @param url http请求地址
     * @param param 请求参数
     * @return
     * @throws Exception
     */
    public static String sendHttpClientDelete(String url, String param)throws ParseException,IOException
    {
        HttpDeleteWithBody delete = new HttpDeleteWithBody(url);
        delete.setEntity(new StringEntity(param, "application/json", "UTF-8"));
        // 添加请求头
        delete.setHeader("Content-Type", "application/json");
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(delete)) {
            return EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
        }
    }

    public static String sendHttpRequest(String url, String param, String method) throws IOException {
        if (method.equals(Constants.HTTP_METHOD_POST)){
            return sendHttpClientPost(url, param);
        }else {
            return sendHttpClientDelete(url, param);
        }
    }

}