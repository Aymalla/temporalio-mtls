package com.temporal.samples.approval.services;

import com.temporal.samples.approval.utils.ApprovalRequest;
import com.temporal.samples.approval.utils.Result;

public interface ApprovalService {

    Result sendCompanyApprovalRequest(ApprovalRequest request);

    Result sendCustodianApprovalRequest(ApprovalRequest request);
}
