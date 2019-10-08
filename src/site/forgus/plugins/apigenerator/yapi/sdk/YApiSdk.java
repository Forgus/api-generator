package site.forgus.plugins.apigenerator.yapi.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import demo.HttpClientUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import site.forgus.plugins.apigenerator.yapi.ProjectConfig;
import site.forgus.plugins.apigenerator.yapi.model.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static site.forgus.plugins.apigenerator.util.HttpUtil.buildGetRequest;
import static site.forgus.plugins.apigenerator.util.HttpUtil.buildPostRequestWithJsonType;

public class YApiSdk {

    private static final String PROJECT_INFO_URI = "/api/project/get";
    private static final String LIST_CATEGORY_URI = "/api/interface/getCatMenu";
    private static final String ADD_CATEGORY_URI = "/api/interface/add_cat";
    private static final String SAVE_INTERFACE_URI = "/api/interface/save";

    private static Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new DateSerializer()).setDateFormat(DateFormat.LONG);
        builder.registerTypeAdapter(Date.class, new DateDeserializer()).setDateFormat(DateFormat.LONG);
        gson = builder.create();
    }

    /**
     * 获取项目信息
     * @param token
     * @return
     * @throws IOException
     */
    public static YApiProject getProjectInfo(String token) throws IOException {
        Map<String,String> params = new HashMap<>();
        params.put("token",token);
        String responseStr = HttpClientUtil.ObjectToString(
                HttpClientUtil.getHttpclient().execute(
                        buildGetRequest(ProjectConfig.getInstance().getyApiUrl() + PROJECT_INFO_URI,params)
                ), "utf-8"
        );
        Type type = new TypeToken<YApiResponse<YApiProject>>(){}.getType();
        YApiResponse<YApiProject> yApiResponse = gson.fromJson(responseStr, type);
        return yApiResponse.getData();
    }

    /**
     * 获取分类列表
     * @param token
     * @return
     * @throws IOException
     */
    public static List<YApiCat> listCategories(String token) throws IOException {
        Map<String,String> params = new HashMap<>();
        params.put("token",token);
        String responseStr = HttpClientUtil.ObjectToString(
                HttpClientUtil.getHttpclient().execute(
                        buildGetRequest(ProjectConfig.getInstance().getyApiUrl() + LIST_CATEGORY_URI,params)
                ), "utf-8"
        );
        Type type = new TypeToken<YApiResponse<List<YApiCat>>>(){}.getType();
        YApiResponse<List<YApiCat>> yApiResponse = gson.fromJson(responseStr,type);
        return yApiResponse.getData();
    }

    /**
     * 添加分类
     * @param token
     * @param projectId
     * @param name 分类名称
     * @return
     * @throws IOException
     */
    public static YApiResponse<YApiCat> addCategory(String token,String projectId,String name) throws IOException {
        return addCategory(token,projectId,name,"");
    }

    /**
     * 保存接口（新增或更新）
     * @param yApiInterface
     * @return
     * @throws IOException
     */
    public static YApiResponse saveInterface(YApiInterface yApiInterface) throws IOException {
        CloseableHttpResponse httpResponse = HttpClientUtil.getHttpclient().execute(
                buildPostRequestWithJsonType(ProjectConfig.getInstance().getyApiUrl() + SAVE_INTERFACE_URI, gson.toJson(yApiInterface))
        );
        String string = HttpClientUtil.ObjectToString(httpResponse,"utf-8");
        return gson.fromJson(string,YApiResponse.class);
    }

    public static YApiResponse<YApiCat> addCategory(String token,String projectId,String name,String desc) throws IOException {
        Map<String,String> params = new HashMap<>();
        params.put("desc",desc);
        params.put("name",name);
        params.put("project_id",projectId);
        params.put("token",token);
        CloseableHttpResponse httpResponse = HttpClientUtil.getHttpclient().execute(
                buildPostRequestWithJsonType(ProjectConfig.getInstance().getyApiUrl() + ADD_CATEGORY_URI, gson.toJson(params))
        );
        String string = HttpClientUtil.ObjectToString(httpResponse,"utf-8");
        Type type = new TypeToken<YApiResponse<YApiCat>>(){}.getType();
        return gson.fromJson(string,type);
    }

}
