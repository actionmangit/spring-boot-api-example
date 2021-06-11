package com.actionman.springbootapiexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringBootApiExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootApiExampleApplication.class, args);
	}

	@GetMapping
	public String home() {
		return "Hello World!";
	}

	@GetMapping("/exit")
	public void exit() {
		ConfigurableApplicationContext ctx = SpringApplication.run(SpringBootApiExampleApplication.class);
        int exitCode = SpringApplication.exit(ctx, () -> 0);

        System.exit(exitCode);
	}
}
