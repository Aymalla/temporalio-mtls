package com.temporal.samples.approval.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalRequest {
    private String workflowInstanceId;
    private String initiatorName;
}
