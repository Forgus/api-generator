# Api Generator
## 简介
《Api Generator》是一款可以自动生成接口文档的IDEA插件。包括基于Spring注解的RESTful接口和用于定义dubbo api的普通接口。其中，RESTful接口将自动上传并托管在内网部署的YApi服务器上，而普通接口则以markdown文件的形式保存在指定目录。
所以，如果你想利用该插件的能力帮你生成REST接口文档，请先确保你已部署好自己的YApi服务端。   
传送门：[如何部署YApi平台](https://hellosean1025.github.io/yapi/devops/index.html)
## 特性
- 基于javadoc解析，无代码入侵
- 支持字段过滤，自动忽略过滤字段的解析
- 自动识别类，生成markdown文档或上传到YApi
- 支持List、Set、Collection等数据结构，支持嵌套泛型解析
- 支持@NotNull、@ResponseBody等常用注解的解析，基于json5生成YApi文档
## 快速开始
### 安装插件
Preferences → Plugins → Marketplace → 搜索“Api Generator” → 安装该插件 → 重启IDE
### 开始使用
#### 上传REST接口
选择一个Controller类，将光标定位到方法区（方法名或者方法注释）或Controller类上，点击鼠标右键，在弹出的菜单项里选择“Generate Api”单击，文档瞬间已经自动生成并托管到YApi平台！   
（PS：首次使用会弹框提示输入YApi部署的url和项目token，填写一次自动保存）
#### 生成dubbo接口文档
操作方式同上，插件会自动识别出这是一个普通接口，插件会将文档以markdown的形式输出，默认保存在当前项目的target目录下。（保存路径可更改，见下文介绍）
## 插件设置
自定义配置项： Preferences —> Other Settings —> Api Generator Setting  
配置项|含义|详细解释
---|---|---
Exclude Fields|过滤字段（多个字段以","分隔）|该配置项功能类似JSONField，用于过滤不想被解析的字段，多用于排除二方包里的干扰字段
Save Directory|markdown文档保存目录（绝对路径）|用于配置生成的markdown形式的接口文档的保存路径，默认保存在当前项目的target目录
Indent Style|二级字段缩进前缀|生成的markdown文档是类似于json schema的字段表格，涉及类型是对象的字段，展示上做缩进处理，默认缩进前缀是“└”
Overwrite exists docs|是否覆盖同名markdown文档|如果生成的markdown文件已存在，会弹框提示是否覆盖，勾选该选项，则直接覆盖不提示
Extract filename from doc comments|是否从javadoc抽取文件名|生成的markdown文件默认是方法名，勾选该选项，将从注释里抽取文件名
YApi server url|YApi部署服务器地址|内网部署的yapi平台的域名，如：http://yapi.xxx.com
Project token|项目token|接口对应的yapi项目的token
Default save category|默认保存分类|插件生成的yapi文档保存位置，默认api_generator
Classify API automatically|是否自动分类|勾选该选项后，生成文档时插件将从controller类注释里抽取模块名，并在yapi上自动创建对应分类保存接口
## 详细文档
更多详细介绍请移步wiki   
PS：如果觉得好用，请帮我点个赞~
