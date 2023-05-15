package com.temporal.samples.approval.workflows;

import com.temporal.samples.approval.services.ApprovalService;
import com.temporal.samples.approval.utils.ActivityContext;
import com.temporal.samples.approval.utils.ApprovalRequest;
import com.temporal.samples.approval.utils.Result;

public class ApprovalActivityImpl implements ApprovalActivity {

    ApprovalService approvalService;

    public ApprovalActivityImpl(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    /**
     * send company approval request
     * @param activityContext
     * @return
     */
    @Override
    public Result sendCompanyApprovalRequest(ActivityContext activityContext) {
        var request = new ApprovalRequest();
        request.setWorkflowInstanceId(activityContext.getWorkflowInstanceId());
        return approvalService.sendCompanyApprovalRequest(request);
    }

    /**
     * send custodian approval request
     * @param activityContext
     * @return
     */
    @Override
    public Result sendCustodianApprovalRequest(ActivityContext activityContext) {
        var request = new ApprovalRequest();
        request.setWorkflowInstanceId(activityContext.getWorkflowInstanceId());
        return approvalService.sendCustodianApprovalRequest(request);
    }
}
