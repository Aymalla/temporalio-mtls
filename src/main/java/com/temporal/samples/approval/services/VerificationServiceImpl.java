package com.temporal.samples.approval.services;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.temporal.samples.approval.config.ApiConfig;
import com.temporal.samples.approval.utils.Result;
import com.temporal.samples.approval.utils.VerificationRequest;

import java.net.URI;

@Service
public class VerificationServiceImpl implements VerificationService {

    private ApiConfig apiConfig;

    public VerificationServiceImpl(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    @Override
    public Result verify(VerificationRequest request) {

        WebClient client = WebClient.create();
        Result result = client.post()
                .uri(URI.create(apiConfig.getApiUrl() + "/verify"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(Result.class)
                .block();

        return result;
    }
}
