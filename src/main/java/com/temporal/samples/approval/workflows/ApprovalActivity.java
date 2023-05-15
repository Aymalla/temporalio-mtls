package com.temporal.samples.approval.workflows;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.net.URISyntaxException;

import com.temporal.samples.approval.utils.ActivityContext;
import com.temporal.samples.approval.utils.Result;

@ActivityInterface
public interface ApprovalActivity {

    @ActivityMethod
    Result sendCompanyApprovalRequest(ActivityContext activityContext) throws URISyntaxException;

    @ActivityMethod
    Result sendCustodianApprovalRequest(ActivityContext activityContext);
}
