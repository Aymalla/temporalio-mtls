#!/usr/bin/env bash
# This scripts generates test keys and certificates for the sample.
# In a production environment such artifacts should be generated
# by a proper certificate authority and handled in a secure manner.

# Exit on errors
set -e

# Check for required parameters
if [[ $1 == "" ]]; then
    echo "Error missing required parameters: make keyvault-certs kv=<Key Vault Name>"
    exit 1
fi

keyVault="$1"
CERTS_DIR=./certs
rm -rf $CERTS_DIR
mkdir $CERTS_DIR
TEMP_DIR=$CERTS_DIR/temp
mkdir $TEMP_DIR

#########################################################
# Create a custom CA certificate to be used for 
# signing both cluster and client the certificate
#########################################################
generate_root_ca_cert() {
    local certName=$1
    local certDir=$2

    # az rest is used instead of az keyvault certificate create 
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
    openssl pkey -in "$CERTS_DIR/$certName.pem" -out "$CERTS_DIR/$certName.key"

    # Export certificate
    openssl x509 -in "$certDir/$certName.pem" -out "$certDir/$certName.cert"
}

    #################################################################################
    # Create a certificate signed by the custom CA certificate generated above
    # The same script is used to generate both the cluster and client certs
    # https://learn.microsoft.com/en-us/azure/key-vault/certificates/create-certificate-signing-request?tabs=azure-portal
    #################################################################################
generate_cert() {
    local certName=$1
    local dir=$2
    local ca=$3
    local exts=$4
    local no_chain_opt=$5

    # Create a new certificate in Key-Vault
    CSR=$(az keyvault certificate create --vault-name $keyVault --name $certName --policy "@$certName-cert-policy.json" | jq -r '.csr')

    # Download and create the certificate CSR(Certificate signing request) File
    CSR_FILE_CONTENT="-----BEGIN CERTIFICATE REQUEST-----\n$CSR\n-----END CERTIFICATE REQUEST-----"
    echo -e $CSR_FILE_CONTENT >> "$TEMP_DIR/$certName.csr"

    # Download the private key from Key-Vault in PEM Format
    az keyvault secret download --vault-name $keyVault --name $certName --file "$CERTS_DIR/$certName.pem"

    # Export private key
    openssl pkey -in "$CERTS_DIR/$certName.pem" -out "$CERTS_DIR/$certName.key"

    # Create the signed certificate using the downloaded CSR (certificate signing request)
    openssl x509 -req -in $TEMP_DIR/$certName.csr -CA $ca.cert -CAkey $ca.key -sha256 -CAcreateserial -out $CERTS_DIR/$certName.cert -days 365 -extfile $certName.conf -extensions $exts
    local chain_file="$CERTS_DIR/$certName.cert"
    if [[ $no_chain_opt != no_chain ]]; then
        chain_file="$CERTS_DIR/$certName-chain.cert"
        cat $CERTS_DIR/$certName.cert $ca.cert > $chain_file
    fi

    # Generate the updated pem file with both cert and key to be imported into the Key-Vault
    cat "$CERTS_DIR/$certName.key" $chain_file > "$CERTS_DIR/$certName.pem"

    # Import new pem into the Key-Vault to be safely stored and used by the application
    CERT_IMPORT_RESPONSE=$(az keyvault certificate import --vault-name $keyVault --name $certName --file "$CERTS_DIR/$certName.pem")
}


#############################################
# This function give an example of how to 
# download the certificates from Azure Key-Vault
#############################################
download_certs() {

    local certName=$1
    local download_dir=$CERTS_DIR/download

        # Download the certificate from Key Vault
    az keyvault secret download --vault-name $keyVault --name $certName --file "$download_dir/$certName.pem"

    # Export private key
    openssl pkey -in "$download_dir/$certName.pem" -out "$download_dir/$certName.key"

    # Export certifcate
    openssl x509 -in "$download_dir/$certName.pem" -out "$download_dir/$certName.cert"
}

# Generate a private key and a certificate for the CA
echo Root CA: Generate a private key and a certificate 
generate_root_ca_cert ca $CERTS_DIR

# Generate a private key and a certificate for the cluster
echo Cluster: Generate a private key and a certificate
generate_cert cluster $CERTS_DIR $CERTS_DIR/ca req_ext

# Generate a private key and a certificate for the client
echo Client: Generate a private key and a certificate
generate_cert client $CERTS_DIR $CERTS_DIR/ca req_ext

# Example of how to download the certificates from Key-Vault
#mkdir $CERTS_DIR/download
#download_certs ca
#download_certs cluster
#download_certs client

rm -rf $TEMP_DIR

