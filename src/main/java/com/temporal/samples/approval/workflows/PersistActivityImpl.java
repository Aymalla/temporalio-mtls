package com.temporal.samples.approval.workflows;

import com.temporal.samples.approval.services.PersistenceService;
import com.temporal.samples.approval.utils.ActivityContext;
import com.temporal.samples.approval.utils.PersistenceRequest;
import com.temporal.samples.approval.utils.Result;

public class PersistActivityImpl implements PersistActivity {

    PersistenceService persistenceService;

    public PersistActivityImpl(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    /**
     * Perform persistence of workflow status
     * @param activityContext
     * @return
     */
    @Override
    public Result persist(ActivityContext activityContext)
    {
        var request = new PersistenceRequest();
        request.setWorkflowInstanceId(activityContext.getWorkflowInstanceId());
        return persistenceService.persist(request);
    }
}
