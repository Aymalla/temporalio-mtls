#!/usr/bin/env bash
# This scripts generates test keys and certificates for the sample.
# In a production environment such artifacts should be genrated
# by a proper certificate authority and handled in a secure manner.

keyVault="kv-ay"
CERTS_DIR=./certs
rm -rf $CERTS_DIR
mkdir $CERTS_DIR
TEMP_DIR=$CERTS_DIR/temp
mkdir $TEMP_DIR

generate_root_ca_cert() {
    local certName=$1

    # Create custom CA-Certificate in Key-Vault
    #CSR=$(az keyvault certificate create --vault-name $keyVault --name $certName --policy "@$certName-cert-policy.json" | jq -r '.csr')

    # generate the CA private key
    openssl genrsa -out $CERTS_DIR/$certName.key 4096
    
    # generate the CA certificate
    openssl req -new -x509 -key $CERTS_DIR/$certName.key -sha256 -subj "/OU=Test CA Corporation/O=Test CA Corporation/L=London/S=Greater London/C=UK" -days 365 -out $CERTS_DIR/$certName.cert
    
    # generate the CA certificate .pfx file
    openssl pkcs12 -export -out "$CERTS_DIR/$certName.pfx" -inkey "$CERTS_DIR/$certName.key" -in "$CERTS_DIR/$certName.cert" -keypbe NONE -certpbe NONE -passout pass:
    
    # Import CA certificate .pfx into Key-Vault
    CERT_IMPORT_RESPONSE=$(az keyvault certificate import --vault-name $keyVault --name $certName --file $dir/$certName.pfx)
}

generate_cert() {
    local certName=$1
    local dir=$2
    local ca=$3
    local exts=$4
    local no_chain_opt=$5

    # Create certificate in Key-Vault
    CSR=$(az keyvault certificate create --vault-name $keyVault --name $certName --policy "@$certName-cert-policy.json" | jq -r '.csr')

    # Download and create the certificate CSR(Certificate signing request) File
    CSR_FILE_CONTENT="-----BEGIN CERTIFICATE REQUEST-----\n$CSR\n-----END CERTIFICATE REQUEST-----"
    echo -e $CSR_FILE_CONTENT >> "$TEMP_DIR/$certName.csr"

    # Download Private Key from Key-Vault in PEM Format
    az keyvault secret download --vault-name $keyVault --name $certName --file "$CERTS_DIR/$certName.key"

    #openssl req -newkey rsa:4096 -nodes -keyout "$dir/$name.key" -sha256 -out "$TEMP_DIR/$name.csr" -config "$name.conf"

    openssl x509 -req -in $TEMP_DIR/$certName.csr -CA $ca.cert -CAkey $ca.key -sha256 -CAcreateserial -out $dir/$certName.pem -days 365 -extfile $certName.conf -extensions $exts
    local chain_file="$dir/$certName.pem"
    if [[ $no_chain_opt != no_chain ]]; then
        chain_file="$dir/$certName-chain.pem"
        cat $dir/$certName.pem $ca.cert > $chain_file
    fi

    # Export to .pfx
    # "-keypbe NONE -certpbe NONE -passout pass:" exports into an unencrypted .pfx archive
    openssl pkcs12 -export -out $dir/$certName.pfx -inkey $dir/$certName.key -in $chain_file -keypbe NONE -certpbe NONE -passout pass:

    # Import .pfx into Key-Vault
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

