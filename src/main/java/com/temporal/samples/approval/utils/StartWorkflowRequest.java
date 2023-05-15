package com.temporal.samples.approval.utils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@Jacksonized
public class StartWorkflowRequest {
    private String workflowInstanceId;
    private String initiatorName;
    private Integer amount;
}
