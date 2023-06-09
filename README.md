# Temporal encryption in transit using mTLS

Temporal supports Mutual Transport Layer Security (mTLS) as a way of encrypting network traffic
between the services of a cluster and also between application processes and a Cluster.
Self-signed or properly minted certificates can be used for mTLS. mTLS is set in
Temporal's [TLS configuration](https://docs.temporal.io/references/configuration/#tls).
The configuration includes two sections such that intra-Cluster and external traffic can be
encrypted with different sets of certificates and settings:

- `internode:` Configuration for encrypting communication between nodes in the cluster.
- `frontend:` Configuration for encrypting the Frontend's public endpoints.
A customized configuration can be passed using either the
[WithConfig](https://docs.temporal.io/references/server-options#withconfig) or
[WithConfigLoader](https://docs.temporal.io/references/server-options#withconfig) Server options.

## Prerequisites

- [Java JDK 17+](https://openjdk.org/install/)
- [Docker / Docker Compose](https://docs.docker.com/engine/install/)
- [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)
- [OpenSSL](https://www.openssl.org/source/)
- [Jq](https://stedolan.github.io/jq/)

`**Note:** In the case of using the dev-container and VSCode, all dependencies are already installed.`

## Get Started

- Clone this repository: `git clone https://github.com/Aymalla/temporalio-mtls.git`.
- Create an Azure Key Vault to store certificates. You also need the `Key Vault Certificates Officer`
and `Key Vault Secrets User` RBAC roles to run the scripts that create, import and download certificates.
- Sign in to your Azure subscription: `az login`.
- Generate certificates for mTLS using Azure Key Vault:
`make keyvault-certs kv=<Key Vault Name>`.
- Start the temporal cluster: `make start-cluster-mtls`.
- Start the workflow worker: `make start-worker`.
- Trigger a new helloworld workflow instance: `http://localhost:8000/workflow/start`.
- Access Temporal dashboard UI to check running history:
`http://localhost:8080/namespaces/default/workflows`.

## Make file

A Makefile provides a front-end to interact with the project. It is used both locally, during CI,
and on GitHub Actions. This Makefile is self-documented, and has the following targets:

```text
help                        💬 This help message :)
keyvault-certs              🔐 Generate the Certificates using Azure Key Vault
openssl-certs               🔐 Generate the Certificates using Openssl
start-worker                🏃 start temporal worker with mlts support
start-cluster-mtls          📦 start temporal cluster with mTLS
clean                       🧹 Clean the working folders created during build/demo
```
## Temporal authentication and authorization using Azure Active Directory(AAD)

To enable single-sign-on(SSO) for temporal web UI users and using the Azure Active directory (AAD) as Oauth identity provider for authenticating users and generating JWT access tokens [please check this blog](https://phongthaicao.medium.com/temporal-authentication-and-authorization-using-azure-ad-f940646b61e0)

## Resources

- [Temporal mTLS](./temporal-mtls.md)
- [Temporal Java Samples](https://github.com/temporalio/samples-java)
- [Temporal Server Samples](https://github.com/temporalio/samples-server/tree/main/tls/tls-simple)
- [Temporal Platform security features](https://docs.temporal.io/security?lang=java)
