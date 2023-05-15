package com.temporal.samples.approval.services;

import com.temporal.samples.approval.utils.PersistenceRequest;
import com.temporal.samples.approval.utils.Result;

public interface PersistenceService {

    Result persist(PersistenceRequest request);
}
