package com.temporal.samples.approval.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class ApiConfig {
    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.companyApprovalUrl}")
    private String companyApprovalUrl;

    @Value("${api.custodianApprovalUrl}")
    private String custodianApprovalUrl;

}
