# Setup and configure temporal mTLS certificates using Azure Key-vault

This document demonstrates the fundamental concepts and pieces needed to setup and configure temporal cluster TLS and use Azure Key Vault to generate and store the mTLS certificates.

You can access the complete source code using [temporalio-mtls repository](https://github.com/Aymalla/temporalio-mtls)

This sample using the self-certificate signed with custom CA for TLS setup, for production or public-facing environments a Trusted CA certificate must be used.

## Temporal Connection and Encryption

Temporal Workers and Clients connect with your Temporal Cluster via gRPC, and must be configured securely for production. There are three main features to know:

- Namespaces help isolate code from each other
- TLS Encryption helps encrypt code in transit
- Data Converter helps encrypt code at rest (available soon)
Temporal Server internally has other Security features, particularly Authorization.

## Encryption in transit with mTLS (Mutual Transport Layer Security)

Temporal supports Mutual Transport Layer Security (mTLS) as a way of encrypting network traffic between the services of a cluster and also between application processes and a Cluster. Self-signed or properly minted certificates can be used for mTLS. mTLS is set in Temporal's [TLS configuration](https://docs.temporal.io/references/configuration/#tls). The configuration includes two sections such that intra-Cluster and external traffic can be encrypted with different sets of certificates and settings:

- `internode:` Configuration for encrypting communication between nodes in the cluster.
- `frontend:` Configuration for encrypting the Frontend's public endpoints.
A customized configuration can be passed using either the [WithConfig](https://docs.temporal.io/references/server-options#withconfig) or [WithConfigLoader](https://docs.temporal.io/references/server-options#withconfig) Server options.

See [TLS configuration reference](https://docs.temporal.io/references/configuration/#tls) for more details.

If you are using mTLS, is completely up to you how to get the clientCert and clientKey pair into your code, whether it is reading from filesystem, secrets manager, or both. Just keep in mind that they are whitespace sensitive and some environment variable systems have been known to cause frustration because they modify whitespace.

## Self-signed certificate vs Trusted CA-signed Certificate 

**Self-signed certificates** are created, issued, and signed by the entities whose identities the certificates are meant to verify. This means that the individual developers or the companies who have created and/or own the website or software in question are, essentially, signing off on themselves. Furthermore, self-signed certificates are signed by their own private keys. This is yet another reason why they’re not publicly trusted certificates

**CA-signed certificate**, on the other hand, is signed by a third-party, publicly trusted certificate authority (CA). The popular CAs are Sectigo (formerly Comodo CA), Symantec, DigiCert, Thawte, GeoTrust, GlobalSign, GoDaddy, and Entrust.  These entities are responsible for validating the person or organization that requests each certificate. [read more here](https://sectigostore.com/page/self-signed-certificate-vs-ca/)

CA (Certificate Authority) has two types
- ***Trusted CA:*** publicly trusted certificate authority (CA) to sign certificate for production environments [more details](https://en.wikipedia.org/wiki/Certificate_authority).
    - Azure key-vault supports (DigiCert - GlobalSign)
- ***Custom CA:*** It is created locally by the company or developer to sign and verify the self-signed certificates for internal, development, and testing environments.

***How to use each certificate***

- Self-signed certificates are suitable for internal (intranet) sites, and sites used in testing environments.
- CA certificates, on the other hand, are suitable for all public-facing websites and software. 

*This sample is using a `self-signed certificate with a custom root CA` to configure TLS to secure network communication with and within Temporal cluster*

***Certificate generation tools***

- [Azure Key-vault](https://learn.microsoft.com/en-us/azure/key-vault/certificates/create-certificate)
- [Openssl](https://www.openssl.org/docs/manmaster/man1/)
- [Certstrap](https://github.com/square/certstrap)
- [XCA](https://hohnstaedt.de/xca/index.php/documentation/tutorial)

***Azure key-vault** and **Openssl** is used for this sample*

## Setup and start Temporal Cluster with TLS enabled

To start a Temporal cluster environment with TLS enabled, three steps need to be executed in order
- [Generate the required certificates](#generate-the-required-tls-certificates)
- [Deploy Temporal Cluster](#start-temporal-cluster)
- [Start Temporal Worker](#start-temporal-worker-client)

### Generate the required TLS certificates

Temporal CA and End-entity certificates requirements is listed [here](https://docs.temporal.io/cloud/how-to-manage-certificates-in-temporal-cloud#certificate-requirements)

The simplest temporal TLS environment requires three certificates
- **CA certificate:** the certificate that is used for signing and verifying the cluster and client certificate
- **Cluster certificate:** encrypting communication between nodes in the cluster 
- **Client certificate:** encrypting communication between worker and the cluster front-end

The following diagram shows the flow of creating a new certificate using an Azure key-vault custom or non-integrated CA provider, [more details](https://learn.microsoft.com/en-us/azure/key-vault/certificates/create-certificate)

![Create certificate using custom or non-integrated CA Provider](https://learn.microsoft.com/en-us/azure/key-vault/media/certificate-authority-1.png)

Full script and configuration
- [generate-test-certs-keyvault.sh](deployment/certs/generate-test-certs-keyvault.sh)
- CA [config](deployment/certs/ca.conf) and [policy]((deployment/certs/ca-cert-policy.json)) files
- Cluster [config](deployment/certs/cluster.conf) and [policy]((deployment/certs/cluster-cert-policy.json)) files
- Client [config](deployment/certs/client.conf) and [policy]((deployment/certs/client-cert-policy.json)) files

```bash
###################################################
# Create a custom CA certificate to be used for 
# signing both cluster and client the certificate
###################################################

# Variables
CERT_DIR=./certs
CERT_NAME="ca"
KEY_VAULT=""

# generate the CA private key
openssl genrsa -out $CERTS_DIR/$CERT_NAME.key 4096

# generate the CA certificate
openssl req -new -x509 -key $CERTS_DIR/$CERT_NAME.key -sha256 -subj "/OU=Test CA Corporation/O=Test CA Corporation/L=London/S=Greater London/C=UK" -days 365 -out $CERTS_DIR/$CERT_NAME.cert

# generate the CA certificate .pfx file
openssl pkcs12 -export -out "$CERTS_DIR/$CERT_NAME.pfx" -inkey "$CERTS_DIR/$CERT_NAME.key" -in "$CERTS_DIR/$CERT_NAME.cert" -keypbe NONE -certpbe NONE -passout pass:

# Import .pfx into Key-Vault to be safely stored and used by the application
CERT_IMPORT_RESPONSE=$(az keyvault certificate import --vault-name $KEY_VAULT --name $CERT_NAME --file $CERTS_DIR/$CERT_NAME.pfx)

```

```bash
###################################################
# Create a certificate signed by the custom CA certificate generated above
# The same script is used to generate both the cluster and client certs
###################################################

# Variables
CERT_DIR=./certs
CERT_NAME="cluster"
CERT_CA_NAME="ca"
KEY_VAULT="<>"

# Create a new certificate in Key-Vault
CSR=$(az keyvault certificate create --vault-name $KEY_VAULT --name $CERT_NAME --policy "@$CERT_NAME-cert-policy.json" | jq -r '.csr')

# Download and create the certificate CSR(Certificate signing request) File
CSR_FILE_CONTENT="-----BEGIN CERTIFICATE REQUEST-----\n$CSR\n-----END CERTIFICATE REQUEST-----"
echo -e $CSR_FILE_CONTENT >> "$CERT_DIR/$CERT_NAME.csr"

# Download the private key from Key-Vault in PEM Format
az keyvault secret download --vault-name $KEY_VAULT --name $CERT_NAME --file "$CERTS_DIR/$CERT_NAME.key"

# Create the signed certificate using the downloaded CSR (certificate signing request)
openssl x509 -req -in $CERT_DIR/$CERT_NAME.csr -CA $CERT_CA_NAME.cert -CAkey $CERT_CA_NAME.key -sha256 -CAcreateserial -out $CERT_DIR/$CERT_NAME.pem -days 365 -extfile $CERT_NAME.conf -extensions $exts
local chain_file="$CERT_DIR/$CERT_NAME.pem"
if [[ $no_chain_opt != no_chain ]]; then
    chain_file="$CERT_DIR/$CERT_NAME-chain.pem"
    cat $CERT_DIR/$CERT_NAME.pem $CERT_CA_NAME.cert > $chain_file
fi

# generate the certificate .pfx (Personal Information Exchange) file which is PKCS 12 archive file format
# "-keypbe NONE -certpbe NONE -passout pass:" exports into an unencrypted .pfx archive (for testing only)
openssl pkcs12 -export -out $CERT_DIR/$CERT_NAME.pfx -inkey $CERT_DIR/$CERT_NAME.key -in $chain_file -keypbe NONE -certpbe NONE -passout pass:

# Import .pfx into Key-Vault to be safely stored and used by the application
CERT_IMPORT_RESPONSE=$(az keyvault certificate import --vault-name $KEY_VAULT --name $CERT_NAME --file $CERT_DIR/$CERT_NAME.pfx)

```

*More complex environment setup can be found in [temporal samples repository](https://github.com/temporalio/samples-server/tree/main/tls/tls-full)*

### Start Temporal Cluster 

After generating the certificates, now we can start up the temporal cluster using
- [docker-compose.yml](deployment/tls-simple/docker-compose.yml): contains definition for cluster nodes and configs
- [start-temporal.sh](deployment/tls-simple/start-temporal.sh) : Script to set the environment variables and compose up the cluster

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
      - "SKIP_DEFAULT_NAMESPACE_CREATION=false"
      - "TEMPORAL_TLS_SERVER_CA_CERT=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_TLS_SERVER_CERT=${TEMPORAL_TLS_CERTS_DIR}/cluster.pem"
      - "TEMPORAL_TLS_SERVER_KEY=${TEMPORAL_TLS_CERTS_DIR}/cluster.key"
      - "TEMPORAL_TLS_REQUIRE_CLIENT_AUTH=true"
      - "TEMPORAL_TLS_FRONTEND_CERT=${TEMPORAL_TLS_CERTS_DIR}/cluster.pem"
      - "TEMPORAL_TLS_FRONTEND_KEY=${TEMPORAL_TLS_CERTS_DIR}/cluster.key"
      - "TEMPORAL_TLS_CLIENT1_CA_CERT=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_TLS_CLIENT2_CA_CERT=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_TLS_INTERNODE_SERVER_NAME=tls-sample"
      - "TEMPORAL_TLS_FRONTEND_SERVER_NAME=tls-sample"
      - "TEMPORAL_TLS_FRONTEND_DISABLE_HOST_VERIFICATION=false"
      - "TEMPORAL_TLS_INTERNODE_DISABLE_HOST_VERIFICATION=false"
      - "TEMPORAL_CLI_ADDRESS=temporal:7233"
      - "TEMPORAL_CLI_TLS_CA=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_CLI_TLS_CERT=${TEMPORAL_TLS_CERTS_DIR}/cluster.pem"
      - "TEMPORAL_CLI_TLS_KEY=${TEMPORAL_TLS_CERTS_DIR}/cluster.key"
      - "TEMPORAL_CLI_TLS_ENABLE_HOST_VERIFICATION=true"
      - "TEMPORAL_CLI_TLS_SERVER_NAME=tls-sample"
    depends_on:
      - cassandra
  temporal-web:
    image: temporalio/web:${WEB_TAG:-latest}
    ports:
      - "8088:8088"
    volumes:
      - ${TEMPORAL_LOCAL_CERT_DIR}:${TEMPORAL_TLS_CERTS_DIR}
    environment:
      - "TEMPORAL_GRPC_ENDPOINT=temporal:7233"
      - "TEMPORAL_TLS_CERT_PATH=${TEMPORAL_TLS_CERTS_DIR}/cluster.pem"
      - "TEMPORAL_TLS_KEY_PATH=${TEMPORAL_TLS_CERTS_DIR}/cluster.key"
      - "TEMPORAL_TLS_CA_PATH=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_TLS_ENABLE_HOST_VERIFICATION=true"
      - "TEMPORAL_TLS_SERVER_NAME=tls-sample"
    depends_on:
      - temporal
  temporal-ui:
    image: temporalio/ui:${UI_TAG:-latest}
    ports:
      - "8080:8080"
    volumes:
      - ${TEMPORAL_LOCAL_CERT_DIR}:${TEMPORAL_TLS_CERTS_DIR}
    environment:
      - "TEMPORAL_ADDRESS=temporal:7233"
      - "TEMPORAL_TLS_CA=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_TLS_CERT=${TEMPORAL_TLS_CERTS_DIR}/cluster.pem"
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
      - "TEMPORAL_CLI_ADDRESS=temporal:7233"
      - "TEMPORAL_CLI_TLS_CA=${TEMPORAL_TLS_CERTS_DIR}/ca.cert"
      - "TEMPORAL_CLI_TLS_CERT=${TEMPORAL_TLS_CERTS_DIR}/client.pem"
      - "TEMPORAL_CLI_TLS_KEY=${TEMPORAL_TLS_CERTS_DIR}/client.key"
      - "TEMPORAL_CLI_TLS_ENABLE_HOST_VERIFICATION=true"
      - "TEMPORAL_CLI_TLS_SERVER_NAME=tls-sample"
    depends_on:
      - temporal

```

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

### Start Temporal Worker (Client)  

```java

// Client.java

// client certificate, which should look like:
InputStream clientCert = new FileInputStream(env.getProperty("temporal.tls.client.certPath"));
// client key, which should look like:
InputStream clientKey = new FileInputStream(env.getProperty("temporal.tls.client.keyPath"));
// certification Authority signing certificate
InputStream caCert = new FileInputStream(env.getProperty("temporal.tls.ca.certPath"));

// Create a TLS Channel Credential Builder : https://community.temporal.io/t/how-to-disable-host-name-verification/2808/8
var tlsBuilder = TlsChannelCredentials.newBuilder();
tlsBuilder.keyManager(clientCert, clientKey);
tlsBuilder.trustManager(caCert);
var channel = Grpc.newChannelBuilder(temporalServerUrl, tlsBuilder.build())
        .overrideAuthority(temporalServerCertAuthorityName)
        .build();

/*
* Get a Workflow service temporalClient which can be used to start, Signal, and
* Query Workflow Executions. This gRPC stubs wrapper talks to the Temporal service.
*/
WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
        WorkflowServiceStubsOptions
                .newBuilder()
                .setChannel(channel)
                .build());

// WorkflowClient can be used to start, signal, query, cancel, and terminate Workflows.
workflowClient = WorkflowClient.newInstance(service);

/*
* Define the workflow factory. It is used to create workflow workers that poll specific Task Queues.
*/
WorkerFactory factory = WorkerFactory.newInstance(workflowClient);

/*
* Define the workflow worker. Workflow workers listen to a defined task queue
* and process workflows and activities.
*/
io.temporal.worker.Worker worker = factory.newWorker(getTemporalTaskQueue());

/*
* Register our workflow implementation with the worker. Workflow implementations must be known to 
* the worker at runtime in order to dispatch workflow tasks.
*/
worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

/*
* Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
* the Activity Type is a shared instance.
*/
worker.registerActivitiesImplementations(new HelloActivityImpl());

/*
* Start all the workers registered for a specific task queue.
* The started workers then start polling for workflows and activities.
*/
factory.start();

```

## Resources

- [How to manage certificates in Temporal](https://docs.temporal.io/cloud/how-to-manage-certificates-in-temporal-cloud)
- [Temporal Java Samples](https://github.com/temporalio/samples-java)
- [Temporal Server Samples](https://github.com/temporalio/samples-server/tree/main/tls/tls-simple)
- [Temporal Platform security features](https://docs.temporal.io/security?lang=java)
- [Self Signed Certificate vs CA Certificate](https://sectigostore.com/page/self-signed-certificate-vs-ca/)