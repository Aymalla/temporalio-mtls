# Temporal Connection and Encryption

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

## Self-signed certificate vs Trusted Certificate 


## Resources

- [Temporal Java Samples](https://github.com/temporalio/samples-java)
- [Temporal Server Samples](https://github.com/temporalio/samples-server/tree/main/tls/tls-simple)
- [Temporal Platform security features](https://docs.temporal.io/security?lang=java)