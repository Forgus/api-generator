package site.forgus.plugins.apigenerator.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.URLEncoder;
import java.util.Map;

public class HttpUtil {

    private static final String CHARSET = "UTF-8";

    public static String doPost(String url, String body) throws IOException {
        return doHttpRequest(buildPostRequestWithJsonType(url, body));
    }

    private static HttpPost buildPostRequestWithJsonType(String url, String body) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-type", "application/json;charset=utf-8");
        httpPost.setEntity(new StringEntity(body == null ? "" : body, CHARSET));
        httpPost.setConfig(RequestConfig.custom().setConnectTimeout(3000).build());
        return httpPost;
    }

    private static String doHttpRequest(HttpUriRequest httpUriRequest) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try(CloseableHttpResponse response = httpclient.execute(httpUriRequest)) {
            return getStreamAsString(response.getEntity().getContent());
        }
    }

    private static String getStreamAsString(InputStream stream) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, CHARSET));
            StringWriter writer = new StringWriter();

            char[] chars = new char[256];
            int count = 0;
            while ((count = reader.read(chars)) > 0) {
                writer.write(chars, 0, count);
            }

            return writer.toString();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

}
