package site.forgus.plugins.apigenerator.yapi.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class YApiResponse<T> implements Serializable {

    private static final long serialVersionUID = -8895912143584647957L;

    private Integer errcode;
    private String errmsg;
    private T data;

}
