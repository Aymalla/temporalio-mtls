package com.temporal.samples.helloworld.workflows;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class GreetingWorkflowImpl implements GreetingWorkflow {

    private static final Integer ACTIVITY_DEFAULT_TIMEOUT = 10;
    private static final Integer ACTIVITY_MAX_RETRIES = 5;

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

    // create instance of HelloActivity
    final HelloActivity helloActivity = Workflow.newActivityStub(HelloActivity.class,defaultActivityOptions);

    /**
     * Execute register-credit workflow
     * 
     * @param request
     * @return
     */
    @Override
    public String greet(String instanceId, String name) {
        var result = helloActivity.hello(name);
        if (result != null) {
            System.out.println("HelloActivity.hello() returned: " + result);
        }
        
        return "Success: Workflow completed successfully";
    }
}
