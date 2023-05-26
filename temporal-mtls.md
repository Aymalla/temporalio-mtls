# Setup and configure a temporal cluster with mTLS certificates using Azure Key-vault

This document demonstrates the fundamental concepts and pieces needed to setup and configure a temporal cluster with mTLS (Mutual Transport Layer Security) enabled and use Azure Key Vault to generate and store the mTLS certificates.

*The Azure key vault is used to give an example of how the certificate can be created, managed, and stored in a secure way.*

*Note: The sample is using `self-signed certificates signed with a custom CA` for TLS setup. For production or public-facing environments, a trusted CA certificate must be used.*

*Complete source code here [temporalio-mtls repository](https://github.com/Aymalla/temporalio-mtls).*

## Temporal Connection and Encryption overview

Temporal workers and clients connect with your Temporal cluster via gRPC and must be configured securely for production. There are three main security features to know:

- **Namespaces:** help isolate code from each other
- **TLS Encryption:** helps encrypt data in transit
- **Data Converter:** helps encrypt data at rest

Temporal Server internally has other [security features](https://docs.temporal.io/security), particularly Authorization.

## Encryption in transit with mTLS

Temporal supports mTLS as a way of encrypting network traffic between the [services of a cluster](https://docs.temporal.io/clusters) and also between application processes and a cluster.

Self-signed or properly minted certificates can be used for mTLS. mTLS is set in [Temporal’s TLS configuration](https://docs.temporal.io/references/configuration/#tls). The configuration includes two sections where intra-cluster and external traffic can be encrypted with different sets of certificates and settings:

- ***Internode:*** Configuration for encrypting communication between nodes in the cluster.
- ***Frontend:*** Configuration for encrypting the frontend’s public endpoints.

![services of a temporal cluster](https://docs.temporal.io/diagrams/temporal-frontend-service.svg)

A customized configuration can be passed using either
[WithConfig](https://docs.temporal.io/references/server-options#withconfig) or 
[WithConfigLoader](https://docs.temporal.io/references/server-options#withconfig) Server options. See the [TLS configuration reference] (https://docs.temporal.io/references/configuration/#tls).

If you are using mTLS, it is completely up to you how to get the clientCert and clientKey pair into your code, whether it is reading from the filesystem, the secrets manager, or both.

## Self-signed vs Trusted CA-signed Certificates

***Self-signed certificates*** are created, issued, and signed by the same entities for whom the certificates are meant to verify their identities. This means that the individual developers or the companies that have created and/or own the website or software in question are, essentially, signing off on themselves. Furthermore, self-signed certificates are signed with their own private keys. This is yet another reason why they’re not publicly trusted certificates.

***CA-signed certificate***, on the other hand, is signed by a third-party, publicly trusted certificate authority (CA). The popular CAs are Sectigo, Symantec, DigiCert, Thawte, GeoTrust, GlobalSign, GoDaddy, and Entrust. These entities are responsible for validating the person or organization that requests each certificate. [read more here](https://sectigostore.com/page/self-signed-certificate-vs-ca/)

**How to use each certificate**

- ***Self-signed certificates*** are suitable for internal (intranet) sites and sites used in testing environments.
- ***CA certificates*** are suitable for all public-facing websites and software. 

**CA (Certificate Authority) has two types**
- ***Trusted CA:*** publicly trusted certificate authority (CA) to sign certificates for production environments;[more details](https://en.wikipedia.org/wiki/Certificate_authority).
    - Azure key-vault supports (DigiCert - GlobalSign)
- ***Custom CA:*** It is created locally by the company or developer to sign and verify the self-signed certificates for internal, development, and testing environments.

**Certificate generation tools**

- [***Azure Key-vault***](https://learn.microsoft.com/en-us/azure/key-vault/certificates/create-certificate)
- [***Openssl***](https://www.openssl.org/docs/manmaster/man1/)
- [***Certstrap***](https://github.com/square/certstrap)
- [***XCA***](https://hohnstaedt.de/xca/index.php/documentation/tutorial)

**Azure key-vault** and **Openssl** is used for this sample*

## Setup and start Temporal Cluster with TLS enabled

To start a Temporal cluster environment with TLS enabled, three steps need to be executed in order:
- [***Generate the required certificates***](#1-generate-the-required-certificates)
- [***Deploy and start a temporal Cluster***](#2-deploy-and-start-temporal-cluster)
- [***Start Temporal Worker***](#3-start-temporal-worker-client)

### 1. Generate the required certificates

Temporal has some requirements from the CA and end-entity certificates listed [here](https://docs.temporal.io/cloud/how-to-manage-certificates-in-temporal-cloud#certificate-requirements)

The simple temporal TLS environment requires three certificates
- ***CA certificate:*** the certificate used for signing and verifying the cluster and client certificate.
- ***Cluster certificate:*** encrypting communication between nodes in the cluster. 
- ***Client certificate:*** encrypting communication between worker and the cluster front-end.

More advanced environment setup can be found in [temporal samples repository](https://medium.com/r/?url=https%3A%2F%2Fgithub.com%2Ftemporalio%2Fsamples-server%2Ftree%2Fmain%2Ftls%2Ftls-full)

The following diagram shows the flow of creating a new certificate using an Azure key-vault custom or non-integrated CA provider, [more details](https://learn.microsoft.com/en-us/azure/key-vault/certificates/create-certificate)

![Create certificate using custom or non-integrated CA Provider](https://learn.microsoft.com/en-us/azure/key-vault/media/certificate-authority-1.png)

The list of configuration required to create the new certs:
- ***CA:*** [config](deployment/certs/ca.conf) and [policy](deployment/certs/ca-cert-policy.json) files
- ***Cluster:*** [config](deployment/certs/cluster.conf) and [policy](deployment/certs/cluster-cert-policy.json) files
- ***Client:*** [config](deployment/certs/client.conf) and [policy](deployment/certs/client-cert-policy.json) files

Full source code [generate-test-certs-keyvault.sh](deployment/certs/generate-test-certs-keyvault.sh)

```bash

# generate-test-certs-keyvault.sh

###################################################
# Create a custom CA certificate to be used for 
# signing both cluster and client the certificate
###################################################

# Variables
certDir=./certs
certName="ca"
keyVault="<key-vault-name>"

# `az rest` is used instead of `az keyvault certificate create` 
# because the latter does not support creating a CA self-signed certificate
# https://github.com/Azure/azure-cli/issues/18178
az rest \
    --method post \
    --body @$certName-cert-policy.json \
    --resource "https://vault.azure.net" \
    --headers '{"content-type":"application/json"}' \
    --uri "https://$keyVault.vault.azure.net/certificates/$certName/create" \
    --uri-parameters 'api-version=7.2'

# Wait for cert to be ready
status="inProgress"
while [ "$status" != "completed" ]
do
    status=$(az keyvault certificate pending show --vault-name=$keyVault --name=$certName --query status --output tsv)
    sleep 1
done

# Download the certificate from Key Vault
az keyvault secret download --vault-name $keyVault --name $certName --file "$certDir/$certName.pem"

# Export private key
openssl pkey -in "$certDir/$certName.pem" -out "$certDir/$certName.key"

# Export certificate
openssl x509 -in "$certDir/$certName.pem" -out "$certDir/$certName.cert"

```

```bash

# generate-test-certs-keyvault.sh

###################################################
# Create a certificate signed by the custom CA certificate generated above
# The same script is used to generate both the cluster and client certs
###################################################

# Variables
certDir=./certs
certName="cluster"
caCertName="ca"
keyVault="<key-vault-name>"

# Create a new certificate in Key-Vault
CSR=$(az keyvault certificate create --vault-name $keyVault --name $certName --policy "@$certName-cert-policy.json" | jq -r '.csr')

# Create the certificate CSR(Certificate signing request) file
CSR_FILE_CONTENT="-----BEGIN CERTIFICATE REQUEST-----\n$CSR\n-----END CERTIFICATE REQUEST-----"
echo -e $CSR_FILE_CONTENT >> "$certDir/$certName.csr"

# Download the private key from Key-Vault in PEM Format
az keyvault secret download --vault-name $keyVault --name $certName --file "$certDir/$certName.pem"

# Export private key from the downloaded pem file
openssl pkey -in "$certDir/$certName.pem" -out "$certDir/$certName.key"

# Create the signed certificate using the downloaded CSR (certificate signing request)
openssl x509 -req -in $certDir/$certName.csr -CA $caCertName.cert -CAkey $caCertName.key -sha256 -CAcreateserial -out $certDir/$certName.cert -days 365 -extfile $certName.conf -extensions $exts
local chain_file="$certDir/$certName.cert"
if [[ $no_chain_opt != no_chain ]]; then
    chain_file="$certDir/$certName-chain.cert"
    cat $certDir/$certName.cert $caCertName.cert > $chain_file
fi

# Generate the updated pem file with both cert and key to be imported into the Key-Vault
cat "$certDir/$certName.key" $chain_file > "$certDir/$certName.pem"

# Import new pem into the Key-Vault to be safely stored and used by the application
CERT_IMPORT_RESPONSE=$(az keyvault certificate import --vault-name $keyVault --name $certName --file "$certDir/$certName.pem")

```

### 2. Deploy and start Temporal Cluster

After generating the certificates, we can now start up the temporal cluster using:
- [**docker-compose.yml**](deployment/tls-simple/docker-compose.yml): contains the definition for cluster nodes and configurations.
- [**start-temporal.sh**](deployment/tls-simple/start-temporal.sh) : Script to set the environment variables and compose the cluster.

```yaml
 # docker-compose.yml

version: '3.5'

services:
  cassandra:
    image: cassandra:3.11
    ports:
      - "9042:9042"
  temporal:
    image: temporalio/auto-setup:${SERVER_TAG:-latest}
    ports:
      - "7233:7233"
    volumes:
      - ${DYNAMIC_CONFIG_DIR:-../config/dynamicconfig}:/etc/temporal/config/dynamicconfig
      - ${TEMPORAL_LOCAL_CERT_DIR}:${TEMPORAL_TLS_CERTS_DIR}
    environment:
      - "CASSANDRA_SEEDS=cassandra"
      - "DYNAMIC_CONFIG_FILE_PATH=config/dynamicconfig/development.yaml"
      - "SERVICES=frontend:matching:history:internal-frontend:worker"
      # Docs: https://www.opensourceagenda.com/projects/temporalio-temporal/versions
      - "USE_INTERNAL_FRONTEND=true"
      - "SKIP_DEFAULT_NAMESPACE_CREATION=false"
      - "TEMPORAL_TLS_SERVER_CA_CERT=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_TLS_SERVER_CERT=${TEMPORAL_TLS_CERTS_DIR}/cluster.cert"
      - "TEMPORAL_TLS_SERVER_KEY=${TEMPORAL_TLS_CERTS_DIR}/cluster.key"
      - "TEMPORAL_TLS_REQUIRE_CLIENT_AUTH=true"
      - "TEMPORAL_TLS_FRONTEND_CERT=${TEMPORAL_TLS_CERTS_DIR}/cluster.cert"
      - "TEMPORAL_TLS_FRONTEND_KEY=${TEMPORAL_TLS_CERTS_DIR}/cluster.key"
      - "TEMPORAL_TLS_CLIENT1_CA_CERT=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_TLS_CLIENT2_CA_CERT=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_TLS_INTERNODE_SERVER_NAME=tls-sample"
      - "TEMPORAL_TLS_FRONTEND_SERVER_NAME=tls-sample"
      - "TEMPORAL_TLS_FRONTEND_DISABLE_HOST_VERIFICATION=false"
      - "TEMPORAL_TLS_INTERNODE_DISABLE_HOST_VERIFICATION=false"
      - "TEMPORAL_CLI_ADDRESS=temporal:7236"
      - "TEMPORAL_CLI_TLS_CA=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_CLI_TLS_CERT=${TEMPORAL_TLS_CERTS_DIR}/cluster.cert"
      - "TEMPORAL_CLI_TLS_KEY=${TEMPORAL_TLS_CERTS_DIR}/cluster.key"
      - "TEMPORAL_CLI_TLS_ENABLE_HOST_VERIFICATION=true"
      - "TEMPORAL_CLI_TLS_SERVER_NAME=tls-sample"
    depends_on:
      - cassandra
  temporal-ui:
    image: temporalio/ui:${UI_TAG:-latest}
    ports:
      - "8080:8080"
    volumes:
      - ${TEMPORAL_LOCAL_CERT_DIR}:${TEMPORAL_TLS_CERTS_DIR}
    environment:
      - "TEMPORAL_ADDRESS=temporal:7233"
      - "TEMPORAL_TLS_CA=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_TLS_CERT=${TEMPORAL_TLS_CERTS_DIR}/cluster.cert"
      - "TEMPORAL_TLS_KEY=${TEMPORAL_TLS_CERTS_DIR}/cluster.key"
      - "TEMPORAL_TLS_ENABLE_HOST_VERIFICATION=true"
      - "TEMPORAL_TLS_SERVER_NAME=tls-sample"
    depends_on:
      - temporal
  temporal-admin-tools:
    image: temporalio/admin-tools:${SERVER_TAG:-latest}
    stdin_open: true
    tty: true
    volumes:
      - ${TEMPORAL_LOCAL_CERT_DIR}:${TEMPORAL_TLS_CERTS_DIR}
    environment:
      - "TEMPORAL_CLI_ADDRESS=temporal:7236"
      - "TEMPORAL_CLI_TLS_CA=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_CLI_TLS_CERT=${TEMPORAL_TLS_CERTS_DIR}/client.cert"
      - "TEMPORAL_CLI_TLS_KEY=${TEMPORAL_TLS_CERTS_DIR}/client.key"
      - "TEMPORAL_CLI_TLS_ENABLE_HOST_VERIFICATION=true"
      - "TEMPORAL_CLI_TLS_SERVER_NAME=tls-sample"
    depends_on:
      - temporal
```

Full source code [start-temporal.sh](deployment/tls-simple/start-temporal.sh)

```bash
# start-temporal.sh

# TEMPORAL_TLS_CERTS_DIR is used in docker-compose.yml to point 
# to the location of generated test certificates within the container
export TEMPORAL_TLS_CERTS_DIR=/etc/temporal/config/certs

# TEMPORAL_LOCAL_CERT_DIR is used in docker-compose.yml to point
# to our local directory with generated certificates to be mounted
# as a volume at TEMPORAL_TLS_CERTS_DIR within the container
export TEMPORAL_LOCAL_CERT_DIR=../certs/certs

docker-compose up
```

### 3. Start Temporal Worker (Client)  

Full source code [Client.java](src/main/java/com/temporal/samples/helloworld/Client.java)

```java

// Client.java

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
        .build());

// WorkflowClient can be used to start, signal, query, cancel, and terminate Workflows.
workflowClient = WorkflowClient.newInstance(service);

```

[Run the sample on your local machine](./README.md)

## References

- [Azure key-vault certificate creation methods](https://learn.microsoft.com/en-us/azure/key-vault/certificates/create-certificate)
- [Azure certificate CLI](https://learn.microsoft.com/en-us/cli/azure/keyvault/certificate)
- [How to manage certificates in Temporal](https://docs.temporal.io/cloud/how-to-manage-certificates-in-temporal-cloud)
- [Temporal Java Samples](https://github.com/temporalio/samples-java)
- [Temporal Server Samples](https://github.com/temporalio/samples-server/tree/main/tls/tls-simple)
- [Temporal Platform security features](https://docs.temporal.io/security?lang=java)
- [Self Signed Certificate vs CA Certificate](https://sectigostore.com/page/self-signed-certificate-vs-ca/)
