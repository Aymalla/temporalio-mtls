package com.temporal.samples.helloworld;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * This controller act as Http trigger Workflow manager that support workflow related
 * operations like (start - signal - getStatus)
 */
@RestController
@RequestMapping("/")
public class Starter {
    Client temporalClient;

    public Starter(Client temporalClient) {
        this.temporalClient = temporalClient;
    }

    @GetMapping
    @RequestMapping("/")
    public Map getServiceInfo() {
        var map = new HashMap<String, String>();
        map.put("start-workflow-url", "http://localhost:8000/workflow/start");
        map.put("temporal-dashboard-url", "http://localhost:8080/namespaces/default/workflows");
        map.put("temporal-server-url", temporalClient.getTemporalServerUrl());
        map.put("temporal-version", temporalClient.getTemporalVersion());
        return map;
    }

    /**
     * Start new instance of New Greeting workflow
     * 
     * @return CompletableFuture<String>: workflow instance unique identified
     */
    @GetMapping
    @RequestMapping("/workflow/start")
    public CompletableFuture<String> startRCCWorkflow() {
        String instanceId = UUID.randomUUID().toString();
        temporalClient.startNewGreetingWorkflow(instanceId, "World");
        return CompletableFuture.completedFuture("A new workflow instance created:"+ instanceId);
    }
}
