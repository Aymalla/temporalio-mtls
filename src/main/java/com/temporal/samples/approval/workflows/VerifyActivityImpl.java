package com.temporal.samples.approval.workflows;

import com.temporal.samples.approval.services.VerificationService;
import com.temporal.samples.approval.utils.ActivityContext;
import com.temporal.samples.approval.utils.Result;
import com.temporal.samples.approval.utils.VerificationRequest;

public class VerifyActivityImpl implements VerifyActivity {

    VerificationService verificationService;

    public VerifyActivityImpl(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * Verify registration request
     * @param activityContext
     * @return
     */
    @Override
    public Result verify(ActivityContext activityContext) {

        var request = new VerificationRequest();
        request.setWorkflowInstanceId(activityContext.getWorkflowInstanceId());
        return verificationService.verify(request);
    }
}
