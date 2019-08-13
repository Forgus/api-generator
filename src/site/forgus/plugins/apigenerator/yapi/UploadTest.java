package site.forgus.plugins.apigenerator.yapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import demo.HttpClientUtil;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import site.forgus.plugins.apigenerator.yapi.model.*;
import site.forgus.plugins.apigenerator.yapi.sdk.YApiSdk;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UploadTest {

    private Gson gson;

    private static final String YAPI_URL = ProjectConfig.getInstance().getyApiUrl();
    private static final String TOKEN = ProjectConfig.getInstance().getToken();

    @Before
    public void init() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new DateDeserializer()).setDateFormat(DateFormat.LONG);
        builder.registerTypeAdapter(Date.class, new DateSerializer()).setDateFormat(DateFormat.LONG);
        gson = builder.create();
    }

    @Test
    public void testAddCat() throws Exception {
        YApiResponse yApiResponse = YApiSdk.addCategory(ProjectConfig.getInstance().getToken(), "241", "测试");
        System.out.println(gson.toJson(yApiResponse));
    }

    @Test
    public void testGetProjectInfo() throws IOException {
        YApiProject yApiProject = YApiSdk.getProjectInfo(ProjectConfig.getInstance().getToken());
        System.out.println(gson.toJson(yApiProject));
    }

    @Test
    public void testGetCatMenu() throws IOException {
        List<YApiCat> yApiCats = YApiSdk.listCategories(TOKEN);
        System.out.println(gson.toJson(yApiCats));
    }

    @Test
    public void testSaveInterface() throws Exception {

    }

    private String getPrettyJson() {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        JsonObject object = new JsonObject();
        object.addProperty("id", 1);
        object.addProperty("taxNo", "xxx");
        jsonArray.add(object);
        jsonObject.add("data", jsonArray);
        jsonObject.addProperty("success", true);
        return new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject);
    }

    @Test
    public void testAddComment() {

    }


    private HttpPost getHttpPostWithJsonType(String url, String body) {
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

    public String doPostWithFormType(String url, Map<String, String> map) throws Exception {
        // 声明httpPost请求
        HttpPost httpPost = new HttpPost(url);

        // 判断map不为空
        if (map != null) {
            // 声明存放参数的List集合
            List<NameValuePair> params = new ArrayList<>();

            // 遍历map，设置参数到list中
            for (Map.Entry<String, String> entry : map.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            // 创建form表单对象
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params, "utf-8");
            formEntity.setContentType("Content-Type:application/x-www-form-urlencoded");

            // 把表单对象设置到httpPost中
            httpPost.setEntity(formEntity);
        }

        // 使用HttpClient发起请求，返回response
        CloseableHttpResponse response = HttpClientUtil.getHttpclient().execute(httpPost);

        // 解析response封装返回对象httpResult
        String httpResult;
        if (response.getEntity() != null) {
            httpResult = EntityUtils.toString(response.getEntity(), "UTF-8");
        } else {
            httpResult = gson.toJson(response.getStatusLine());
        }
        // 返回结果
        return httpResult;
    }


}
