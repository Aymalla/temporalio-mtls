package com.temporal.samples.approval.services;

import com.temporal.samples.approval.utils.Result;
import com.temporal.samples.approval.utils.VerificationRequest;

public interface VerificationService {

    Result verify(VerificationRequest request);
}
