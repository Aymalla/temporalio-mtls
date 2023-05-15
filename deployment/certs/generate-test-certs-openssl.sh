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
    openssl genrsa -out $dir/$name.key 4096
    openssl req -new -x509 -key $dir/$name.key -sha256 -config $name.conf -days 365 -out $dir/$name.cert
}

generate_cert() {
    local name=$1
    local dir=$2
    local ca=$3
    local exts=$4
    local no_chain_opt=$5

    openssl req -newkey rsa:4096 -nodes -keyout "$dir/$name.key" -sha256 -out "$TEMP_DIR/$name.csr" -config "$name.conf"
    openssl x509 -req -in $TEMP_DIR/$name.csr -CA $ca.cert -CAkey $ca.key -sha256 -CAcreateserial -out $dir/$name.pem -days 365 -extfile $name.conf -extensions $exts
    local chain_file="$dir/$name.pem"
    if [[ $no_chain_opt != no_chain ]]; then
        chain_file="$dir/$name-chain.pem"
        cat $dir/$name.pem $ca.cert > $chain_file
    fi

    # Export to .pfx
    # "-keypbe NONE -certpbe NONE -passout pass:" exports into an unencrypted .pfx archive
    openssl pkcs12 -export -out $dir/$name.pfx -inkey $dir/$name.key -in $chain_file -keypbe NONE -certpbe NONE -passout pass:
}

echo Generate a private key and a certificate for server root CA
generate_root_ca_cert ca $CERTS_DIR


echo Generate a private key and a certificate for internode communication
generate_cert cluster $CERTS_DIR $CERTS_DIR/ca req_ext

# Generate a private key and a certificate client
echo Generate a private key and a certificate for default client
generate_cert client $CERTS_DIR $CERTS_DIR/ca req_ext

rm -rf $TEMP_DIR
