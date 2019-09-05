package site.forgus.plugins.apigenerator.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.util.Map;

public class HttpUtil {

    public static HttpGet buildGetRequest(String url, Map<String,String> params) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry entry : params.entrySet()) {
            if(sb.length() != 0) {
                sb.append("&");
            }
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
        }
        return new HttpGet(url + "?" + sb.toString());
    }

    public static HttpPost buildPostRequestWithJsonType(String url, String body) {
        HttpPost httpPost = null;
        try {
            httpPost = new HttpPost(url);
            httpPost.setHeader("Content-type", "application/json;charset=utf-8");
            HttpEntity reqEntity = new StringEntity(body == null ? "" : body, "UTF-8");
            httpPost.setEntity(reqEntity);
        } catch (Exception e) {
        }
        return httpPost;
    }

}
