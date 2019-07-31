package demo;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

/**
 * Description: log util
 * Copyright (c) Department of Research and Development/Beijing
 * All Rights Reserved.
 * @version 1.0 2019年06月19日 18:07
 */
public class NotifyUtil {

    /**
     * Log.
     *
     * @param notificationGroup the notification group
     * @param project           the project
     * @param message           the s
     * @param notificationType  the notification type
     */
    public static void log(NotificationGroup notificationGroup, Project project, String message, NotificationType notificationType) {
        Notification notification = notificationGroup.createNotification(message, notificationType);
        Notifications.Bus.notify(notification, project);
    }

}
