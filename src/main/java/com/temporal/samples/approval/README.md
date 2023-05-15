# Temporal - credit registration workflow

This repository contains a sample register credit workflow implementation using Temporal, this sample will demonstrate temporal capabilities like 

- Waiting for external event (Human manual approval) 
- Approval timeout
- Integration with external Api endpoints
- How to define activity retry strategy 

![Register carbon credit](https://user-images.githubusercontent.com/116678905/217840869-98c9f156-69e7-42d9-be3e-d8432eb327ab.png)

- Approval activity 101 - sends approval request for two party to approve credit registration 
- Verification activity 201 - call external service to verify user identity
- Persistence activity 301 - persist request status to external data-store
- Notification activity 401 - Inform workflow participants with request status

## Prerequisites

- Java JDK 17+
- IntelliJ IDE

## Get Started

- Clone repository
- Navigate to `register-credit` folder
- Start temporal server `temporal server start-dev`
- Start application `gradle run`, `Application.java` is the entry point
- Start a new workflow instance:  GET "<http://localhost:8000/workflow/start>", the workflow instance unique identifier is returned as result
- Navigate to Temporal dashboard UI "<http://localhost:8233/>"
- Approval callback links will be logged in the temporal workflow history that can be accessed by dashboard
  - Company approval callback: <http://localhost:8000/workflow/approval/company/{workflowInstanceId}/Approved>
  - Company rejection callback: <http://localhost:8000/workflow/approval/company/{workflowInstanceId}/Rejected>
  - Custodian approval callback: <http://localhost:8000/workflow/approval/custodian/{workflowInstanceId}/Approved>
  - Custodian rejection callback: <http://localhost:8000/workflow/approval/custodian/{workflowInstanceId}/Rejected>
- Workflow will wait for 3 minutes to get manual approvals; if not, the workflow will end with a timeout status

## References

- [Developer's guide - Foundations](https://docs.temporal.io/application-development/foundations)
- [Workflow Signal](https://docs.temporal.io/application-development/features?lang=java#signals)