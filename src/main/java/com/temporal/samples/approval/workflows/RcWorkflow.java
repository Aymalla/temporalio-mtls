package com.temporal.samples.approval.workflows;

import com.temporal.samples.approval.enums.ApprovalStatus;
import com.temporal.samples.approval.utils.StartWorkflowRequest;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface RcWorkflow {

    // The Workflow method is called by the initiator either via code or CLI.
    @WorkflowMethod
    String execute(StartWorkflowRequest request);

    @SignalMethod
    void companyApprovalCallback(ApprovalStatus approvalStatus);

    @SignalMethod
    void custodianApprovalCallback(ApprovalStatus approvalStatus);
}