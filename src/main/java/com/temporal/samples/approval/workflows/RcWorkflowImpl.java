package com.temporal.samples.approval.workflows;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.SneakyThrows;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.temporal.samples.approval.enums.ApprovalStatus;
import com.temporal.samples.approval.utils.ActivityContext;
import com.temporal.samples.approval.utils.StartWorkflowRequest;

public class RcWorkflowImpl implements RcWorkflow {

    private static final Integer ACTIVITY_DEFAULT_TIMEOUT = 10;
    private static final Integer ACTIVITY_MAX_RETRIES = 5;
    private static final Integer APPROVAL_ACTIVITY_TIMEOUT = 180;

    private ApprovalStatus companyApprovalStatus = ApprovalStatus.Waiting;
    private ApprovalStatus custodianApprovalStatus = ApprovalStatus.Waiting;

    // define default retry options that will be attached to activities
    private static final RetryOptions retryoptions = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(100))
            .setBackoffCoefficient(2)
            .setMaximumAttempts(ACTIVITY_MAX_RETRIES)
            .build();

    // define default activity options that will be attached to activities (options like: Timeout and retry).
    private static final ActivityOptions defaultActivityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(ACTIVITY_DEFAULT_TIMEOUT))
            .setRetryOptions(retryoptions)
            .build();

    // define custom activity options per activity method
    private static final Map<String, ActivityOptions> perActivityMethodOptions = new HashMap<String, ActivityOptions>() {
        {
            put("waitForApproval", ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(APPROVAL_ACTIVITY_TIMEOUT)).build());
        }
    };

    // create instance of approvalActivity
    final ApprovalActivity approvalActivity = Workflow.newActivityStub(ApprovalActivity.class, defaultActivityOptions,
            perActivityMethodOptions);

    // create instance of persistActivity
    final PersistActivity persistActivity = Workflow.newActivityStub(PersistActivity.class, defaultActivityOptions,
            perActivityMethodOptions);

    // create instance of verifyActivity
    final VerifyActivity verifyActivity = Workflow.newActivityStub(VerifyActivity.class, defaultActivityOptions,
            perActivityMethodOptions);

    // create instance of notifyActivity
    final NotifyActivity notifyActivity = Workflow.newActivityStub(NotifyActivity.class, defaultActivityOptions,
            perActivityMethodOptions);

    /**
     * Execute register-credit workflow
     * 
     * @param request
     * @return
     */
    @SneakyThrows
    @Override
    public String execute(StartWorkflowRequest request) {

        ActivityContext activityContext = new ActivityContext(request.getWorkflowInstanceId(),
                request.getInitiatorName(),
                request.getAmount());

        var sendCompanyApprovalRequest = approvalActivity.sendCompanyApprovalRequest(activityContext);
        var sendCustodianApprovalResult = approvalActivity.sendCustodianApprovalRequest(activityContext);
        if (!sendCompanyApprovalRequest.getSuccess() || !sendCustodianApprovalResult.getSuccess()) {
            return "Failure: failed to send approval requests";
        }

        var waitForApprovalResult = waitForApproval(activityContext);
        if (waitForApprovalResult == ApprovalStatus.Rejected) {
            return "Failure: register credit request is rejected";
        }

        if (waitForApprovalResult == ApprovalStatus.Timeout) {
            return "Failure: approval timeout";
        }

        var verifyResult = verifyActivity.verify(activityContext);
        if (!verifyResult.getSuccess()) {
            return "Failure: verification failed";
        }

        var persistResult = persistActivity.persist(activityContext);
        if (!persistResult.getSuccess()) {
            return "Failure: persistence failed";
        }

        var notifyResult = notifyActivity.notify(activityContext);
        if (!notifyResult.getSuccess()) {
            return "Failure: notification failed";
        }

        return "Success: Workflow completed successfully";
    }

    public ApprovalStatus waitForApproval(ActivityContext activityContext) {
        while (companyApprovalStatus == ApprovalStatus.Waiting || custodianApprovalStatus == ApprovalStatus.Waiting) {
            // wait for external signals of approval
            Workflow.await(Duration.ofSeconds(APPROVAL_ACTIVITY_TIMEOUT),
                    () -> (companyApprovalStatus != ApprovalStatus.Waiting
                            && custodianApprovalStatus != ApprovalStatus.Waiting));
            break;
        }

        if (companyApprovalStatus == ApprovalStatus.Waiting || custodianApprovalStatus == ApprovalStatus.Waiting) {
            return ApprovalStatus.Timeout;
        }

        var result = companyApprovalStatus == ApprovalStatus.Approved
                && custodianApprovalStatus == ApprovalStatus.Approved
                        ? ApprovalStatus.Approved
                        : ApprovalStatus.Rejected;

        return result;
    }

    @Override
    public void companyApprovalCallback(ApprovalStatus approvalStatus) {
        this.companyApprovalStatus = approvalStatus;
    }

    @Override
    public void custodianApprovalCallback(ApprovalStatus approvalStatus) {
        this.custodianApprovalStatus = approvalStatus;
    }

}
