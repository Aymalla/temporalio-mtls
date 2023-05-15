package com.temporal.samples.approval.utils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@Jacksonized
public class ActivityContext {
    private String workflowInstanceId;

    private String initiatorName;

    private Integer amount;

    public ActivityContext(){

    }
    public ActivityContext(String workflowInstanceId, String initiatorName,Integer amount)
    {
        this.workflowInstanceId = workflowInstanceId;
        this.initiatorName = initiatorName;
        this.amount = amount;
    }
}
