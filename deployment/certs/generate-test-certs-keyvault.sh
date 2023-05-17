#!/usr/bin/env bash
# This scripts generates test keys and certificates for the sample.
# In a production environment such artifacts should be generated
# by a proper certificate authority and handled in a secure manner.

# Exit on errors
set -e

# Check for required parameters
if [ $1 == "" ] || [ $2 == "" ] || [ $3 == "" ] || [ $4 == "" ]; then
    echo "Error missing required parameters: make keyvault-certs sid=<Client ID> spwd=<Client Secret> tid=<Tenant ID> kv=<Key Vault Name>"
    exit 1
fi

# Login to Azure
clientId="$1"
clientSecret="$2"
tenantId="$3"
az login --service-principal -u $clientId -p $clientSecret --tenant $tenantId

keyVault="$4"
CERTS_DIR=./certs
rm -rf $CERTS_DIR
mkdir $CERTS_DIR
TEMP_DIR=$CERTS_DIR/temp
mkdir $TEMP_DIR

generate_root_ca_cert() {
    local certName=$1
    local certDir=$2

    #########################################################
    # Create a custom CA certificate to be used for 
    # signing both cluster and client the certificate
    #########################################################

    # Query an OAuth2 token
    token=$(curl -X POST https://login.microsoftonline.com/$tenantId/oauth2/token \
        -d "grant_type=client_credentials&client_id=${clientId}&client_secret=${clientSecret}&resource=https://vault.azure.net" | jq -r '.access_token')

    # Create a new certificate in Key Vault
    curl -X POST "https://$keyVault.vault.azure.net/certificates/$certName/create?api-version=7.2" \
        -H "Authorization: Bearer $token" -H "content-type: application/json" \
        --data "@$certName-cert-policy-curl.json"

    # Wait for cert to be ready
    status="inProgress"
    while [ "$status" != "completed" ]
    do
        status=$(az keyvault certificate pending show --vault-name=$keyVault --name=$certName --query status --output tsv)
        echo $status
        sleep 1
    done

    # Download the certificate from Key Vault
    az keyvault secret download --vault-name $keyVault --name $certName --file "$certDir/$certName.pem"

    # Export cert and key
    openssl rsa -in "$certDir/$certName.pem" -out "$certDir/$certName.key"
    openssl x509 -in "$certDir/$certName.pem" -out "$certDir/$certName.cert"
}

generate_cert() {
    local certName=$1
    local dir=$2
    local ca=$3
    local exts=$4
    local no_chain_opt=$5

    #################################################################################
    # Create a certificate signed by the custom CA certificate generated above
    # The same script is used to generate both the cluster and client certs
    # https://learn.microsoft.com/en-us/azure/key-vault/certificates/create-certificate-signing-request?tabs=azure-portal
    #################################################################################

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

    # Import .pfx into Key-Vault to be safely stored and used by the application
    CERT_IMPORT_RESPONSE=$(az keyvault certificate import --vault-name $keyVault --name $certName --file $dir/$certName.pfx)
}

echo Root CA: Generate a private key and a certificate 
generate_root_ca_cert ca $CERTS_DIR

echo Cluster: Generate a private key and a certificate
generate_cert cluster $CERTS_DIR $CERTS_DIR/ca req_ext

# Generate a private key and a certificate client
echo Client: Generate a private key and a certificate
generate_cert client $CERTS_DIR $CERTS_DIR/ca req_ext

rm -rf $TEMP_DIR
