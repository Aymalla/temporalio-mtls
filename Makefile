SHELL := /bin/bash

.PHONY: help
.DEFAULT_GOAL := help

help: ## 💬 This help message :)
	@grep -E '[a-zA-Z_-]+:.*?## .*$$' $(firstword $(MAKEFILE_LIST)) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-23s\033[0m %s\n", $$1, $$2}'

lint: ## 🔍 Lint the code base (but don't fix)
	@echo -e "\e[34m$@\e[0m" || true
	@./scripts/lint.sh

lint-fix: ## 🌟 Lint and fix the code base
	@echo -e "\e[34m$@\e[0m" || true
	@./scripts/lint.sh -f

build: ## 🔨 Build an Application
	@echo -e "\e[34mPlease change directory to the sample you wish to build.\e[0m" || true

test: ## 🧪 Test an Application
	@echo -e "\e[34mPlease change directory to the sample you wish to test.\e[0m" || true

generate-certs-keyvault: ## 🔐 Generate the Certificates using Azure KeyVault
	@echo -e "----\e[34mStart $@\e[0m----" || true
	@cd deployment/certs && ./generate-test-certs-keyvault.sh
	@echo -e "----\e[34mCompleted\e[0m----"

generate-certs-openssl: ## 🔐 Generate the Certificates using Azure KeyVault
	@echo -e "----\e[34mStart $@\e[0m----" || true
	@cd deployment/certs && ./generate-test-certs-openssl.sh
	@echo -e "----\e[34mCompleted\e[0m----"

tls-simple: ## 🔐 start temporal cluster
	@echo -e "----\e[34mStart $@\e[0m----" || true
	@cd deployment/tls-simple && ./start-temporal.sh
	@echo -e "----\e[34mCompleted\e[0m----"

clean: ## 🧹 Clean the working folders
	@echo -e "----\e[34mStart $@\e[0m----" || true
	@rm -rf deployment/certs/certs
	@echo -e "----\e[34mCompleted\e[0m----"