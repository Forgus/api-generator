import com.google.gson.Gson;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import site.forgus.plugins.apigenerator.normal.BuildMdForDubbo;
import site.forgus.plugins.apigenerator.normal.FieldDocVO;

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
        dubboApiUpload(e,project);
    }
    @Override
    public void update(AnActionEvent anActionEvent) {
        // Set the availability based on whether a project is open
        Project project = anActionEvent.getProject();
        anActionEvent.getPresentation().setEnabledAndVisible(project != null);
    }


    private void dubboApiUpload(AnActionEvent anActionEvent, Project project) {
        // 获得dubbo需上传的接口列表 参数对象
        List<FieldDocVO> fieldDocVOS = new BuildMdForDubbo().generateParamFieldDocVOs(anActionEvent);
        String jsonDoc = new Gson().toJson(fieldDocVOS);
        System.out.println(jsonDoc);
    }
}
