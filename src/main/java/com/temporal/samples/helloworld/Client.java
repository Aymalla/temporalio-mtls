package com.temporal.samples.helloworld;


import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.temporal.authorization.AuthorizationGrpcMetadataProvider;
import io.temporal.authorization.AuthorizationTokenSupplier;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.temporal.samples.helloworld.workflows.GreetingWorkflow;
import com.temporal.samples.helloworld.workflows.GreetingWorkflowImpl;
import com.temporal.samples.helloworld.workflows.HelloActivityImpl;

import java.io.FileInputStream;
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

    @Value("${temporal.server.namespace}")
    private String temporalServerNamespace;

    @Value("${temporal.server.certAuthorityName}")
    private String temporalServerCertAuthorityName;

    @Value("${temporal.version}")
    private String temporalVersion;

    private WorkflowClient workflowClient;

    public Client(Environment env) throws Exception {

        temporalTaskQueue = env.getProperty("temporal.workflow.taskqueue");
        temporalServerUrl = env.getProperty("temporal.server.url");
        temporalVersion = env.getProperty("temporal.version");
        temporalServerNamespace = env.getProperty("temporal.server.namespace");
        temporalServerCertAuthorityName = env.getProperty("temporal.server.certAuthorityName");

        // Load your client certificate:
        InputStream clientCert = new FileInputStream(env.getProperty("temporal.tls.client.certPath"));
        
        // Load PKCS8 client key:
        InputStream clientKey = new FileInputStream(env.getProperty("temporal.tls.client.keyPath"));
        
        // Certification Authority signing certificate
        InputStream caCert = new FileInputStream(env.getProperty("temporal.tls.ca.certPath"));

        // Create an SSL Context using the client certificate and key
        var sslContext = GrpcSslContexts.configure(SslContextBuilder
        .forClient()
        .keyManager(clientCert, clientKey)
        .trustManager(caCert))
        .build();


        // This code is required if you are using Temporal's authorization feature. 
        // Implement code to retrieve an access token, then provide it below.
        // AuthorizationTokenSupplier tokenSupplier = 
        //     () -> "Bearer {Access Token}";

        /*
         * Get a Workflow service temporalClient which can be used to start, Signal, and
         * Query Workflow Executions. This gRPC stubs wrapper talks to the Temporal service.
         */
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions
                .newBuilder()
                .setSslContext(sslContext)
                .setTarget(temporalServerUrl)
                .setChannelInitializer(c -> c.overrideAuthority(temporalServerCertAuthorityName)) // Override the server name used for TLS handshakes
                // .addGrpcMetadataProvider(new AuthorizationGrpcMetadataProvider(tokenSupplier)) // Uncomment if you're using Temporal's authorization
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