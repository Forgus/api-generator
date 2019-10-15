package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class YApiEnv implements Serializable {

    private static final long serialVersionUID = -2972535876457197291L;

    private String _id;
    private String name;
    private String domain;
    private List<YApiHeader> header;
    private List<YApiGlobal> global;

}
