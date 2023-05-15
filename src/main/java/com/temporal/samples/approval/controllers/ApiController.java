package com.temporal.samples.approval.controllers;

import org.springframework.web.bind.annotation.*;

import com.temporal.samples.approval.utils.NotificationRequest;
import com.temporal.samples.approval.utils.PersistenceRequest;
import com.temporal.samples.approval.utils.Result;
import com.temporal.samples.approval.utils.VerificationRequest;

/**
 * This controller simulate an external Apis to be called from the workflow activities
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    /**
     * Simulate notification endpoint
     * 
     * @param request
     * @return
     */
    @PostMapping
    @RequestMapping("/notify")
    public Result notify(@RequestBody NotificationRequest request) {
        return Result.Succeeded("notified successfully:" + request.getWorkflowInstanceId());
    }

    /**
     * Simulate verification endpoint
     * 
     * @param request
     * @return
     */
    @PostMapping
    @RequestMapping("/verify")
    public Result verify(@RequestBody VerificationRequest request) {
        return Result.Succeeded("verified successfully:" + request.getWorkflowInstanceId());
    }

    /**
     * Simulate persistence endpoint
     * 
     * @param request
     * @return
     */
    @PostMapping
    @RequestMapping("/persist")
    public Result persist(@RequestBody PersistenceRequest request) {
        return Result.Succeeded("persisted successfully:" + request.getWorkflowInstanceId());
    }
}
