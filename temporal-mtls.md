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

## Self-signed certificate vs Trusted CA-signed Certificate 

**A CA-signed certificate**, on the other hand, is signed by a third-party, publicly trusted certificate authority (CA). The popular CAs are Sectigo (formerly Comodo CA), Symantec, DigiCert, Thawte, GeoTrust, GlobalSign, GoDaddy, and Entrust.  These entities are responsible for validating the person or organization that requests each certificate. [read more here](https://sectigostore.com/page/self-signed-certificate-vs-ca/)

***How to use each certificate***

- Self-signed certificates are suitable for internal (intranet) sites, and sites used in testing environments.
- CA certificates, on the other hand, are suitable for all public-facing websites and software. 

*This sample is using a `self-signed certificate with a custom root CA` to configure TLS to secure network communication with and within Temporal cluster*

***Certificate generation tools***

- [Azure Key-vault](https://learn.microsoft.com/en-us/azure/key-vault/certificates/create-certificate)
- [Openssl](https://www.openssl.org/docs/manmaster/man1/)
- [Certstrap](https://github.com/square/certstrap)
- [XCA](https://hohnstaedt.de/xca/index.php/documentation/tutorial)

## Temporal Certificate Requirements

Temporal CA and End-entity certificates requirement is listed [here](https://docs.temporal.io/cloud/how-to-manage-certificates-in-temporal-cloud#certificate-requirements)

## Generate certificates for temporal environment

The simplest temporal tls environment requires 3 certificate
- CA certificate: the certificate that is used for signing and verifying the cluster and client certificate
- Cluster certificate: encrypting communication between nodes in the cluster 
- Client certificate: encrypting communication between worker and the cluster front-end

```bash
###################################################
# Create a custom CA certificate to be used for 
# signing both cluster and client the certificate
###################################################

# generate the CA private key
openssl genrsa -out $CERTS_DIR/$certName.key 4096

# generate the CA certificate
openssl req -new -x509 -key $CERTS_DIR/$certName.key -sha256 -subj "/OU=Test CA Corporation/O=Test CA Corporation/L=London/S=Greater London/C=UK" -days 365 -out $CERTS_DIR/$certName.cert

# generate the CA certificate .pfx file
openssl pkcs12 -export -out "$CERTS_DIR/$certName.pfx" -inkey "$CERTS_DIR/$certName.key" -in "$CERTS_DIR/$certName.cert" -keypbe NONE -certpbe NONE -passout pass:

# Import .pfx into Key-Vault to be safly stored and used by the application
CERT_IMPORT_RESPONSE=$(az keyvault certificate import --vault-name $keyVault --name $certName --file $CERTS_DIR/$certName.pfx)

```

```bash
###################################################
# Create a certificate signed by the custom CA certificate generated above
# The same script is used to generate both the cluster and client certs
###################################################

# Create a new certificate in Key-Vault
CSR=$(az keyvault certificate create --vault-name $keyVault --name $certName --policy "@$certName-cert-policy.json" | jq -r '.csr')

# Download and create the certificate CSR(Certificate signing request) File
CSR_FILE_CONTENT="-----BEGIN CERTIFICATE REQUEST-----\n$CSR\n-----END CERTIFICATE REQUEST-----"
echo -e $CSR_FILE_CONTENT >> "$TEMP_DIR/$certName.csr"

# Download the private key from Key-Vault in PEM Format
az keyvault secret download --vault-name $keyVault --name $certName --file "$CERTS_DIR/$certName.key"

# Create the signed certificate using the downloaded CSR (certificate signing request)
openssl x509 -req -in $TEMP_DIR/$certName.csr -CA $ca.cert -CAkey $ca.key -sha256 -CAcreateserial -out $dir/$certName.pem -days 365 -extfile $certName.conf -extensions $exts
local chain_file="$dir/$certName.pem"
if [[ $no_chain_opt != no_chain ]]; then
    chain_file="$dir/$certName-chain.pem"
    cat $dir/$certName.pem $ca.cert > $chain_file
fi

# generate the certificate .pfx (Personal Information Exchange) file which is PKCS 12 archive file format
# "-keypbe NONE -certpbe NONE -passout pass:" exports into an unencrypted .pfx archive (for testing only)
openssl pkcs12 -export -out $dir/$certName.pfx -inkey $dir/$certName.key -in $chain_file -keypbe NONE -certpbe NONE -passout pass:

# Import .pfx into Key-Vault to be safly stored and used by the application
CERT_IMPORT_RESPONSE=$(az keyvault certificate import --vault-name $keyVault --name $certName --file $dir/$certName.pfx)

```

*More complex environment setup can be found [here](https://github.com/temporalio/samples-server/tree/main/tls/tls-full)*

## Certificate generation tools

- [Azure Key-vault](https://learn.microsoft.com/en-us/azure/key-vault/certificates/create-certificate)
- [Openssl](https://www.openssl.org/docs/manmaster/man1/)
- [Certstrap](https://github.com/square/certstrap)
- [XCA](https://hohnstaedt.de/xca/index.php/documentation/tutorial)

*Azure key-vault and openssl is used for this sample*

## Resources

- [How to manage certificates in Temporal](https://docs.temporal.io/cloud/how-to-manage-certificates-in-temporal-cloud)
- [Temporal Java Samples](https://github.com/temporalio/samples-java)
- [Temporal Server Samples](https://github.com/temporalio/samples-server/tree/main/tls/tls-simple)
- [Temporal Platform security features](https://docs.temporal.io/security?lang=java)
- [Self Signed Certificate vs CA Certificate](https://sectigostore.com/page/self-signed-certificate-vs-ca/)