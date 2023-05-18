#!/usr/bin/env bash

# This scripts generates test keys and certificates for the sample.
# In a production environment such artifacts should be genrated
# by a proper certificate authority and handled in a secure manner.

CERTS_DIR=./certs
mkdir $CERTS_DIR
TEMP_DIR=$CERTS_DIR/temp
mkdir $TEMP_DIR

generate_root_ca_cert() {
    local name=$1
    local dir=$2

    # Generate a private key for server root CA
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out $dir/$name.key

    # Generate a certificate for server root CA
    openssl req -new -x509 -key $dir/$name.key -sha256 -config $name.conf -days 365 -out $dir/$name.cert

     # Generate the pem file with both cert and key to be imported into the Key-Vault
    cat "$dir/$name.key" "$dir/$name.cert" > "$dir/$name.pem"
}

generate_cert() {
    local name=$1
    local dir=$2
    local ca=$3
    local exts=$4
    local no_chain_opt=$5

    # Generate a private key and certificate signing request (CSR)
    openssl req -newkey rsa:4096 -nodes -keyout "$dir/$name.key" -sha256 -out "$TEMP_DIR/$name.csr" -config "$name.conf"
    
    # Create the signed certificate using the downloaded CSR (certificate signing request)
    openssl x509 -req -in $TEMP_DIR/$name.csr -CA $ca.cert -CAkey $ca.key -sha256 -CAcreateserial -out $dir/$name.cert -days 365 -extfile $name.conf -extensions $exts
    local chain_file="$dir/$name.pem"
    if [[ $no_chain_opt != no_chain ]]; then
        chain_file="$dir/$name-chain.pem"
        cat "$dir/$name.cert" "$ca.cert" > $chain_file
    fi

    # Generate the pem file with both cert and key to be imported into the Key-Vault
    cat "$dir/$name.key" $chain_file > "$dir/$name.pem"
}

# Generate a private key and a certificate for the CA
echo Generate a private key and a certificate for server root CA
generate_root_ca_cert ca $CERTS_DIR

# Generate a private key and a certificate for the cluster
echo Generate a private key and a certificate for cluster communication
generate_cert cluster $CERTS_DIR $CERTS_DIR/ca req_ext

# Generate a private key and a certificate for the client
echo Generate a private key and a certificate for default client
generate_cert client $CERTS_DIR $CERTS_DIR/ca req_ext

rm -rf $TEMP_DIR
