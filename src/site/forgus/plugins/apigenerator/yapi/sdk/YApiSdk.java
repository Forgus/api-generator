package site.forgus.plugins.apigenerator.yapi.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import site.forgus.plugins.apigenerator.yapi.model.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YApiSdk {

    private static final String PROJECT_INFO_URI = "/api/project/get";
    private static final String LIST_CATEGORY_URI = "/api/interface/getCatMenu";
    private static final String ADD_CATEGORY_URI = "/api/interface/add_cat";
    private static final String SAVE_INTERFACE_URI = "/api/interface/save";

    private static final String TOKEN_ERROR_MSG = "Project token is not right!";

    private static final String URL_ERROR_MSG = "YApi server url is Unreachable!";

    private static Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new DateSerializer()).setDateFormat(DateFormat.LONG);
        builder.registerTypeAdapter(Date.class, new DateDeserializer()).setDateFormat(DateFormat.LONG);
        gson = builder.create();
    }

    /**
     * 获取项目信息
     *
     * @param token
     * @return
     * @throws IOException
     */
    public static YApiProject getProjectInfo(String serverUrl, String token) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        String responseStr =  doGet(serverUrl + PROJECT_INFO_URI,params);
        Type type = new TypeToken<YApiResponse<YApiProject>>() {
        }.getType();
        YApiResponse<YApiProject> yApiResponse = gson.fromJson(responseStr, type);
        YApiProject yApiProject = yApiResponse.getData();
        if(yApiProject == null) {
            throw new ConfigException(TOKEN_ERROR_MSG);
        }
        return yApiProject;
    }

    /**
     * 获取分类列表
     *
     * @param token
     * @return
     * @throws IOException
     */
    public static List<YApiCat> listCategories(String serverUrl, String token) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        String responseStr = doGet(serverUrl + LIST_CATEGORY_URI, params);
        Type type = new TypeToken<YApiResponse<List<YApiCat>>>() {
        }.getType();
        YApiResponse<List<YApiCat>> yApiResponse = gson.fromJson(responseStr, type);
        List<YApiCat> yApiCats = yApiResponse.getData();
        if(yApiCats == null) {
            throw new ConfigException(TOKEN_ERROR_MSG);
        }
        return yApiCats;
    }

    /**
     * 添加分类
     *
     * @param token
     * @param projectId
     * @param name      分类名称
     * @return
     * @throws IOException
     */
    public static YApiCat addCategory(String serverUrl, String token, String projectId, String name) throws Exception {
        return addCategory(serverUrl, token, projectId, name, "");
    }

    /**
     * 保存接口（新增或更新）
     *
     * @param yApiInterface
     * @return
     * @throws IOException
     */
    public static void saveInterface(String serverUrl, YApiInterface yApiInterface) throws Exception {
        String string = doPost(serverUrl + SAVE_INTERFACE_URI, gson.toJson(yApiInterface));
        YApiResponse yApiResponse = gson.fromJson(string, YApiResponse.class);
        if (yApiResponse.getErrcode() != 0) {
            throw new ConfigException(TOKEN_ERROR_MSG);
        }
    }

    private static YApiCat addCategory(String serverUrl, String token, String projectId, String name, String desc) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("desc", desc);
        params.put("name", name);
        params.put("project_id", projectId);
        params.put("token", token);
        String string = doPost(serverUrl + ADD_CATEGORY_URI, gson.toJson(params));
        Type type = new TypeToken<YApiResponse<YApiCat>>() {
        }.getType();
        YApiResponse<YApiCat> yApiResponse = gson.fromJson(string, type);
        if(yApiResponse.getData() == null) {
            throw new ConfigException("Project token or Project id is not right!");
        }
        return yApiResponse.getData();
    }

    private static String doPost(String url, String body) throws IOException,ConfigException {
        return doHttpRequest(buildPostRequestWithJsonType(url, body));
    }

    private static HttpPost buildPostRequestWithJsonType(String url, String body) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-type", "application/json;charset=utf-8");
        httpPost.setEntity(new StringEntity(body == null ? "" : body, StandardCharsets.UTF_8));
        httpPost.setConfig(RequestConfig.custom().setConnectTimeout(3000).build());
        return httpPost;
    }

    private static String doGet(String url, Map<String, String> params) throws IOException,ConfigException {
        return doHttpRequest(buildGetRequest(url, params));
    }

    private static String doHttpRequest(HttpUriRequest httpUriRequest) throws IOException,ConfigException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try(CloseableHttpResponse response = httpclient.execute(httpUriRequest)) {
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new ConfigException(URL_ERROR_MSG);
            }
            return getStreamAsString(response.getEntity().getContent());
        }
    }

    private static HttpGet buildGetRequest(String url, Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() != 0) {
                sb.append("&");
            }
            sb.append(entry.getKey());
            sb.append("=");
            try {
                sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        HttpGet httpGet = new HttpGet(url + "?" + sb.toString());
        httpGet.setConfig(RequestConfig.custom().setConnectTimeout(3000).build());
        return httpGet;
    }

    public static String getStreamAsString(InputStream stream) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
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
