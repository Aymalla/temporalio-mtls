package com.temporal.samples.helloworld;

import io.grpc.Grpc;
import io.grpc.TlsChannelCredentials;
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

import com.temporal.samples.helloworld.workflows.GreetingWorkflow;
import com.temporal.samples.helloworld.workflows.GreetingWorkflowImpl;
import com.temporal.samples.helloworld.workflows.HelloActivityImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Service
@Getter
@Setter
public class Client {

    @Value("${temporal.workflow.taskqueue}")
    private String temporalTaskQueue;

    @Value("${temporal.server.url}")
    private String temporalServerUrl;

    @Value("${temporal.server.hostname}")
    private String temporalServerHostName;

    @Value("${temporal.server.port}")
    private String temporalServerPort;

    @Value("${temporal.version}")
    private String temporalVersion;

    private WorkflowClient workflowClient;

    public Client() throws IOException {

        // Load your client certificate, which should look like:
        InputStream clientCert = new FileInputStream("deployment/certs/certs/client.pem");
        // PKCS8 client key, which should look like:
        InputStream clientKey = new FileInputStream("deployment/certs/certs/client.key");
        // certification Authority signing certificate
        InputStream caCert = new FileInputStream("deployment/certs/certs/ca.cert");

        var tlsBuilder = TlsChannelCredentials.newBuilder();
        tlsBuilder.keyManager(clientCert, clientKey);
        tlsBuilder.trustManager(caCert);
        var channel = Grpc.newChannelBuilderForAddress("localhost", Integer.parseInt("7233"), tlsBuilder.build())
                .overrideAuthority("tls-sample")
                .build();

        /*
         * Get a Workflow service temporalClient which can be used to start, Signal, and
         * Query Workflow Executions.
         * This gRPC stubs wrapper talks to the Temporal service.
         */
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions
                        .newBuilder()
                        .setChannel(channel)
                        // .setTarget(temporalServerUrl)
                        .build());

        // WorkflowClient can be used to start, signal, query, cancel, and terminate
        // Workflows.
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
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

        /*
         * Register our Activity Types with the Worker. Since Activities are stateless
         * and thread-safe,
         * the Activity Type is a shared instance.
         */
        worker.registerActivitiesImplementations(new HelloActivityImpl());

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
    public CompletableFuture<String> startNewGreetingWorkflow(String workflowInstanceId, String name) {

        /*
         * Set Workflow options such as WorkflowId and Task Queue so the worker knows
         * where to list and which workflows to execute.
         */
        var options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowInstanceId)
                .setTaskQueue(temporalTaskQueue)
                .build();

        // Create the workflow temporalClient stub. It is used to start our workflow
        // execution.
        var workflow = workflowClient.newWorkflowStub(GreetingWorkflow.class, options);

        /*
         * Execute our workflow and wait for it to complete. The call to our getGreeting
         * method is synchronous.
         */
        var result = workflow.greet(workflowInstanceId, name);
        var workflowId = WorkflowStub.fromTyped(workflow).getExecution().getWorkflowId();

        // Display workflow execution results
        System.out.println(workflowId + " " + result);

        return CompletableFuture.completedFuture(workflowId);
    }
}