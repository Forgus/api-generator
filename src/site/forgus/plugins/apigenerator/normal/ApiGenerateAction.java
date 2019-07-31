package site.forgus.plugins.apigenerator.normal;

import com.google.gson.Gson;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import demo.BuildJsonForDubbo;
import demo.YapiDubboDTO;
import site.forgus.plugins.apigenerator.normal.BuildMdForDubbo;
import site.forgus.plugins.apigenerator.normal.FieldDocVO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class ApiGenerateAction extends AnAction {

    private static NotificationGroup notificationGroup;

    static {
        notificationGroup = new NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getDataContext().getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        Project project = editor.getProject();
//        Notification notification = notificationGroup.createNotification("Hey,you should config first before generate api docs!", NotificationType.ERROR);
//        Notifications.Bus.notify(notification, project);
        try {
            dubboApiUpload(e,project);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    @Override
    public void update(AnActionEvent anActionEvent) {
        // Set the availability based on whether a project is open
        Project project = anActionEvent.getProject();
        anActionEvent.getPresentation().setEnabledAndVisible(project != null);
    }


    private void dubboApiUpload(AnActionEvent anActionEvent, Project project) throws IOException {
        List<FieldDocVO> fieldDocVOS = new BuildMdForDubbo().generateParamFieldDocVOs(anActionEvent);
        File file = new File("/Users/chenwenbin/Desktop/test.md");
        Writer writer = new FileWriter(file);
        writer.write("名称|类型|是否必须|值域范围|描述/示例\n");
        writer.write("--|--|--|--|--\n");
        for(FieldDocVO vo : fieldDocVOS) {
            String str = vo.getName() + "|" + vo.getType() + "|"+vo.getRequire()+"|" + vo.getRange() + "|" + vo.getDesc()+"\n";
            writer.write(str);
        }
        writer.close();
//        ArrayList<YapiDubboDTO> yapiDubboDTOS = new BuildJsonForDubbo().actionPerformedList(anActionEvent);
//        System.out.println(new Gson().toJson(yapiDubboDTOS));
    }
}
