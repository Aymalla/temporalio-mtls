package com.temporal.samples.approval.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersistenceRequest {
    private String workflowInstanceId;
    Integer amount;
    private String initiatorName;
}
