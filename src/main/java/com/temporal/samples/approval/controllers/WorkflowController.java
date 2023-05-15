package com.temporal.samples.approval.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temporal.samples.approval.Client;
import com.temporal.samples.approval.enums.ApprovalStatus;
import com.temporal.samples.approval.utils.StartWorkflowRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * This controller act as Workflow manager that support workflow related
 * operations like (start - signal - getStatus)
 */
@RestController
@RequestMapping("/workflow")
public class WorkflowController {
    Client temporalClient;

    public WorkflowController(Client temporalClient) {
        this.temporalClient = temporalClient;
    }

    @GetMapping
    @RequestMapping("/")
    public Map getServiceInfo() {
        Map map = new HashMap<String, String>();
        map.put("temporal-server-url", temporalClient.getTemporalServerUrl());
        map.put("temporal-version", temporalClient.getTemporalVersion());
        return map;
    }

    @GetMapping
    @RequestMapping("/start-worker")
    public void startWorkflowWorker() {

        temporalClient.startNewWorker();
    }

    /**
     * Start new instance of register-credit workflow
     * 
     * @return CompletableFuture<String>: workflow instance unique identified
     */
    @GetMapping
    @RequestMapping("/start")
    public CompletableFuture<String> startRCCWorkflow() {
        String instanceId = UUID.randomUUID().toString();
        StartWorkflowRequest request = StartWorkflowRequest.builder().workflowInstanceId(instanceId).build();
        temporalClient.startNewRcInstance(request);
        return CompletableFuture.completedFuture(instanceId);
    }

    /**
     * Company approval callback api entry to signal the workflow with the approval result
     * 
     * @param workflowInstanceId
     * @param approvalStatus
     * @return ResponseEntity
     */
    @GetMapping
    @RequestMapping("/approval/company/{workflowInstanceId}/{approvalStatus}")
    public ResponseEntity companyApprovalCallback(@PathVariable String workflowInstanceId,
            @PathVariable ApprovalStatus approvalStatus) {
        var workflow = temporalClient.getWorkflowInstance(workflowInstanceId);
        workflow.companyApprovalCallback(approvalStatus);
        return ResponseEntity.ok("approvalStatus:" + approvalStatus);
    }

    /**
     * Custodian approval callback api entry to signal the workflow with the approval result
     * 
     * @param workflowInstanceId
     * @param approvalStatus
     * @return ResponseEntity
     */
    @GetMapping
    @RequestMapping("/approval/custodian/{workflowInstanceId}/{approvalStatus}")
    public ResponseEntity custodianApprovalCallback(@PathVariable String workflowInstanceId,
            @PathVariable ApprovalStatus approvalStatus) {
        var workflow = temporalClient.getWorkflowInstance(workflowInstanceId);
        workflow.custodianApprovalCallback(approvalStatus);
        return ResponseEntity.ok("approvalStatus:" + approvalStatus);
    }
}
