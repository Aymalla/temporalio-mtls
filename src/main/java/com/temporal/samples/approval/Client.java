package com.temporal.samples.approval;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.temporal.samples.approval.services.ApprovalService;
import com.temporal.samples.approval.services.NotificationService;
import com.temporal.samples.approval.services.PersistenceService;
import com.temporal.samples.approval.services.VerificationService;
import com.temporal.samples.approval.utils.StartWorkflowRequest;
import com.temporal.samples.approval.workflows.ApprovalActivityImpl;
import com.temporal.samples.approval.workflows.NotifyActivityImpl;
import com.temporal.samples.approval.workflows.PersistActivityImpl;
import com.temporal.samples.approval.workflows.RcWorkflow;
import com.temporal.samples.approval.workflows.RcWorkflowImpl;
import com.temporal.samples.approval.workflows.VerifyActivityImpl;

import java.util.concurrent.CompletableFuture;

@Service
@Getter
@Setter
public class Client {

    @Value("${temporal.workflow.taskqueue}")
    private String temporalTaskQueue;

    @Value("${temporal.server.url}")
    private String temporalServerUrl;

    @Value("${temporal.version}")
    private String temporalVersion;

    private ApprovalService approvalService;
    private NotificationService notificationService;
    private VerificationService verificationService;
    private PersistenceService persistenceService;
    private WorkflowClient workflowClient;

    public Client(ApprovalService approvalService,
            NotificationService notificationService,
            VerificationService verificationService,
            PersistenceService persistenceService) {

        this.approvalService = approvalService;
        this.notificationService = notificationService;
        this.verificationService = verificationService;
        this.persistenceService = persistenceService;

        /*
         * Get a Workflow service temporalClient which can be used to start, Signal, and Query Workflow Executions.
         * This gRPC stubs wrapper talks to the Temporal service.
         */
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions
                        .newBuilder()
                        .setTarget(temporalServerUrl)
                        .build());

        // WorkflowClient can be used to start, signal, query, cancel, and terminate Workflows.
        workflowClient = WorkflowClient.newInstance(service);
    }

    /**
     * create temporal WorkflowClient can be used to start, signal, query, cancel,
     * and terminate Workflows.
     * 
     * @param
     * @return WorkflowClient
     */
    public WorkflowClient getWorkflowClient() {
        return workflowClient;
    }

    /**
     * Start a new temporal worker node
     * 
     * @param
     * @return
     */
    public void startNewWorker() {

        /*
         * Define the workflow factory. It is used to create workflow workers that poll
         * specific Task Queues.
         */
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);

        /*
         * Define the workflow worker. Workflow workers listen to a defined task queue
         * and process
         * workflows and activities.
         */
        io.temporal.worker.Worker worker = factory.newWorker(getTemporalTaskQueue());

        /*
         * Register our workflow implementation with the worker.
         * Workflow implementations must be known to the worker at runtime in
         * order to dispatch workflow tasks.
         */
        worker.registerWorkflowImplementationTypes(RcWorkflowImpl.class);

        /*
         * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
         * the Activity Type is a shared instance.
         */
        worker.registerActivitiesImplementations(new ApprovalActivityImpl(approvalService));
        worker.registerActivitiesImplementations(new VerifyActivityImpl(verificationService));
        worker.registerActivitiesImplementations(new PersistActivityImpl(persistenceService));
        worker.registerActivitiesImplementations(new NotifyActivityImpl(notificationService));

        /*
         * Start all the workers registered for a specific task queue.
         * The started workers then start polling for workflows and activities.
         */
        factory.start();
    }

    /**
     * start new instance of register credit
     * 
     * @param request
     * @return CompletableFuture<String>
     */
    @Async
    public CompletableFuture<String> startNewRcInstance(StartWorkflowRequest request) {

        // Define our workflow unique id
        var workflowInstanceId = request.getWorkflowInstanceId();

        /*
         * Set Workflow options such as WorkflowId and Task Queue so the worker knows
         * where to list and which workflows to execute.
         */
        var options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowInstanceId)
                .setTaskQueue(temporalTaskQueue)
                .build();

        // Create the workflow temporalClient stub. It is used to start our workflow execution.
        var workflow = workflowClient.newWorkflowStub(RcWorkflow.class, options);

        /*
         * Execute our workflow and wait for it to complete. The call to our getGreeting
         * method is synchronous.
         */
        var result = workflow.execute(request);
        var workflowId = WorkflowStub.fromTyped(workflow).getExecution().getWorkflowId();

        // Display workflow execution results
        System.out.println(workflowId + " " + result);

        return CompletableFuture.completedFuture(workflowId);
    }

    /**
     * get workflow instance
     * 
     * @param workflowInstanceId
     * @return RcWorkflow
     */
    public RcWorkflow getWorkflowInstance(String workflowInstanceId) {
        return workflowClient.newWorkflowStub(RcWorkflow.class, workflowInstanceId);
    }
}