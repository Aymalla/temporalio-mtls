package com.temporal.samples.approval.services;

import org.springframework.stereotype.Service;

import com.temporal.samples.approval.config.ApiConfig;
import com.temporal.samples.approval.utils.ApprovalRequest;
import com.temporal.samples.approval.utils.Result;

@Service
public class ApprovalServiceImpl implements ApprovalService {

    private ApiConfig apiConfig;

    public ApprovalServiceImpl(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    @Override
    public Result sendCompanyApprovalRequest(ApprovalRequest request) {
        var approveCallBack = String.format("%s/%s/Approved", apiConfig.getCompanyApprovalUrl(),
                request.getWorkflowInstanceId());
        var rejectCallBack = String.format("%s/%s/Rejected", apiConfig.getCompanyApprovalUrl(),
                request.getWorkflowInstanceId());
        var result = String.format("Company ApproveCallBack: %s , Company RejectCallBack: %s", approveCallBack,
                rejectCallBack);
        return Result.Succeeded(result);
    }

    @Override
    public Result sendCustodianApprovalRequest(ApprovalRequest request) {
        var approveCallBack = String.format("%s/%s/Approved", apiConfig.getCustodianApprovalUrl(),
                request.getWorkflowInstanceId());
        var rejectCallBack = String.format("%s/%s/Rejected", apiConfig.getCustodianApprovalUrl(),
                request.getWorkflowInstanceId());
        var result = String.format("Custodian ApproveCallBack: %s , Company RejectCallBack: %s", approveCallBack,
                rejectCallBack);
        return Result.Succeeded(result);
    }
}
