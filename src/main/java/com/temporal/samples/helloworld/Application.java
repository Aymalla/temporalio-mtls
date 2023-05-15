package com.temporal.samples.helloworld;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class Application {
	private Client temporalClient;

	public Application(Client temporalClient) {
		this.temporalClient = temporalClient;
	}
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/*
	 * These interfaces get called just before run() once SpringApplication completes.
	 */
	@Bean
	ApplicationRunner applicationRunner() {
		return args -> {
			temporalClient.startNewWorker();
		};
	}

	@Bean
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(2);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("Workflow");
		executor.initialize();
		return executor;
	}
}
