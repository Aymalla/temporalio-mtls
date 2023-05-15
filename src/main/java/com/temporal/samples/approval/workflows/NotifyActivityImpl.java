package com.temporal.samples.approval.workflows;

import java.util.Arrays;
import java.util.List;

import com.temporal.samples.approval.services.NotificationService;
import com.temporal.samples.approval.utils.ActivityContext;
import com.temporal.samples.approval.utils.NotificationRequest;
import com.temporal.samples.approval.utils.Result;

public class NotifyActivityImpl implements NotifyActivity {

    NotificationService notificationService;

    public NotifyActivityImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Notify the participants with workflow status
     * @param activityContext
     * @return
     */
    @Override
    public Result notify(ActivityContext activityContext) {

        NotificationRequest request = new NotificationRequest();
        request.setWorkflowInstanceId(activityContext.getWorkflowInstanceId());
        List<String> toList = Arrays.asList("company@email.com","customer@email.com");
        request.setToList(toList);
        request.setSubject("Subject");
        request.setBody("Body");
        Result result = notificationService.notify(request);
        return result;
    }
}
