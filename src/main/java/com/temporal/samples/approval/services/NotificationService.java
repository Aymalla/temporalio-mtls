package com.temporal.samples.approval.services;

import com.temporal.samples.approval.utils.NotificationRequest;
import com.temporal.samples.approval.utils.Result;

public interface NotificationService {

    Result notify(NotificationRequest request);
}
