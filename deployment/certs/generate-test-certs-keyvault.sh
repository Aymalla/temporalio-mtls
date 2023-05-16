#!/usr/bin/env bash
# This scripts generates test keys and certificates for the sample.
# In a production environment such artifacts should be generated
# by a proper certificate authority and handled in a secure manner.

# Checking key vault parameter
keyVault="$1"
if [[ $keyVault == "" ]]; then
    echo "Error: Missing key vault parameter"
    exit 1
elif ! az keyvault show -n $keyVault -o none; then
    exit 1
fi

CERTS_DIR=./certs
rm -rf $CERTS_DIR
mkdir $CERTS_DIR
TEMP_DIR=$CERTS_DIR/temp
mkdir $TEMP_DIR

generate_root_ca_cert() {
    local certName=$1

    ###########################
    # Create a custom CA certificate to be used for 
    # signing both cluster and client the certificate
    ###########################

    # Create custom CA-Certificate in Key-Vault
    #CSR=$(az keyvault certificate create --vault-name $keyVault --name $certName --policy "@$certName-cert-policy.json" | jq -r '.csr')

    # generate the CA private key
    openssl genrsa -out $CERTS_DIR/$certName.key 4096
    
    # generate the CA certificate
    openssl req -new -x509 -key $CERTS_DIR/$certName.key -sha256 -subj "/OU=Test CA Corporation/O=Test CA Corporation/L=London/S=Greater London/C=UK" -days 365 -out $CERTS_DIR/$certName.cert
    
    # generate the CA certificate .pfx file
    openssl pkcs12 -export -out "$CERTS_DIR/$certName.pfx" -inkey "$CERTS_DIR/$certName.key" -in "$CERTS_DIR/$certName.cert" -keypbe NONE -certpbe NONE -passout pass:
    
    # Import .pfx into Key-Vault to be safly stored and used by the application
    CERT_IMPORT_RESPONSE=$(az keyvault certificate import --vault-name $keyVault --name $certName --file $CERTS_DIR/$certName.pfx)
}

generate_cert() {
    local certName=$1
    local dir=$2
    local ca=$3
    local exts=$4
    local no_chain_opt=$5

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
}

echo Root CA: Generate a private key and a certificate 
generate_root_ca_cert ca $CERTS_DIR

echo Cluster: Generate a private key and a certificate
generate_cert cluster $CERTS_DIR $CERTS_DIR/ca req_ext

# Generate a private key and a certificate client
echo Client: Generate a private key and a certificate
generate_cert client $CERTS_DIR $CERTS_DIR/ca req_ext

rm -rf $TEMP_DIR
