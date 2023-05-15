package com.temporal.samples.approval.utils;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class NotificationRequest {
    private String workflowInstanceId;
    List<String> toList;
    String subject;
    String body;
}
