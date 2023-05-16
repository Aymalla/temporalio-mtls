# Encryption in transit with mTLS

Temporal supports Mutual Transport Layer Security (mTLS) as a way of encrypting network traffic between the services of a cluster and also between application processes and a Cluster. Self-signed or properly minted certificates can be used for mTLS. mTLS is set in Temporal's [TLS configuration](https://docs.temporal.io/references/configuration/#tls). The configuration includes two sections such that intra-Cluster and external traffic can be encrypted with different sets of certificates and settings:

- `internode:` Configuration for encrypting communication between nodes in the cluster.
- `frontend:` Configuration for encrypting the Frontend's public endpoints.
A customized configuration can be passed using either the [WithConfig](https://docs.temporal.io/references/server-options#withconfig) or [WithConfigLoader](https://docs.temporal.io/references/server-options#withconfig) Server options.

## Prerequisites

- Java JDK 17+
- Docker
- Azure CLI
- openssl

## Get Started

- Clone repository `git clone https://github.com/Aymalla/temporalio-mtls.git`
- A keyvault need to be created and it's name replaced inside `deployment/certs/generate-test-certs-keyvault.sh` for variable `keyVault=<keyvault name>`
- Login to your subscription `az login`
- Generate certificates for mTLS using azure keyvault `make keyvault-certs`
- Start temporal cluster `make start-temporal-cluster-mtls`
- Start workflow worker `make start-worker`
- Trigger new helloworld workflow instance `http://localhost:8000/workflow/start`
- Access Temporal dashboard UI to check running history `http://localhost:8080/namespaces/default/workflows`

## Make file

A Makefile provides a front-end to interact with the project. It is used both locally, during CI, and on GitHub Actions. This Makefile is self-documented, and has the following targets:

```text
help                        💬 This help message :)
keyvault-certs              🔐 Generate the Certificates using Azure KeyVault
openssl-certs               🔐 Generate the Certificates using Openssl
start-worker                🏃 start temporal worker with mlts support
start-temporal-dev          📦 start temporal dev server
start-temporal-cluster-mtls 📦 start temporal cluster with mTLS
clean                       🧹 Clean the working folders created during build/demo
```

## Resources

- [Temporal Java Samples](https://github.com/temporalio/samples-java)
- [Temporal Server Samples](https://github.com/temporalio/samples-server/tree/main/tls/tls-simple)
- [Temporal Platform security features](https://docs.temporal.io/security?lang=java)