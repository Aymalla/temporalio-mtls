SHELL := /bin/bash

.PHONY: help
.DEFAULT_GOAL := help

help: ## 💬 This help message :)
	@grep -E '[a-zA-Z_-]+:.*?## .*$$' $(firstword $(MAKEFILE_LIST)) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-23s\033[0m %s\n", $$1, $$2}'

keyvault-certs: ## 🔐 Generate the Certificates using Azure KeyVault
	@echo -e "----\e[34mStart $@\e[0m----" || true
	@cd deployment/certs && ./generate-test-certs-keyvault.sh $(kv)
	@echo -e "----\e[34mCompleted\e[0m----"

openssl-certs: ## 🔐 Generate the Certificates using openssl
	@echo -e "----\e[34mStart $@\e[0m----" || true
	@cd deployment/certs && ./generate-test-certs-openssl.sh
	@echo -e "----\e[34mCompleted\e[0m----"

start-worker: ## 🏃 start temporal worker with mlts support
	@echo -e "----\e[34mStart $@\e[0m----" || true
	@./gradlew run
	@curl http://localhost:8000/workflow/start
	@echo -e "----\e[34mCompleted\e[0m----"

start-cluster-mtls: ## 📦 start temporal cluster
	@echo -e "----\e[34mStart $@\e[0m----" || true
	@cd deployment/tls-simple && ./start-temporal.sh
	@echo -e "----\e[34mCompleted\e[0m----"

clean: ## 🧹 Clean the working folders
	@echo -e "----\e[34mStart $@\e[0m----" || true
	@rm -rf deployment/certs/certs
	@echo -e "----\e[34mCompleted\e[0m----"