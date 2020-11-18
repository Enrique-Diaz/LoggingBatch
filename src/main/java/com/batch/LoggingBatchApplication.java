package com.batch;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.batch.processor.ProcessLogs;

@SpringBootApplication
public class LoggingBatchApplication {

	@Autowired
	ProcessLogs processLogs;
	
	public static void main(String[] args) {
		SpringApplication.run(LoggingBatchApplication.class, args);
	}

	@PostConstruct
	public void executeBatch() {
		processLogs.runLogs();
	}
}