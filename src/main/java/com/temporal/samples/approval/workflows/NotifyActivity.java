package com.temporal.samples.approval.workflows;

import com.temporal.samples.approval.utils.ActivityContext;
import com.temporal.samples.approval.utils.Result;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface NotifyActivity {
    @ActivityMethod
    Result notify(ActivityContext activityContext);

}
